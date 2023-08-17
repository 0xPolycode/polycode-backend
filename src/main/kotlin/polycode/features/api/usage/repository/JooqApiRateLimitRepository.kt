package polycode.features.api.usage.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.RequestMethod
import polycode.config.ApiRateProperties
import polycode.features.api.usage.model.result.ApiUsageLimit
import polycode.features.api.usage.model.result.ApiUsagePeriod
import polycode.features.api.usage.model.result.RequestUsage
import polycode.generated.jooq.id.ApiUsagePeriodId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.ApiUsagePeriodTable
import polycode.generated.jooq.tables.records.ApiReadCallRecord
import polycode.generated.jooq.tables.records.ApiUsagePeriodRecord
import polycode.generated.jooq.tables.records.ApiWriteCallRecord
import polycode.util.UtcDateTime
import java.util.UUID
import kotlin.math.max
import kotlin.time.toKotlinDuration
import polycode.generated.jooq.enums.RequestMethod as DbRequestMethod

@Repository
@Suppress("TooManyFunctions")
class JooqApiRateLimitRepository(
    private val dslContext: DSLContext,
    private val apiRateProperties: ApiRateProperties
) : ApiRateLimitRepository {

    companion object : KLogging()

    override fun createNewFutureUsageLimits(userId: UserId, currentTime: UtcDateTime, limits: List<ApiUsageLimit>) {
        logger.info { "Create future API usage limits, userId: $userId, currentTime: $currentTime, limits: $limits" }

        // delete all future usage limits
        dslContext.deleteFrom(ApiUsagePeriodTable)
            .where(
                DSL.and(
                    ApiUsagePeriodTable.USER_ID.eq(userId),
                    ApiUsagePeriodTable.START_DATE.ge(currentTime)
                )
            )
            .execute()

        // end current usage period
        dslContext.update(ApiUsagePeriodTable)
            .set(ApiUsagePeriodTable.END_DATE, currentTime)
            .where(
                DSL.and(
                    ApiUsagePeriodTable.USER_ID.eq(userId),
                    ApiUsagePeriodTable.START_DATE.le(currentTime),
                    ApiUsagePeriodTable.END_DATE.ge(currentTime)
                )
            )
            .execute()

        val records = limits.map {
            ApiUsagePeriodRecord(
                id = ApiUsagePeriodId(UUID.randomUUID()),
                userId = userId,
                allowedWriteRequests = it.allowedWriteRequests,
                allowedReadRequests = it.allowedReadRequests,
                usedWriteRequests = 0L,
                usedReadRequests = 0L,
                startDate = it.startDate,
                endDate = it.endDate
            )
        }

        // insert new usage periods
        dslContext.batchInsert(records).execute()
    }

    override fun getCurrentApiUsagePeriodLimits(userId: UserId, currentTime: UtcDateTime): ApiUsageLimit? {
        logger.debug { "Get current API usage period limits, userId: $userId, currentTime: $currentTime" }

        val currentPeriod = dslContext.selectFrom(ApiUsagePeriodTable)
            .where(
                DSL.and(
                    ApiUsagePeriodTable.USER_ID.eq(userId),
                    ApiUsagePeriodTable.START_DATE.le(currentTime),
                    ApiUsagePeriodTable.END_DATE.ge(currentTime)
                )
            )
            .orderBy(ApiUsagePeriodTable.END_DATE.desc())
            .limit(1)
            .fetchOne()

        return currentPeriod?.let {
            ApiUsageLimit(
                userId = userId,
                allowedWriteRequests = it.allowedWriteRequests,
                allowedReadRequests = it.allowedReadRequests,
                startDate = it.startDate,
                endDate = it.endDate
            )
        }
    }

    override fun getCurrentApiUsagePeriod(userId: UserId, currentTime: UtcDateTime): ApiUsagePeriod {
        logger.debug { "Get current API usage period, userId: $userId, currentTime: $currentTime" }

        val currentPeriod = getOrCreateApiUsagePeriod(userId, currentTime)

        return ApiUsagePeriod(
            userId = userId,
            writeRequestUsage = calculateUsage(currentPeriod.usedWriteRequests, currentPeriod.allowedWriteRequests),
            readRequestUsage = calculateUsage(currentPeriod.usedReadRequests, currentPeriod.allowedReadRequests),
            startDate = currentPeriod.startDate,
            endDate = currentPeriod.endDate
        )
    }

    override fun remainingWriteLimit(userId: UserId, currentTime: UtcDateTime): Long {
        val currentPeriod = getOrCreateApiUsagePeriod(userId, currentTime)
        return calculateUsage(currentPeriod.usedWriteRequests, currentPeriod.allowedWriteRequests).remaining
    }

    override fun remainingReadLimit(userId: UserId, currentTime: UtcDateTime): Long {
        val currentPeriod = getOrCreateApiUsagePeriod(userId, currentTime)
        return calculateUsage(currentPeriod.usedReadRequests, currentPeriod.allowedReadRequests).remaining
    }

    override fun addWriteCall(userId: UserId, currentTime: UtcDateTime, method: RequestMethod, endpoint: String) {
        logger.info {
            "Adding write call, userId: $userId, currentTime: $currentTime, method: $method, endpoint: $endpoint"
        }

        dslContext.executeInsert(
            ApiWriteCallRecord(
                userId = userId,
                requestMethod = DbRequestMethod.valueOf(method.name),
                requestPath = endpoint,
                createdAt = currentTime
            )
        )

        getOrCreateApiUsagePeriod(userId, currentTime).incrementField(ApiUsagePeriodTable.USED_WRITE_REQUESTS)
    }

    override fun addReadCall(userId: UserId, currentTime: UtcDateTime, endpoint: String) {
        logger.info { "Adding read call, userId: $userId, currentTime: $currentTime, endpoint: $endpoint" }

        dslContext.executeInsert(
            ApiReadCallRecord(
                userId = userId,
                requestPath = endpoint,
                createdAt = currentTime
            )
        )

        getOrCreateApiUsagePeriod(userId, currentTime).incrementField(ApiUsagePeriodTable.USED_READ_REQUESTS)
    }

    private fun getOrCreateApiUsagePeriod(userId: UserId, currentTime: UtcDateTime): ApiUsagePeriodRecord =
        dslContext.selectFrom(ApiUsagePeriodTable)
            .where(
                DSL.and(
                    ApiUsagePeriodTable.USER_ID.eq(userId),
                    ApiUsagePeriodTable.START_DATE.le(currentTime),
                    ApiUsagePeriodTable.END_DATE.ge(currentTime)
                )
            )
            .orderBy(ApiUsagePeriodTable.END_DATE.desc())
            .limit(1)
            .fetchOne() ?: insertNewApiUsagePeriodRecord(userId, currentTime)

    private fun insertNewApiUsagePeriodRecord(userId: UserId, startDate: UtcDateTime): ApiUsagePeriodRecord {
        val endDate = startDate + apiRateProperties.usagePeriodDuration.toKotlinDuration()

        logger.info {
            "Creating API usage period for userId: $userId, period: [${startDate.value}, ${endDate.value}]"
        }

        val record = ApiUsagePeriodRecord(
            id = ApiUsagePeriodId(UUID.randomUUID()),
            userId = userId,
            allowedWriteRequests = apiRateProperties.freeTierWriteRequests,
            allowedReadRequests = apiRateProperties.freeTierReadRequests,
            usedWriteRequests = 0L,
            usedReadRequests = 0L,
            startDate = startDate,
            endDate = endDate
        )

        dslContext.executeInsert(record)

        return record
    }

    private fun ApiUsagePeriodRecord.incrementField(field: TableField<ApiUsagePeriodRecord, Long>) =
        dslContext.update(ApiUsagePeriodTable)
            .set(field, field + 1)
            .where(ApiUsagePeriodTable.ID.eq(this.id))
            .execute()

    private fun calculateUsage(count: Long, total: Long): RequestUsage =
        RequestUsage(
            used = count,
            remaining = max(total - count, 0)
        )
}
