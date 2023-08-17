package polycode.model.params

import polycode.features.api.access.model.result.Project
import polycode.util.UtcDateTime
import java.util.UUID

interface ParamsFactory<P, R> {
    fun fromCreateParams(id: UUID, params: P, project: Project, createdAt: UtcDateTime): R
}
