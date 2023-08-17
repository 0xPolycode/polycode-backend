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
import polycode.TestData
import polycode.config.DatabaseConfig
import polycode.features.contract.deployment.model.filters.ContractDecoratorFilters
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.repository.JooqImportedContractDecoratorRepository
import polycode.features.contract.interfaces.model.filters.ContractInterfaceFilters
import polycode.features.contract.interfaces.repository.InMemoryContractInterfacesRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ImportedContractDecoratorId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ImportedContractDecoratorRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.testcontainers.SharedTestContainers
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import java.util.UUID

@JooqTest
@Import(
    JooqImportedContractDecoratorRepository::class,
    InMemoryContractInterfacesRepository::class,
    DatabaseConfig::class
)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqImportedContractDecoratorRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID_1 = ProjectId(UUID.randomUUID())
        private val PROJECT_ID_2 = ProjectId(UUID.randomUUID())
        private val OWNER_ID = UserId(UUID.randomUUID())
        private val EMPTY_INTERFACE_MANIFEST = InterfaceManifestJson(
            name = null,
            description = null,
            tags = emptySet(),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        private data class DecoratorTestData(
            val contractId: ContractId,
            val manifest: ManifestJson,
            val artifact: ArtifactJson,
            val markdown: String
        )

        private data class DecoratorWithProjectId(
            val projectId: ProjectId,
            val decorator: ContractDecorator,
            val manifest: ManifestJson,
            val artifact: ArtifactJson,
            val markdown: String
        )
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqImportedContractDecoratorRepository

    @Autowired
    private lateinit var interfacesRepository: InMemoryContractInterfacesRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
        interfacesRepository.getAll(ContractInterfaceFilters(OrList(emptyList()))).forEach {
            interfacesRepository.delete(it.id)
        }

        interfacesRepository.store(InterfaceId("trait-1"), EMPTY_INTERFACE_MANIFEST)
        interfacesRepository.store(InterfaceId("trait-2"), EMPTY_INTERFACE_MANIFEST)
        interfacesRepository.store(InterfaceId("trait-3"), EMPTY_INTERFACE_MANIFEST)
        interfacesRepository.store(InterfaceId("ignored-trait"), EMPTY_INTERFACE_MANIFEST)
        interfacesRepository.store(InterfaceId("new-interface"), EMPTY_INTERFACE_MANIFEST)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_1,
                ownerId = OWNER_ID,
                baseRedirectUrl = BaseUrl("base-redirect-url-0"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url-0",
                createdAt = TestData.TIMESTAMP
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_2,
                ownerId = OWNER_ID,
                baseRedirectUrl = BaseUrl("base-redirect-url-1"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url-1",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyStoreAndFetchImportedContractDecoratorWhenPreviewOnlyIsSetToFalse() {
        val id = ImportedContractDecoratorId(UUID.randomUUID())
        val contractId = ContractId("imported-contract")
        val manifestJson = ManifestJson(
            name = "name",
            description = "description",
            tags = setOf("tag-1"),
            implements = setOf("trait-1"),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
        val artifactJson = ArtifactJson(
            contractName = "imported-contract",
            sourceName = "imported.sol",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        val infoMarkdown = "markdown"

        val storedContractDecorator = suppose("imported contract decorator will be stored into the database") {
            repository.store(
                id = id,
                projectId = PROJECT_ID_1,
                contractId = contractId,
                manifestJson = manifestJson,
                artifactJson = artifactJson,
                infoMarkdown = infoMarkdown,
                importedAt = TestData.TIMESTAMP,
                previewOnly = false
            )
        }

        val expectedDecorator = ContractDecorator(
            id = contractId,
            artifact = artifactJson,
            manifest = manifestJson,
            imported = true,
            interfacesProvider = null
        )

        verify("storing imported contract decorator returns correct result") {
            expectThat(storedContractDecorator)
                .isEqualTo(expectedDecorator)
        }

        verify("imported contract decorator was correctly stored into the database") {
            expectThat(
                repository.getByContractIdAndProjectId(contractId, PROJECT_ID_1)
                    ?.copy(artifact = expectedDecorator.artifact)
            ).isEqualTo(expectedDecorator)
            expectThat(repository.getManifestJsonByContractIdAndProjectId(contractId, PROJECT_ID_1))
                .isEqualTo(manifestJson)
            expectThat(
                repository.getArtifactJsonByContractIdAndProjectId(contractId, PROJECT_ID_1)
                    ?.copy(linkReferences = null, deployedLinkReferences = null)
            ).isEqualTo(artifactJson)
            expectThat(repository.getInfoMarkdownByContractIdAndProjectId(contractId, PROJECT_ID_1))
                .isEqualTo(infoMarkdown)
        }
    }

    @Test
    fun mustCorrectlyUpdateImportedContractDecoratorInterfaces() {
        val id = ImportedContractDecoratorId(UUID.randomUUID())
        val contractId = ContractId("imported-contract")
        val manifestJson = ManifestJson(
            name = "name",
            description = "description",
            tags = setOf("tag-1"),
            implements = setOf("trait-1"),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
        val artifactJson = ArtifactJson(
            contractName = "imported-contract",
            sourceName = "imported.sol",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        val infoMarkdown = "markdown"

        suppose("imported contract decorator will be stored into the database") {
            repository.store(
                id = id,
                projectId = PROJECT_ID_1,
                contractId = contractId,
                manifestJson = manifestJson,
                artifactJson = artifactJson,
                infoMarkdown = infoMarkdown,
                importedAt = TestData.TIMESTAMP,
                previewOnly = false
            )
        }

        val newInterfaces = listOf(InterfaceId("new-interface"))

        suppose("imported contract decorator interfaces are updated") {
            repository.updateInterfaces(contractId, PROJECT_ID_1, newInterfaces, manifestJson)
        }

        val expectedManifest = manifestJson.copy(implements = newInterfaces.map { it.value }.toSet())
        val expectedDecorator = ContractDecorator(
            id = contractId,
            artifact = artifactJson,
            manifest = expectedManifest,
            imported = true,
            interfacesProvider = null
        )

        verify("imported contract decorator was correctly updated in the database") {
            expectThat(
                repository.getByContractIdAndProjectId(contractId, PROJECT_ID_1)
                    ?.copy(artifact = expectedDecorator.artifact)
            ).isEqualTo(expectedDecorator)
            expectThat(repository.getManifestJsonByContractIdAndProjectId(contractId, PROJECT_ID_1))
                .isEqualTo(expectedManifest)
            expectThat(
                repository.getArtifactJsonByContractIdAndProjectId(contractId, PROJECT_ID_1)
                    ?.copy(linkReferences = null, deployedLinkReferences = null)
            ).isEqualTo(artifactJson)
            expectThat(repository.getInfoMarkdownByContractIdAndProjectId(contractId, PROJECT_ID_1))
                .isEqualTo(infoMarkdown)
        }
    }

    @Test
    fun mustNotStoreImportedContractDecoratorWhenPreviewOnlyIsSetToTrue() {
        val id = ImportedContractDecoratorId(UUID.randomUUID())
        val contractId = ContractId("imported-contract")
        val manifestJson = ManifestJson(
            name = "name",
            description = "description",
            tags = setOf("tag-1"),
            implements = setOf("trait-1"),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
        val artifactJson = ArtifactJson(
            contractName = "imported-contract",
            sourceName = "imported.sol",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        val infoMarkdown = "markdown"

        val storedContractDecorator = suppose("imported contract decorator will be stored into the database") {
            repository.store(
                id = id,
                projectId = PROJECT_ID_1,
                contractId = contractId,
                manifestJson = manifestJson,
                artifactJson = artifactJson,
                infoMarkdown = infoMarkdown,
                importedAt = TestData.TIMESTAMP,
                previewOnly = true
            )
        }

        val expectedDecorator = ContractDecorator(
            id = contractId,
            artifact = artifactJson,
            manifest = manifestJson,
            imported = true,
            interfacesProvider = null
        )

        verify("storing imported contract decorator returns correct result") {
            expectThat(storedContractDecorator)
                .isEqualTo(expectedDecorator)
        }

        verify("imported contract decorator is not stored into the database") {
            expectThat(repository.getByContractIdAndProjectId(contractId, PROJECT_ID_1))
                .isNull()
            expectThat(repository.getManifestJsonByContractIdAndProjectId(contractId, PROJECT_ID_1))
                .isNull()
            expectThat(repository.getArtifactJsonByContractIdAndProjectId(contractId, PROJECT_ID_1))
                .isNull()
            expectThat(repository.getInfoMarkdownByContractIdAndProjectId(contractId, PROJECT_ID_1))
                .isNull()
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentImportedContractDecoratorByContractId() {
        val contractId = ContractId("abc")
        val projectId = ProjectId(UUID.randomUUID())

        verify("null is returned when fetching non-existent imported contract decorator by contract id") {
            expectThat(repository.getByContractIdAndProjectId(contractId, projectId))
                .isNull()
            expectThat(repository.getManifestJsonByContractIdAndProjectId(contractId, projectId))
                .isNull()
            expectThat(repository.getArtifactJsonByContractIdAndProjectId(contractId, projectId))
                .isNull()
            expectThat(repository.getInfoMarkdownByContractIdAndProjectId(contractId, projectId))
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchImportedContractDecoratorsByProjectIdAndFilters() {
        val tag1TestData = createTestData(contractId = ContractId("cid-tag-1"), tags = listOf("tag-1"))
        val tag2TestData = createTestData(contractId = ContractId("cid-tag-2"), tags = listOf("tag-2"))
        val tag2AndIgnoredTagTestData = createTestData(
            contractId = ContractId("cid-tag-2-ignored"),
            tags = listOf("tag-2", "ignored-tag")
        )
        val ignoredTagTestData = createTestData(
            contractId = ContractId("cid-ignored-tag"),
            tags = listOf("ignored-tag")
        )
        val trait1TestData = createTestData(contractId = ContractId("cid-trait-1"), traits = listOf("trait-1"))
        val trait2TestData = createTestData(contractId = ContractId("cid-trait-2"), traits = listOf("trait-2"))
        val trait2AndIgnoredTraitTestData = createTestData(
            contractId = ContractId("cid-trait-2-ignored"),
            traits = listOf("trait-2", "ignored-trait")
        )
        val ignoredTraitTestData = createTestData(
            contractId = ContractId("cid-ignored-trait"),
            traits = listOf("ignored-trait")
        )
        val project2TestData1 = createTestData(
            contractId = ContractId("cid-1-project-2"),
            tags = listOf("tag-1", "tag-2"),
            traits = listOf("trait-1", "trait-2")
        )
        val project2TestData2 = createTestData(
            contractId = ContractId("cid-2-project-2"),
            tags = listOf("tag-1", "tag-2"),
            traits = listOf("trait-1", "trait-2")
        )
        val project2TestData3 = createTestData(
            contractId = ContractId("cid-3-project-2"),
            tags = listOf("ignored-tag", "tag-3"),
            traits = listOf("ignored-trait", "trait-3")
        )
        val project2NonMatchingTestData1 = createTestData(
            contractId = ContractId("cid-3-project-2-missing-tag"),
            tags = listOf("tag-1"),
            traits = listOf("ignored-trait", "trait-1", "trait-2")
        )
        val project2NonMatchingTestData2 = createTestData(
            contractId = ContractId("cid-4-project-2-missing-trait"),
            tags = listOf("ignored-tag", "tag-1", "tag-2"),
            traits = listOf("trait-1")
        )
        val testDataById = listOf(
            tag1TestData,
            tag2TestData,
            tag2AndIgnoredTagTestData,
            ignoredTagTestData,
            trait1TestData,
            trait2TestData,
            trait2AndIgnoredTraitTestData,
            ignoredTraitTestData,
            project2TestData1,
            project2TestData2,
            project2TestData3,
            project2NonMatchingTestData1,
            project2NonMatchingTestData2
        ).associateBy { it.contractId }

        val project1DecoratorsWithMatchingTags = listOf(
            createDecorator(projectId = PROJECT_ID_1, testData = tag1TestData),
            createDecorator(projectId = PROJECT_ID_1, testData = tag2TestData),
            createDecorator(projectId = PROJECT_ID_1, testData = tag2AndIgnoredTagTestData)
        )
        val project1DecoratorsWithNonMatchingTags = listOf(
            createDecorator(projectId = PROJECT_ID_1, testData = ignoredTagTestData)
        )
        val project1DecoratorsWithMatchingTraits = listOf(
            createDecorator(projectId = PROJECT_ID_1, testData = trait1TestData),
            createDecorator(projectId = PROJECT_ID_1, testData = trait2TestData),
            createDecorator(projectId = PROJECT_ID_1, testData = trait2AndIgnoredTraitTestData)
        )
        val project1DecoratorsWithNonMatchingTraits = listOf(
            createDecorator(projectId = PROJECT_ID_1, testData = ignoredTraitTestData)
        )

        val project2MatchingDecorators = listOf(
            createDecorator(projectId = PROJECT_ID_2, testData = project2TestData1),
            createDecorator(projectId = PROJECT_ID_2, testData = project2TestData2),
            createDecorator(projectId = PROJECT_ID_2, testData = project2TestData3)
        )
        val project2NonMatchingDecorators = listOf(
            createDecorator(projectId = PROJECT_ID_2, testData = project2NonMatchingTestData1),
            createDecorator(projectId = PROJECT_ID_2, testData = project2NonMatchingTestData2)
        )

        suppose("some imported contract decorators exist in database") {
            dslContext.batchInsert(
                (
                    project1DecoratorsWithMatchingTags + project1DecoratorsWithNonMatchingTags +
                        project1DecoratorsWithMatchingTraits + project1DecoratorsWithNonMatchingTraits +
                        project2MatchingDecorators + project2NonMatchingDecorators
                    )
                    .map {
                        ImportedContractDecoratorRecord(
                            id = ImportedContractDecoratorId(UUID.randomUUID()),
                            projectId = it.projectId,
                            contractId = it.decorator.id,
                            manifestJson = it.manifest,
                            artifactJson = it.artifact,
                            infoMarkdown = it.markdown,
                            contractTags = it.manifest.tags.toTypedArray(),
                            contractImplements = it.manifest.implements.toTypedArray(),
                            importedAt = TestData.TIMESTAMP
                        )
                    }
            ).execute()
        }

        verify("must correctly fetch project 1 imported contract decorators with matching tags") {
            val filters = ContractDecoratorFilters(
                contractTags = OrList(
                    AndList(ContractTag("tag-1")),
                    AndList(ContractTag("tag-2"))
                ),
                contractImplements = OrList()
            )

            expectThat(repository.getAll(PROJECT_ID_1, filters).fixEquals())
                .containsExactlyInAnyOrderElementsOf(project1DecoratorsWithMatchingTags.map { it.decorator })
            expectThat(repository.getAllManifestJsonFiles(PROJECT_ID_1, filters))
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTags.map { testDataById[it.decorator.id]!!.manifest }
                )
            expectThat(
                repository.getAllArtifactJsonFiles(PROJECT_ID_1, filters)
                    .map { it.copy(linkReferences = null, deployedLinkReferences = null) }
            ).containsExactlyInAnyOrderElementsOf(
                project1DecoratorsWithMatchingTags.map { testDataById[it.decorator.id]!!.artifact }
            )
            expectThat(repository.getAllInfoMarkdownFiles(PROJECT_ID_1, filters))
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTags.map { testDataById[it.decorator.id]!!.markdown }
                )
        }

        verify("must correctly fetch project 1 imported contract decorators with matching traits") {
            val filters = ContractDecoratorFilters(
                contractTags = OrList(),
                contractImplements = OrList(
                    AndList(InterfaceId("trait-1")),
                    AndList(InterfaceId("trait-2"))
                )
            )
            expectThat(repository.getAll(PROJECT_ID_1, filters).fixEquals())
                .containsExactlyInAnyOrderElementsOf(project1DecoratorsWithMatchingTraits.map { it.decorator })
            expectThat(repository.getAllManifestJsonFiles(PROJECT_ID_1, filters))
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTraits.map { testDataById[it.decorator.id]!!.manifest }
                )
            expectThat(
                repository.getAllArtifactJsonFiles(PROJECT_ID_1, filters)
                    .map { it.copy(linkReferences = null, deployedLinkReferences = null) }
            ).containsExactlyInAnyOrderElementsOf(
                project1DecoratorsWithMatchingTraits.map { testDataById[it.decorator.id]!!.artifact }
            )
            expectThat(repository.getAllInfoMarkdownFiles(PROJECT_ID_1, filters))
                .containsExactlyInAnyOrderElementsOf(
                    project1DecoratorsWithMatchingTraits.map { testDataById[it.decorator.id]!!.markdown }
                )
        }

        verify("must correctly fetch project 2 imported contract decorators which match given filters") {
            val filters = ContractDecoratorFilters(
                contractTags = OrList(
                    AndList(ContractTag("tag-1"), ContractTag("tag-2")),
                    AndList(ContractTag("tag-3"))
                ),
                contractImplements = OrList(
                    AndList(InterfaceId("trait-1"), InterfaceId("trait-2")),
                    AndList(InterfaceId("trait-3"))
                )
            )

            expectThat(repository.getAll(PROJECT_ID_2, filters).fixEquals())
                .containsExactlyInAnyOrderElementsOf(project2MatchingDecorators.map { it.decorator })
            expectThat(repository.getAllManifestJsonFiles(PROJECT_ID_2, filters))
                .containsExactlyInAnyOrderElementsOf(
                    project2MatchingDecorators.map { testDataById[it.decorator.id]!!.manifest }
                )
            expectThat(
                repository.getAllArtifactJsonFiles(PROJECT_ID_2, filters)
                    .map { it.copy(linkReferences = null, deployedLinkReferences = null) }
            ).containsExactlyInAnyOrderElementsOf(
                project2MatchingDecorators.map { testDataById[it.decorator.id]!!.artifact }
            )
            expectThat(repository.getAllInfoMarkdownFiles(PROJECT_ID_2, filters))
                .containsExactlyInAnyOrderElementsOf(
                    project2MatchingDecorators.map { testDataById[it.decorator.id]!!.markdown }
                )
        }
    }

    private fun createTestData(
        contractId: ContractId,
        tags: List<String> = emptyList(),
        traits: List<String> = emptyList(),
    ) = DecoratorTestData(
        contractId = contractId,
        manifest = ManifestJson(
            name = "name-${contractId.value}",
            description = "description",
            tags = tags.toSet(),
            implements = traits.toSet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        ),
        artifact = ArtifactJson(
            contractName = "imported-contract-${contractId.value}",
            sourceName = "imported.sol",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        ),
        markdown = "markdown-${contractId.value}"
    )

    private fun createDecorator(projectId: ProjectId, testData: DecoratorTestData) = DecoratorWithProjectId(
        projectId = projectId,
        decorator = ContractDecorator(
            id = testData.contractId,
            artifact = testData.artifact,
            manifest = testData.manifest,
            imported = true,
            interfacesProvider = null
        ),
        manifest = testData.manifest,
        artifact = testData.artifact,
        markdown = testData.markdown
    )

    private fun List<ContractDecorator>.fixEquals(): List<ContractDecorator> =
        map { it.copy(artifact = it.artifact.copy(linkReferences = null, deployedLinkReferences = null)) }
}
