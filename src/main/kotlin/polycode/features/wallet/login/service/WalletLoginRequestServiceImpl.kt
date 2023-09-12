package polycode.features.wallet.login.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.config.JwtProperties
import polycode.config.authentication.JwtAuthToken
import polycode.config.authentication.JwtTokenUtils
import polycode.exception.WalletLoginFailedException
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.api.access.repository.UserIdentifierRepository
import polycode.features.wallet.authorization.service.SignatureCheckerService
import polycode.features.wallet.login.model.params.CreateWalletLoginRequestParams
import polycode.features.wallet.login.model.params.StoreWalletLoginRequestParams
import polycode.features.wallet.login.model.result.WalletLoginRequest
import polycode.features.wallet.login.repository.WalletLoginRequestRepository
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.service.EthCommonService
import polycode.service.UtcDateTimeProvider
import polycode.service.UuidProvider
import polycode.util.SignedMessage
import kotlin.time.toKotlinDuration

@Service
class WalletLoginRequestServiceImpl(
    private val signatureCheckerService: SignatureCheckerService,
    private val walletLoginRequestRepository: WalletLoginRequestRepository,
    private val userIdentifierRepository: UserIdentifierRepository,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val ethCommonService: EthCommonService,
    private val jwtProperties: JwtProperties
) : WalletLoginRequestService { // TODO test happy path for attach (and user fetch + create in that path)

    companion object : KLogging()

    override fun createWalletLoginRequest(params: CreateWalletLoginRequestParams): WalletLoginRequest {
        logger.info { "Creating wallet login request, params: $params" }
        return walletLoginRequestRepository.store(
            StoreWalletLoginRequestParams.fromCreateParams(
                id = uuidProvider.getRawUuid(),
                params = params,
                createdAt = utcDateTimeProvider.getUtcDateTime()
            )
        )
    }

    override fun attachSignedMessageAndVerifyLogin(
        id: WalletLoginRequestId,
        signedMessage: SignedMessage
    ): JwtAuthToken {
        logger.debug { "Fetching wallet login request, id: $id" }

        val walletLoginRequest = ethCommonService.fetchResource(
            walletLoginRequestRepository.getById(id),
            "Wallet login request not found for ID: $id"
        )

        val validUntil = walletLoginRequest.createdAt
            .plus(this.jwtProperties.walletLoginRequestValidity.toKotlinDuration()).value
        val now = this.utcDateTimeProvider.getUtcDateTime().value

        if (validUntil.isBefore(now)) {
            throw WalletLoginFailedException("Wallet login request has expired")
        }

        logger.info { "Attach signedMessage to wallet login request, id: $id, signedMessage: $signedMessage" }

        val attached = walletLoginRequestRepository.setSignedMessage(id, signedMessage)

        if (attached.not()) {
            throw WalletLoginFailedException(
                "Unable to attach signed message to wallet login request with ID: $id"
            )
        }

        val signatureMatches = signatureCheckerService.signatureMatches(
            message = walletLoginRequest.messageToSign,
            signedMessage = signedMessage,
            signer = walletLoginRequest.walletAddress
        )

        if (signatureMatches.not()) {
            throw WalletLoginFailedException("Signature does not match expected signature")
        }

        val userIdentifier = userIdentifierRepository.getByWalletAddress(walletLoginRequest.walletAddress)
            ?: userIdentifierRepository.store(
                UserWalletAddressIdentifier(
                    id = uuidProvider.getUuid(UserId),
                    walletAddress = walletLoginRequest.walletAddress
                )
            )

        return JwtTokenUtils.encodeToken(
            walletAddress = userIdentifier.walletAddress,
            privateKey = jwtProperties.privateKey,
            tokenValidity = jwtProperties.tokenValidity.toKotlinDuration()
        )
    }
}
