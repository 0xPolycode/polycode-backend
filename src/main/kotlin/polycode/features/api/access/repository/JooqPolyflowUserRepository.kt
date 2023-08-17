package polycode.features.api.access.repository

import mu.KLogging
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import polycode.features.api.access.model.result.PolyflowUser
import polyflow.generated.jooq.id.PolyflowUserId
import polyflow.generated.jooq.tables.UserTable

@Repository
class JooqPolyflowUserRepository( // TODO test
    @Qualifier("polyflowDslContext") private val polyflowDslContext: DSLContext
) : PolyflowUserRepository {

    companion object : KLogging()

    override fun getById(id: PolyflowUserId): PolyflowUser? {
        logger.debug { "Get Polyflow user by id: $id" }
        return polyflowDslContext.selectFrom(UserTable)
            .where(UserTable.ID.eq(id))
            .fetchOne {
                PolyflowUser(
                    id = it.id,
                    email = it.email,
                    monthlyReadRequests = it.monthlyPolycodeReadRequests,
                    monthlyWriteRequests = it.monthlyPolycodeWriteRequests
                )
            }
    }
}
