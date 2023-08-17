package polycode.features.api.access.repository

import polycode.features.api.access.model.result.PolyflowUser
import polyflow.generated.jooq.id.PolyflowUserId

interface PolyflowUserRepository {
    fun getById(id: PolyflowUserId): PolyflowUser?
}
