package polycode.features.api.usage.model.result

import polycode.generated.jooq.id.UserId
import polycode.util.UtcDateTime

data class ApiUsagePeriod(
    val userId: UserId,
    val writeRequestUsage: RequestUsage,
    val readRequestUsage: RequestUsage,
    val startDate: UtcDateTime,
    val endDate: UtcDateTime
)

data class RequestUsage(val used: Long, val remaining: Long)
