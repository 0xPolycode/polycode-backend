package polycode.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.config.JwtProperties
import polycode.exception.ResourceNotFoundException
import polycode.exception.WalletLoginFailedException
import polycode.features.wallet.authorization.service.SignatureCheckerService
import polycode.features.wallet.login.model.params.CreateWalletLoginRequestParams
import polycode.features.wallet.login.model.params.StoreWalletLoginRequestParams
import polycode.features.wallet.login.model.result.WalletLoginRequest
import polycode.features.wallet.login.repository.WalletLoginRequestRepository
import polycode.features.wallet.login.service.WalletLoginRequestServiceImpl
import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.util.DurationSeconds
import polycode.util.SignedMessage
import polycode.util.WalletAddress
import java.math.BigInteger
import java.time.Duration
import java.util.UUID
import kotlin.time.toKotlinDuration

class WalletLoginRequestServiceTest : TestBase() {

    companion object {
        private val JWT_PROPERTIES = JwtProperties(
            privateKey = mock(),
            tokenValidity = Duration.ofDays(30L),
            walletLoginRequestValidity = Duration.ofDays(1L)
        )
    }

    @Test
    fun mustSuccessfullyCreateWalletLoginRequest() {
        val uuid = WalletLoginRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getRawUuid())
                .willReturn(uuid.value)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val createParams = CreateWalletLoginRequestParams(
            walletAddress = WalletAddress("a")
        )
        val databaseParams = StoreWalletLoginRequestParams(
            id = uuid,
            walletAddress = createParams.walletAddress,
            messageToSign = "Sign this message to confirm that you are the owner of the wallet:" +
                " ${WalletAddress("a").rawValue}\nID to sign: ${uuid.value}, timestamp: ${TestData.TIMESTAMP.iso}",
            createdAt = TestData.TIMESTAMP
        )
        val databaseResponse = WalletLoginRequest(
            id = uuid,
            walletAddress = databaseParams.walletAddress,
            messageToSign = databaseParams.messageToSign,
            signedMessage = null,
            createdAt = TestData.TIMESTAMP
        )
        val walletLoginRequestRepository = mock<WalletLoginRequestRepository>()

        suppose("wallet login request is stored in database") {
            call(walletLoginRequestRepository.store(databaseParams))
                .willReturn(databaseResponse)
        }

        val service = WalletLoginRequestServiceImpl(
            signatureCheckerService = mock(),
            walletLoginRequestRepository = walletLoginRequestRepository,
            userIdentifierRepository = mock(),
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            jwtProperties = JWT_PROPERTIES
        )

