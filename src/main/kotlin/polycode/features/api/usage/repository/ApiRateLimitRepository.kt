package polycode.features.api.usage.repository

import org.springframework.web.bind.annotation.RequestMethod
import polycode.features.api.usage.model.result.ApiUsageLimit
import polycode.features.api.usage.model.result.ApiUsagePeriod
import polycode.generated.jooq.id.UserId
import polycode.util.UtcDateTime

interface ApiRateLimitRepository {
    fun createNewFutureUsageLimits(userId: UserId, currentTime: UtcDateTime, limits: List<ApiUsageLimit>)
    fun getCurrentApiUsagePeriodLimits(userId: UserId, currentTime: UtcDateTime): ApiUsageLimit?
    fun getCurrentApiUsagePeriod(userId: UserId, currentTime: UtcDateTime): ApiUsagePeriod
    fun remainingWriteLimit(userId: UserId, currentTime: UtcDateTime): Long
    fun remainingReadLimit(userId: UserId, currentTime: UtcDateTime): Long
    fun addWriteCall(userId: UserId, currentTime: UtcDateTime, method: RequestMethod, endpoint: String)
    fun addReadCall(userId: UserId, currentTime: UtcDateTime, endpoint: String)
}
