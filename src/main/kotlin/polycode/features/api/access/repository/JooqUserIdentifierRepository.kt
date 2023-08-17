package polycode.features.api.access.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.api.access.model.result.UserPolyflowAccountIdIdentifier
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.UserIdentifierTable
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.util.WalletAddress
import polyflow.generated.jooq.id.PolyflowUserId
import java.util.UUID

@Repository
class JooqUserIdentifierRepository(private val dslContext: DSLContext) : UserIdentifierRepository {

    companion object : KLogging()

    override fun store(userIdentifier: UserIdentifier): UserIdentifier {
        logger.info { "Store user identifier: $userIdentifier" }
        val record = UserIdentifierRecord(
            id = userIdentifier.id,
            userIdentifier = userIdentifier.userIdentifier,
            identifierType = userIdentifier.identifierType
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UserId): UserIdentifier? {
        logger.debug { "Get user identifier by id: $id" }
        return dslContext.selectFrom(UserIdentifierTable)
            .where(UserIdentifierTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByUserIdentifier(userIdentifier: String, identifierType: UserIdentifierType): UserIdentifier? {
        logger.debug { "Get user identifier by userIdentifier: $userIdentifier, identifierType: $identifierType" }
        return dslContext.selectFrom(UserIdentifierTable)
            .where(
                DSL.and(
                    UserIdentifierTable.USER_IDENTIFIER_.eq(userIdentifier),
                    UserIdentifierTable.IDENTIFIER_TYPE.eq(identifierType)
                )
            )
            .fetchOne { it.toModel() }
    }

    private fun UserIdentifierRecord.toModel(): UserIdentifier =
        when (identifierType) {
            UserIdentifierType.ETH_WALLET_ADDRESS ->
                UserWalletAddressIdentifier(
                    id = id,
                    walletAddress = WalletAddress(userIdentifier)
                )

            UserIdentifierType.POLYFLOW_USER_ID ->
                UserPolyflowAccountIdIdentifier(
                    id = id,
                    polyflowId = PolyflowUserId(UUID.fromString(userIdentifier))
                )
        }
}
