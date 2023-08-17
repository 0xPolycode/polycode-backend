package polycode.features.api.access.repository

import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.ProjectTable
import polycode.generated.jooq.tables.records.ProjectRecord

@Repository
class JooqProjectRepository(private val dslContext: DSLContext) : ProjectRepository {

    companion object : KLogging()

    override fun store(project: Project): Project {
        logger.info { "Store project: $project" }
        val record = ProjectRecord(
            id = project.id,
            ownerId = project.ownerId,
            baseRedirectUrl = project.baseRedirectUrl,
            chainId = project.chainId,
            customRpcUrl = project.customRpcUrl,
            createdAt = project.createdAt
        )

        dslContext.executeInsert(record)

        return record.toModel()
    }

    override fun getById(id: ProjectId): Project? {
        logger.debug { "Get project by id: $id" }
        return dslContext.selectFrom(ProjectTable)
            .where(ProjectTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByOwnerId(ownerId: UserId): List<Project> {
        logger.info { "Get projects by ownerId: $ownerId" }
        return dslContext.selectFrom(ProjectTable)
            .where(ProjectTable.OWNER_ID.eq(ownerId))
            .orderBy(ProjectTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    private fun ProjectRecord.toModel(): Project =
        Project(
            id = id,
            ownerId = ownerId,
            baseRedirectUrl = baseRedirectUrl,
            chainId = chainId,
            customRpcUrl = customRpcUrl,
            createdAt = createdAt
        )
}
