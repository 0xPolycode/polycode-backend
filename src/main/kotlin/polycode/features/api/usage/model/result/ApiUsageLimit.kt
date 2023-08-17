package polycode.features.api.usage.model.result

import polycode.generated.jooq.id.UserId
import polycode.util.UtcDateTime

data class ApiUsageLimit(
    val userId: UserId,
    val allowedWriteRequests: Long,
    val allowedReadRequests: Long,
    val startDate: UtcDateTime,
    val endDate: UtcDateTime
)