        verify("wallet login request is correctly created") {
            expectThat(service.createWalletLoginRequest(createParams))
                .isEqualTo(databaseResponse)

            expectInteractions(walletLoginRequestRepository) {
                once.store(databaseParams)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentWalletLoginRequest() {
        val uuid = WalletLoginRequestId(UUID.randomUUID())
        val walletLoginRequestRepository = mock<WalletLoginRequestRepository>()

        suppose("wallet login request is not in database") {
            call(walletLoginRequestRepository.getById(uuid))
                .willReturn(null)
        }

        val service = WalletLoginRequestServiceImpl(
            signatureCheckerService = mock(),
            walletLoginRequestRepository = walletLoginRequestRepository,
            userIdentifierRepository = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            jwtProperties = JWT_PROPERTIES
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.attachSignedMessageAndVerifyLogin(uuid, SignedMessage("example"))
            }
        }
    }

    @Test
    fun mustThrowWalletLoginFailedExceptionWhenWalletLoginRequestHasExpired() {
        val uuid = WalletLoginRequestId(UUID.randomUUID())
        val walletLoginRequestRepository = mock<WalletLoginRequestRepository>()

        val walletLoginRequest = WalletLoginRequest(
            id = uuid,
            walletAddress = WalletAddress("a"),
            messageToSign = "b",
            signedMessage = null,
            createdAt = TestData.TIMESTAMP
        )

        suppose("wallet login request is returned from database") {
            call(walletLoginRequestRepository.getById(uuid))
                .willReturn(walletLoginRequest)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some later timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(
                    TestData.TIMESTAMP
                        .plus(JWT_PROPERTIES.walletLoginRequestValidity.toKotlinDuration())
                        .plus(DurationSeconds(BigInteger.ONE))
                )
        }

        val service = WalletLoginRequestServiceImpl(
            signatureCheckerService = mock(),
            walletLoginRequestRepository = walletLoginRequestRepository,
            userIdentifierRepository = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            jwtProperties = JWT_PROPERTIES
        )

        verify("WalletLoginFailedException is thrown") {
            expectThrows<WalletLoginFailedException> {
                service.attachSignedMessageAndVerifyLogin(uuid, SignedMessage("example"))
            }
        }
    }

    @Test
    fun mustThrowWalletLoginFailedExceptionWhenAttachingSignedMessageFails() {
        val uuid = WalletLoginRequestId(UUID.randomUUID())
        val walletLoginRequestRepository = mock<WalletLoginRequestRepository>()

        val walletLoginRequest = WalletLoginRequest(
            id = uuid,
            walletAddress = WalletAddress("a"),
            messageToSign = "b",
            signedMessage = null,
            createdAt = TestData.TIMESTAMP
        )

        suppose("wallet login request is returned from database") {
            call(walletLoginRequestRepository.getById(uuid))
                .willReturn(walletLoginRequest)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        suppose("signed message will not be attached") {
            call(walletLoginRequestRepository.setSignedMessage(uuid, SignedMessage("example")))
                .willReturn(false)
        }

        val service = WalletLoginRequestServiceImpl(
            signatureCheckerService = mock(),
            walletLoginRequestRepository = walletLoginRequestRepository,
            userIdentifierRepository = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            jwtProperties = JWT_PROPERTIES
        )

        verify("WalletLoginFailedException is thrown") {
            expectThrows<WalletLoginFailedException> {
                service.attachSignedMessageAndVerifyLogin(uuid, SignedMessage("example"))
            }

            expectInteractions(walletLoginRequestRepository) {
                once.getById(uuid)
                once.setSignedMessage(uuid, SignedMessage("example"))
            }
        }
    }

    @Test
    fun mustThrowWalletLoginFailedExceptionWhenSignatureDoesNotMatch() {
        val uuid = WalletLoginRequestId(UUID.randomUUID())
        val walletLoginRequestRepository = mock<WalletLoginRequestRepository>()

        val walletLoginRequest = WalletLoginRequest(
            id = uuid,
            walletAddress = WalletAddress("a"),
            messageToSign = "b",
            signedMessage = null,
            createdAt = TestData.TIMESTAMP
        )

        suppose("wallet login request is returned from database") {
            call(walletLoginRequestRepository.getById(uuid))
                .willReturn(walletLoginRequest)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        suppose("signed message will be attached") {
            call(walletLoginRequestRepository.setSignedMessage(uuid, SignedMessage("example")))
                .willReturn(true)
        }

        val signatureCheckerService = mock<SignatureCheckerService>()

        suppose("signature does not match") {
            call(
                signatureCheckerService.signatureMatches(
                    message = walletLoginRequest.messageToSign,
                    signedMessage = SignedMessage("example"),
                    signer = walletLoginRequest.walletAddress
                )
            )
                .willReturn(false)
        }

        val service = WalletLoginRequestServiceImpl(
            signatureCheckerService = signatureCheckerService,
            walletLoginRequestRepository = walletLoginRequestRepository,
            userIdentifierRepository = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            jwtProperties = JWT_PROPERTIES
        )

        verify("WalletLoginFailedException is thrown") {
            expectThrows<WalletLoginFailedException> {
                service.attachSignedMessageAndVerifyLogin(uuid, SignedMessage("example"))
            }

            expectInteractions(walletLoginRequestRepository) {
                once.getById(uuid)
                once.setSignedMessage(uuid, SignedMessage("example"))
            }
        }
    }
}
