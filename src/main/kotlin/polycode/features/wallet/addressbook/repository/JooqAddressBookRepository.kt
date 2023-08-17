package polycode.features.wallet.addressbook.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import polycode.exception.AliasAlreadyInUseException
import polycode.features.wallet.addressbook.model.result.AddressBookEntry
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.AddressBookId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.AddressBookTable
import polycode.generated.jooq.tables.UserIdentifierTable
import polycode.generated.jooq.tables.records.AddressBookRecord
import polycode.util.WalletAddress

@Repository
class JooqAddressBookRepository(private val dslContext: DSLContext) : AddressBookRepository {

    companion object : KLogging()

    override fun store(addressBookEntry: AddressBookEntry): AddressBookEntry {
        logger.info { "Store address book entry, addressBookEntry: $addressBookEntry" }
        return handleDuplicateAlias(addressBookEntry.alias) {
            val record = addressBookEntry.toRecord()
            dslContext.executeInsert(record)
            record.toModel()
        }
    }

    override fun update(addressBookEntry: AddressBookEntry): AddressBookEntry? {
        logger.info { "Update address book entry, addressBookEntry: $addressBookEntry" }
        return handleDuplicateAlias(addressBookEntry.alias) {
            dslContext.update(AddressBookTable)
                .set(
                    addressBookEntry.toRecord().apply {
                        changed(true)
                        changed(AddressBookTable.ID, false)
                        changed(AddressBookTable.CREATED_AT, false)
                        changed(AddressBookTable.USER_ID, false)
                    }
                )
                .where(
                    DSL.and(
                        AddressBookTable.ID.eq(addressBookEntry.id),
                        AddressBookTable.USER_ID.eq(addressBookEntry.userId)
                    )
                )
                .returning()
                .fetchOne { it.toModel() }
        }
    }

    override fun delete(id: AddressBookId): Boolean {
        logger.info { "Delete address book entry, id: $id" }
        return dslContext.deleteFrom(AddressBookTable)
            .where(AddressBookTable.ID.eq(id))
            .execute() > 0
    }

    override fun getById(id: AddressBookId): AddressBookEntry? {
        logger.debug { "Get address book entry by id: $id" }
        return dslContext.selectFrom(AddressBookTable)
            .where(AddressBookTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getByAliasAndUserId(alias: String, userId: UserId): AddressBookEntry? {
        logger.debug { "Get address book entry by alias and project ID, alias: $alias, userId: $userId" }
        return dslContext.selectFrom(AddressBookTable)
            .where(
                DSL.and(
                    AddressBookTable.ALIAS.eq(alias),
                    AddressBookTable.USER_ID.eq(userId)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getAllByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry> {
        logger.debug { "Get address book entries by walletAddress: $walletAddress" }
        return dslContext.select(AddressBookTable.fields().toList())
            .from(
                AddressBookTable.join(UserIdentifierTable)
                    .on(AddressBookTable.USER_ID.eq(UserIdentifierTable.ID))
            )
            .where(
                DSL.and(
                    UserIdentifierTable.IDENTIFIER_TYPE.eq(UserIdentifierType.ETH_WALLET_ADDRESS),
                    UserIdentifierTable.USER_IDENTIFIER_.eq(walletAddress.rawValue)
                )
            )
            .orderBy(AddressBookTable.CREATED_AT.asc())
            .fetch { it.into(AddressBookTable).toModel() }
    }

    private fun AddressBookEntry.toRecord() =
        AddressBookRecord(
            id = id,
            alias = alias,
            walletAddress = address,
            phoneNumber = phoneNumber,
            email = email,
            createdAt = createdAt,
            userId = userId
        )

    private fun AddressBookRecord.toModel() =
        AddressBookEntry(
            id = id,
            alias = alias,
            address = walletAddress,
            phoneNumber = phoneNumber,
            email = email,
            createdAt = createdAt,
            userId = userId
        )

    private fun <T> handleDuplicateAlias(alias: String, fn: () -> T): T =
        try {
            fn()
        } catch (e: DuplicateKeyException) {
            throw AliasAlreadyInUseException(alias)
        }
}
