package polycode.config.binding

import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import polycode.config.binding.annotation.UserIdentifierBinding
import polycode.exception.BadAuthenticationException
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.api.access.model.result.UserPolyflowAccountIdIdentifier
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.api.access.repository.PolyflowUserRepository
import polycode.features.api.access.repository.UserIdentifierRepository
import polycode.features.api.usage.model.result.ApiUsageLimit
import polycode.features.api.usage.repository.ApiRateLimitRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.UserId
import polycode.service.UtcDateTimeProvider
import polycode.service.UuidProvider
import polycode.util.Either
import polycode.util.WalletAddress
import polyflow.generated.jooq.id.PolyflowUserId

class UserIdentifierResolver(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val userIdentifierRepository: UserIdentifierRepository,
    private val polyflowUserRepository: PolyflowUserRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == UserIdentifier::class.java &&
            parameter.hasParameterAnnotation(UserIdentifierBinding::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): UserIdentifier {
        @Suppress("UNCHECKED_CAST")
        val principal = (SecurityContextHolder.getContext().authentication?.principal as? Either<UserId, WalletAddress>)

        return principal?.mapRight { attemptWalletAddressLogin(it) }
            ?: principal?.mapLeft { attemptPolyflowUserIdLogin(it) }
            ?: throw BadAuthenticationException()
    }

    private fun attemptWalletAddressLogin(walletAddress: WalletAddress): UserIdentifier =
        userIdentifierRepository.getByWalletAddress(walletAddress)
            ?: userIdentifierRepository.store(
                UserWalletAddressIdentifier(
                    id = uuidProvider.getUuid(UserId),
                    walletAddress = walletAddress
                )
            )

    private fun attemptPolyflowUserIdLogin(userId: UserId): UserIdentifier? {
        val polyflowUserId = PolyflowUserId(userId.value)

        return polyflowUserRepository.getById(polyflowUserId)?.let { polyflowUser ->
            val localUser = userIdentifierRepository
                .getByUserIdentifier(userId.value.toString(), UserIdentifierType.POLYFLOW_USER_ID)
                ?: userIdentifierRepository.store(
                    UserPolyflowAccountIdIdentifier(
                        id = uuidProvider.getUuid(UserId),
                        polyflowId = polyflowUserId
                    )
                )
            val now = utcDateTimeProvider.getUtcDateTime()
            val apiLimits = apiRateLimitRepository.getCurrentApiUsagePeriodLimits(localUser.id, now)

            if (
                apiLimits == null ||
                apiLimits.allowedReadRequests != polyflowUser.monthlyReadRequests ||
                apiLimits.allowedWriteRequests != polyflowUser.monthlyWriteRequests
            ) {
                val newLimits = ApiUsageLimit(
                    userId = localUser.id,
                    allowedWriteRequests = polyflowUser.monthlyWriteRequests,
                    allowedReadRequests = polyflowUser.monthlyReadRequests,
                    startDate = now.atMonthStart(),
                    endDate = now.atMonthEnd()
                )

                apiRateLimitRepository.createNewFutureUsageLimits(localUser.id, now, listOf(newLimits))
            }

            localUser
        }
    }
}
