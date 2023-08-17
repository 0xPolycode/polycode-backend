package polycode.repository

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import polycode.TestBase
import polycode.config.DatabaseConfig
import polycode.features.contract.deployment.repository.JooqContractMetadataRepository
import polycode.generated.jooq.id.ContractMetadataId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.ContractMetadataTable
import polycode.generated.jooq.tables.records.ContractMetadataRecord
import polycode.model.result.ContractMetadata
import polycode.testcontainers.SharedTestContainers
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import java.util.UUID

@JooqTest
@Import(JooqContractMetadataRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqContractMetadataRepositoryIntegTest : TestBase() {

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqContractMetadataRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyCreateContractMetadata() {
        val id = ContractMetadataId(UUID.randomUUID())
        val name = "name"
        val description = "description"
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(InterfaceId("trait"))
        val projectId = ProjectId(UUID.randomUUID())

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = id,
                    name = name,
                    description = description,
                    contractId = contractId,
                    contractTags = contractTags,
                    contractImplements = contractImplements,
                    projectId = projectId
                )
            )
        }

        verify("contract metadata is correctly stored into the database") {
            val record = dslContext.selectFrom(ContractMetadataTable)
                .where(ContractMetadataTable.ID.eq(id))
                .fetchOne()

            expectThat(record)
                .isEqualTo(
                    ContractMetadataRecord(
                        id = id,
                        name = name,
                        description = description,
                        contractId = contractId,
                        contractTags = contractTags.map { it.value }.toTypedArray(),
                        contractImplements = contractImplements.map { it.value }.toTypedArray(),
                        projectId = projectId
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyUpdateExistingContractMetadata() {
        val id = ContractMetadataId(UUID.randomUUID())
        val name = "name"
        val description = "description"
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(InterfaceId("trait"))
        val projectId = ProjectId(UUID.randomUUID())

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = id,
                    name = name,
                    description = description,
                    contractId = contractId,
                    contractTags = contractTags,
                    contractImplements = contractImplements,
                    projectId = projectId
                )
            )
        }

        val otherId = ContractMetadataId(UUID.randomUUID())
        val otherName = "name"
        val otherDescription = "description"
        val otherTags = listOf(ContractTag("other-tag-1"), ContractTag("other-tag-2"))
        val otherImplements = listOf(InterfaceId("other-trait-1"), InterfaceId("other-trait-2"))

        suppose("contract metadata is stored into the database with different data") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = otherId,
                    name = otherName,
                    description = otherDescription,
                    contractId = contractId,
                    contractTags = otherTags,
                    contractImplements = otherImplements,
                    projectId = projectId
                )
            )
        }

        verify("contract metadata is correctly updated in the database") {
            val record = dslContext.selectFrom(ContractMetadataTable)
                .where(ContractMetadataTable.ID.eq(id))
                .fetchOne()

            expectThat(record)
                .isEqualTo(
                    ContractMetadataRecord(
                        id = id,
                        name = name,
                        description = description,
                        contractId = contractId,
                        contractTags = otherTags.map { it.value }.toTypedArray(),
                        contractImplements = otherImplements.map { it.value }.toTypedArray(),
                        projectId = projectId
                    )
                )
        }

        verify("contract metadata is not created for different UUID") {
            val record = dslContext.selectFrom(ContractMetadataTable)
                .where(ContractMetadataTable.ID.eq(otherId))
                .fetchOne()

            expectThat(record)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyUpdateContractMetadataInterfaces() {
        val id = ContractMetadataId(UUID.randomUUID())
        val name = "name"
        val description = "description"
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(InterfaceId("trait"))
        val projectId = ProjectId(UUID.randomUUID())

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = id,
                    name = name,
                    description = description,
                    contractId = contractId,
                    contractTags = contractTags,
                    contractImplements = contractImplements,
                    projectId = projectId
                )
            )
        }

        val newInterfaces = listOf(InterfaceId("new-interface"))

        suppose("contract metadata interfaces are updated") {
            repository.updateInterfaces(contractId, projectId, newInterfaces)
        }

        verify("contract metadata interfaces are correctly updated in database") {
            val record = dslContext.selectFrom(ContractMetadataTable)
                .where(ContractMetadataTable.ID.eq(id))
                .fetchOne()

            expectThat(record)
                .isEqualTo(
                    ContractMetadataRecord(
                        id = id,
                        name = name,
                        description = description,
                        contractId = contractId,
                        contractTags = contractTags.map { it.value }.toTypedArray(),
                        contractImplements = newInterfaces.map { it.value }.toTypedArray(),
                        projectId = projectId
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCheckIfContractMetadataExists() {
        val id = ContractMetadataId(UUID.randomUUID())
        val name = "name"
        val description = "description"
        val contractId = ContractId("cid")
        val contractTags = listOf(ContractTag("tag"))
        val contractImplements = listOf(InterfaceId("trait"))
        val projectId = ProjectId(UUID.randomUUID())

        suppose("contract metadata is stored into the database") {
            repository.createOrUpdate(
                ContractMetadata(
                    id = id,
                    name = name,
                    description = description,
                    contractId = contractId,
                    contractTags = contractTags,
                    contractImplements = contractImplements,
                    projectId = projectId
                )
            )
        }

        verify("contract metadata exists in the database") {
            expectThat(repository.exists(contractId, projectId))
                .isTrue()
        }

        verify("no other contract metadata exists in the database") {
            expectThat(repository.exists(ContractId("non-existent"), projectId))
                .isFalse()
        }
    }
}
