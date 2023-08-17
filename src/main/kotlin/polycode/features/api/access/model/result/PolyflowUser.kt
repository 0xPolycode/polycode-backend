package polycode.features.api.access.model.result

import polyflow.generated.jooq.id.PolyflowUserId

data class PolyflowUser(
    val id: PolyflowUserId,
    val email: String,
    val monthlyReadRequests: Long,
    val monthlyWriteRequests: Long
)
