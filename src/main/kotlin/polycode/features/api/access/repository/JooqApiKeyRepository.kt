package polycode.features.api.access.repository

import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import polycode.features.api.access.model.result.ApiKey
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.ApiKeyTable
import polycode.generated.jooq.tables.records.ApiKeyRecord

@Repository
class JooqApiKeyRepository(private val dslContext: DSLContext) : ApiKeyRepository {

    companion object : KLogging()

    override fun store(apiKey: ApiKey): ApiKey {
        logger.info { "Store API key, apiKey: $apiKey" }
        val record = ApiKeyRecord(
            id = apiKey.id,
            projectId = apiKey.projectId,
            apiKey = apiKey.apiKey,
            createdAt = apiKey.createdAt
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: ApiKeyId): ApiKey? {
        logger.debug { "Get API key by id: $id" }
        return dslContext.selectFrom(ApiKeyTable)
            .where(ApiKeyTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByValue(value: String): ApiKey? {
        logger.debug { "Get API key by value: $value" }
        return dslContext.selectFrom(ApiKeyTable)
            .where(ApiKeyTable.API_KEY_.eq(value))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: ProjectId): List<ApiKey> {
        logger.debug { "Get API keys by projectId: $projectId" }
        return dslContext.selectFrom(ApiKeyTable)
            .where(ApiKeyTable.PROJECT_ID.eq(projectId))
            .orderBy(ApiKeyTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun exists(apiKey: String): Boolean {
        logger.debug { "Check if API key exists: $apiKey" }
        return dslContext.fetchExists(
            ApiKeyTable,
            ApiKeyTable.API_KEY_.eq(apiKey)
        )
    }

    private fun ApiKeyRecord.toModel(): ApiKey =
        ApiKey(
            id = id,
            projectId = projectId,
            apiKey = apiKey,
            createdAt = createdAt
        )
}
