package polycode.features.api.usage.model.response

import polycode.features.api.usage.model.result.ApiUsagePeriod
import polycode.features.api.usage.model.result.RequestUsage
import polycode.generated.jooq.id.UserId
import java.time.OffsetDateTime

data class ApiUsagePeriodResponse(
    val userId: UserId,
    val writeRequestUsage: RequestUsage,
    val readRequestUsage: RequestUsage,
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime
) {
    constructor(apiUsagePeriod: ApiUsagePeriod) : this(
        userId = apiUsagePeriod.userId,
        writeRequestUsage = apiUsagePeriod.writeRequestUsage,
        readRequestUsage = apiUsagePeriod.readRequestUsage,
        startDate = apiUsagePeriod.startDate.value,
        endDate = apiUsagePeriod.endDate.value
    )
}
