package polycode.config

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.devtools.filewatch.ChangedFile
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import polycode.TestBase
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.result.ContractConstructor
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.model.result.ContractEvent
import polycode.features.contract.deployment.model.result.ContractFunction
import polycode.features.contract.deployment.model.result.ContractParameter
import polycode.features.contract.deployment.model.result.EventParameter
import polycode.features.contract.deployment.repository.ContractMetadataRepository
import polycode.features.contract.deployment.repository.InMemoryContractDecoratorRepository
import polycode.features.contract.deployment.repository.JooqContractMetadataRepository
import polycode.features.contract.interfaces.repository.ContractInterfacesRepository
import polycode.features.contract.interfaces.repository.InMemoryContractInterfacesRepository
import polycode.service.RandomUuidProvider
import polycode.testcontainers.SharedTestContainers
import polycode.util.Constants
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import java.nio.file.Paths

@JooqTest
@Import(JooqContractMetadataRepository::class, DatabaseConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContractDecoratorFileChangeListenerIntegTest : TestBase() {

    companion object {
        private val CONSTRUCTORS = listOf(
            ContractConstructor(
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                description = "Constructor description",
                payable = false
            ),
            ContractConstructor(
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "string",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                description = "Payable constructor description",
                payable = true
            )
        )
        private val FUNCTIONS = listOf(
            ContractFunction(
                name = "View function",
                description = "View function description",
                solidityName = "viewFn",
                signature = "viewFn(address)",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "string",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                emittableEvents = listOf("ExampleEvent(address)"),
                readOnly = true
            ),
            ContractFunction(
                name = "Pure function",
                description = "Pure function description",
                solidityName = "pureFn",
                signature = "pureFn(address)",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "string",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                emittableEvents = listOf("ExampleEvent(address)"),
                readOnly = true
            ),
            ContractFunction(
                name = "Modifying function",
                description = "Modifying function description",
                solidityName = "modifyingFn",
                signature = "modifyingFn(address)",
                inputs = listOf(
                    ContractParameter(
                        name = "Arg",
                        description = "Arg description",
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                outputs = listOf(
                    ContractParameter(
                        name = "Return value",
                        description = "Return value description",
                        solidityName = "",
                        solidityType = "string",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                ),
                emittableEvents = listOf("ExampleEvent(address)"),
                readOnly = false
            )
        )
        private val EVENTS = listOf(
            ContractEvent(
                name = "Example Event",
                description = "Example Event description",
                solidityName = "ExampleEvent",
                signature = "ExampleEvent(address)",
                inputs = listOf(
                    EventParameter(
                        name = "Arg",
                        description = "Arg description",
                        indexed = false,
                        solidityName = "arg",
                        solidityType = "address",
                        recommendedTypes = listOf("example"),
                        parameters = null,
                        hints = null
                    )
                )
            )
        )
        private val EMPTY_CONTRACT_INTERFACE = InterfaceManifestJson(null, null, emptySet(), emptyList(), emptyList())
    }

    private val interfacesDir = Paths.get(javaClass.classLoader.getResource("dummyInterfaces")!!.path)
    private val parsableContractsDir = Paths.get(javaClass.classLoader.getResource("dummyContracts")!!.path)
    private val unparsableContractsDir = Paths.get(javaClass.classLoader.getResource("unparsableContracts")!!.path)
    private val ignoredDirs = listOf("IgnoredContract")

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var contractMetadataRepository: ContractMetadataRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyLoadInitialContractDecoratorsAndInterfaces() {
        val contractDecoratorRepository = InMemoryContractDecoratorRepository()
        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("mock contract interfaces will be returned") {
            call(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(
                    InterfaceManifestJson(
                        name = null,
                        description = null,
                        tags = emptySet(),
                        eventDecorators = emptyList(),
                        functionDecorators = emptyList()
                    )
                )
        }

        suppose("initial contract decorators and interfaces will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractInterfacesRepository = contractInterfacesRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                contractsDir = parsableContractsDir,
                interfacesDir = interfacesDir,
                ignoredDirs = ignoredDirs
            )
        }

        val exampleInterfaceId = InterfaceId("example")
        val nestedInterfaceId = InterfaceId("nested")
        val otherNestedInterfaceId = InterfaceId("nested/other")

        verify("correct contract interfaces have been loaded") {
            expectInteractions(contractInterfacesRepository) {
                once.getById(InterfaceId("trait.another"))
                twice.getById(InterfaceId("trait.example"))

                once.store(exampleInterfaceId, EMPTY_CONTRACT_INTERFACE)
                once.store(exampleInterfaceId, "")

                once.store(nestedInterfaceId, EMPTY_CONTRACT_INTERFACE)
                once.store(nestedInterfaceId, "")

                once.store(otherNestedInterfaceId, EMPTY_CONTRACT_INTERFACE)
                once.store(otherNestedInterfaceId, "")
            }
        }

        val dummyContractId = ContractId("DummyContractSet/DummyContract")
        val deeplyNestedContractId = ContractId("DummyContractSet/Deeply/Nested/Contract")
        val anotherContractId = ContractId("AnotherContractSet/AnotherContract")
        val contractWithoutArtifactId = ContractId("DummyContractSet/ContractWithoutArtifact")
        val contractWithoutManifestId = ContractId("DummyContractSet/ContractWithoutManifest")
        val ignoredContractId = ContractId("AnotherContractSet/IgnoredContract")

        verify("correct contract decorators have been loaded") {
            val dummyDecorator = contractDecoratorRepository.getById(dummyContractId)!!

            expectThat(dummyDecorator)
                .isEqualTo(
                    ContractDecorator(
                        id = dummyContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(InterfaceId("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = dummyDecorator.manifest,
                        artifact = dummyDecorator.artifact
                    )
                )

            val deeplyNestedDecorator = contractDecoratorRepository.getById(deeplyNestedContractId)!!

            expectThat(deeplyNestedDecorator)
                .isEqualTo(
                    ContractDecorator(
                        id = deeplyNestedContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0123456"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(InterfaceId("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = deeplyNestedDecorator.manifest,
                        artifact = deeplyNestedDecorator.artifact
                    )
                )

            expectThat(contractDecoratorRepository.getById(contractWithoutArtifactId))
                .isNull()
            expectThat(contractDecoratorRepository.getById(contractWithoutManifestId))
                .isNull()

            val anotherDecorator = contractDecoratorRepository.getById(anotherContractId)!!

            expectThat(anotherDecorator)
                .isEqualTo(
                    ContractDecorator(
                        id = anotherContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x2"),
                        tags = listOf(ContractTag("tag.another")),
                        implements = listOf(InterfaceId("trait.another")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = anotherDecorator.manifest,
                        artifact = anotherDecorator.artifact
                    )
                )

            expectThat(contractDecoratorRepository.getById(ignoredContractId))
                .isNull()
        }

        verify("correct contract metadata exists in the database") {
            expectThat(contractMetadataRepository.exists(dummyContractId, Constants.NIL_PROJECT_ID))
                .isTrue()
            expectThat(contractMetadataRepository.exists(deeplyNestedContractId, Constants.NIL_PROJECT_ID))
                .isTrue()
            expectThat(contractMetadataRepository.exists(anotherContractId, Constants.NIL_PROJECT_ID))
                .isTrue()

            expectThat(contractMetadataRepository.exists(contractWithoutArtifactId, Constants.NIL_PROJECT_ID))
                .isFalse()
            expectThat(contractMetadataRepository.exists(contractWithoutManifestId, Constants.NIL_PROJECT_ID))
                .isFalse()
            expectThat(contractMetadataRepository.exists(ignoredContractId, Constants.NIL_PROJECT_ID))
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyReloadContractsAndInterfacesAfterSomeFileChangesHaveBeenDetected() {
        val contractDecoratorRepository = InMemoryContractDecoratorRepository()
        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("mock contract interfaces will be returned") {
            call(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(
                    InterfaceManifestJson(
                        name = null,
                        description = null,
                        tags = emptySet(),
                        eventDecorators = emptyList(),
                        functionDecorators = emptyList()
                    )
                )
        }

        val listener = suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractInterfacesRepository = contractInterfacesRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                contractsDir = parsableContractsDir,
                interfacesDir = interfacesDir,
                ignoredDirs = ignoredDirs
            )
        }

        val dummyContractId = ContractId("DummyContractSet/DummyContract")
        val deeplyNestedContractId = ContractId("DummyContractSet/Deeply/Nested/Contract")
        val anotherContractId = ContractId("AnotherContractSet/AnotherContract")

        suppose("existing contracts will be removed from repository") {
            contractDecoratorRepository.delete(dummyContractId)
            contractDecoratorRepository.delete(deeplyNestedContractId)
            contractDecoratorRepository.delete(anotherContractId)
        }

        val contractWithoutArtifactId = ContractId("DummyContractSet/ContractWithoutArtifact")
        val contractWithoutManifestId = ContractId("DummyContractSet/ContractWithoutManifest")

        val withoutManifestDecorator = ContractDecorator(
            id = contractWithoutManifestId,
            name = "name",
            description = "description",
            binary = ContractBinaryData("0x1"),
            tags = listOf(ContractTag("tag.no.manifest")),
            implements = listOf(InterfaceId("trait.no.manifest")),
            constructors = CONSTRUCTORS,
            functions = FUNCTIONS,
            events = EVENTS,
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )

        suppose("contract which needs to be deleted is in repository") {
            contractDecoratorRepository.store(
                ContractDecorator(
                    id = contractWithoutArtifactId,
                    name = "name",
                    description = "description",
                    binary = ContractBinaryData("0x4"),
                    tags = listOf(ContractTag("tag.no.artifact")),
                    implements = listOf(InterfaceId("trait.no.artifact")),
                    constructors = CONSTRUCTORS,
                    functions = FUNCTIONS,
                    events = EVENTS,
                    manifest = ManifestJson.EMPTY,
                    artifact = ArtifactJson.EMPTY
                )
            )
            contractDecoratorRepository.store(withoutManifestDecorator)
        }

        suppose("listener will get some file changes") {
            listener.onChange(
                setOf(
                    ChangedFiles(
                        parsableContractsDir.toFile(),
                        setOf(
                            ChangedFile(
                                parsableContractsDir.toFile(),
                                parsableContractsDir.resolve("DummyContractSet/DummyContract/artifact.json").toFile(),
                                ChangedFile.Type.ADD
                            ),
                            ChangedFile(
                                parsableContractsDir.toFile(),
                                parsableContractsDir.resolve("DummyContractSet/Deeply/Nested/Contract/artifact.json")
                                    .toFile(),
                                ChangedFile.Type.ADD
                            ),
                            ChangedFile(
                                parsableContractsDir.toFile(),
                                parsableContractsDir.resolve("DummyContractSet/ContractWithoutArtifact/artifact.json")
                                    .toFile(),
                                ChangedFile.Type.DELETE
                            )
                        )
                    ),
                    ChangedFiles(
                        interfacesDir.toFile(),
                        setOf(
                            ChangedFile(
                                interfacesDir.toFile(),
                                interfacesDir.resolve("NonExistentInterface/manifest.json").toFile(),
                                ChangedFile.Type.DELETE
                            ),
                            ChangedFile(
                                interfacesDir.toFile(),
                                interfacesDir.resolve("AnotherNonExistentInterface/info.md").toFile(),
                                ChangedFile.Type.DELETE
                            ),
                            ChangedFile(
                                interfacesDir.toFile(),
                                interfacesDir.resolve("AnotherNonExistentInterface/example.info.md").toFile(),
                                ChangedFile.Type.DELETE
                            )
                        )
                    )
                )
            )
        }

        verify("correct contracts have been updated in database") {
            expectInteractions(contractInterfacesRepository) {
                once.getById(InterfaceId("trait.another"))
                4.times.getById(InterfaceId("trait.example"))

                once.store(InterfaceId("nested.other"), EMPTY_CONTRACT_INTERFACE)
                once.store(InterfaceId("nested.other"), "")

                once.store(InterfaceId("nested"), EMPTY_CONTRACT_INTERFACE)
                once.store(InterfaceId("nested"), "")

                once.store(InterfaceId("example"), EMPTY_CONTRACT_INTERFACE)
                once.store(InterfaceId("example"), "")

                once.delete(InterfaceId("NonExistentInterface"))
                once.delete(InterfaceId("AnotherNonExistentInterface"))
                once.delete(InterfaceId("AnotherNonExistentInterface/example"))
            }
        }

        val ignoredContractId = ContractId("AnotherContractSet/IgnoredContract")

        verify("correct contract decorators have been updated in database") {
            val dummyDecorator = contractDecoratorRepository.getById(dummyContractId)!!

            expectThat(dummyDecorator)
                .isEqualTo(
                    ContractDecorator(
                        id = dummyContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(InterfaceId("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = dummyDecorator.manifest,
                        artifact = dummyDecorator.artifact
                    )
                )

            val deeplyNestedDecorator = contractDecoratorRepository.getById(deeplyNestedContractId)!!

            expectThat(deeplyNestedDecorator)
                .isEqualTo(
                    ContractDecorator(
                        id = deeplyNestedContractId,
                        name = "name",
                        description = "description",
                        binary = ContractBinaryData("0x0123456"),
                        tags = listOf(ContractTag("tag.example")),
                        implements = listOf(InterfaceId("trait.example")),
                        constructors = CONSTRUCTORS,
                        functions = FUNCTIONS,
                        events = EVENTS,
                        manifest = deeplyNestedDecorator.manifest,
                        artifact = deeplyNestedDecorator.artifact
                    )
                )

            expectThat(contractDecoratorRepository.getById(contractWithoutArtifactId))
                .isNull()
            expectThat(contractDecoratorRepository.getById(contractWithoutManifestId))
                .isEqualTo(withoutManifestDecorator)

            expectThat(contractDecoratorRepository.getById(anotherContractId))
                .isNull()
            expectThat(contractDecoratorRepository.getById(ignoredContractId))
                .isNull()
        }

        verify("correct contract metadata exists in the database") {
            expectThat(contractMetadataRepository.exists(dummyContractId, Constants.NIL_PROJECT_ID))
                .isTrue()
            expectThat(contractMetadataRepository.exists(deeplyNestedContractId, Constants.NIL_PROJECT_ID))
                .isTrue()
            expectThat(contractMetadataRepository.exists(anotherContractId, Constants.NIL_PROJECT_ID))
                .isTrue()

            expectThat(contractMetadataRepository.exists(contractWithoutArtifactId, Constants.NIL_PROJECT_ID))
                .isFalse()
            expectThat(contractMetadataRepository.exists(contractWithoutManifestId, Constants.NIL_PROJECT_ID))
                .isFalse()
            expectThat(contractMetadataRepository.exists(ignoredContractId, Constants.NIL_PROJECT_ID))
                .isFalse()
        }
    }

    @Test
    fun mustSkipUnparsableContractDecorators() {
        val contractDecoratorRepository = InMemoryContractDecoratorRepository()
        val contractInterfacesRepository = InMemoryContractInterfacesRepository()

        suppose("initial contract decorators will be loaded from file system") {
            ContractDecoratorFileChangeListener(
                uuidProvider = RandomUuidProvider(),
                contractDecoratorRepository = contractDecoratorRepository,
                contractInterfacesRepository = contractInterfacesRepository,
                contractMetadataRepository = contractMetadataRepository,
                objectMapper = JsonConfig().objectMapper(),
                contractsDir = unparsableContractsDir,
                interfacesDir = interfacesDir,
                ignoredDirs = ignoredDirs
            )
        }

        val unparsableArtifactContractId = ContractId("InvalidJson/UnparsableArtifact")
        val unparsableManifestContractId = ContractId("InvalidJson/UnparsableManifest")
        val missingConstructorSignatureContractId = ContractId("MissingValue/MissingConstructorSignature")
        val missingEventNameContractId = ContractId("MissingValue/MissingEventName")
        val missingFunctionNameContractId = ContractId("MissingValue/MissingFunctionName")
        val missingFunctionOutputsContractId = ContractId("MissingValue/MissingFunctionOutputs")

        verify("no contract decorators have been loaded") {
            expectThat(contractDecoratorRepository.getById(unparsableArtifactContractId))
                .isNull()
            expectThat(contractDecoratorRepository.getById(unparsableManifestContractId))
                .isNull()
            expectThat(contractDecoratorRepository.getById(missingConstructorSignatureContractId))
                .isNull()
            expectThat(contractDecoratorRepository.getById(missingEventNameContractId))
                .isNull()
            expectThat(contractDecoratorRepository.getById(missingFunctionNameContractId))
                .isNull()
            expectThat(contractDecoratorRepository.getById(missingFunctionOutputsContractId))
                .isNull()
        }

        verify("correct contract metadata exists in the database") {
            expectThat(contractMetadataRepository.exists(unparsableArtifactContractId, Constants.NIL_PROJECT_ID))
                .isFalse()
            expectThat(contractMetadataRepository.exists(unparsableManifestContractId, Constants.NIL_PROJECT_ID))
                .isFalse()
            expectThat(
                contractMetadataRepository.exists(
                    missingConstructorSignatureContractId,
                    Constants.NIL_PROJECT_ID
                )
            )
                .isFalse()
            expectThat(contractMetadataRepository.exists(missingEventNameContractId, Constants.NIL_PROJECT_ID))
                .isFalse()
            expectThat(contractMetadataRepository.exists(missingFunctionNameContractId, Constants.NIL_PROJECT_ID))
                .isFalse()
            expectThat(contractMetadataRepository.exists(missingFunctionOutputsContractId, Constants.NIL_PROJECT_ID))
                .isFalse()
        }
    }
}
