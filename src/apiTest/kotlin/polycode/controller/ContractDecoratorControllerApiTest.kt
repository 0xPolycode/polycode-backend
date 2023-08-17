package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import polycode.ControllerTestBase
import polycode.blockchain.ExampleContract
import polycode.exception.ErrorCode
import polycode.features.contract.deployment.model.filters.ContractDecoratorFilters
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ConstructorDecorator
import polycode.features.contract.deployment.model.json.FunctionDecorator
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.json.ReturnTypeDecorator
import polycode.features.contract.deployment.model.json.TypeDecorator
import polycode.features.contract.deployment.model.response.ArtifactJsonsResponse
import polycode.features.contract.deployment.model.response.ContractDecoratorResponse
import polycode.features.contract.deployment.model.response.ContractDecoratorsResponse
import polycode.features.contract.deployment.model.response.InfoMarkdownsResponse
import polycode.features.contract.deployment.model.response.ManifestJsonsResponse
import polycode.features.contract.deployment.model.result.ContractConstructor
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.model.result.ContractFunction
import polycode.features.contract.deployment.model.result.ContractParameter
import polycode.features.contract.deployment.repository.ContractDecoratorRepository
import polycode.model.filters.OrList
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import java.util.UUID

class ContractDecoratorControllerApiTest : ControllerTestBase() {

    companion object {
        private val MANIFEST_JSON = ManifestJson(
            name = "name",
            description = "description",
            tags = setOf("example", "simple"),
            implements = setOf("traits.example", "traits.exampleOwnable"),
            eventDecorators = emptyList(),
            constructorDecorators = listOf(
                ConstructorDecorator(
                    signature = "constructor(address)",
                    description = "Main constructor",
                    parameterDecorators = listOf(
                        TypeDecorator(
                            name = "Owner address",
                            description = "Contract owner address",
                            recommendedTypes = emptyList(),
                            parameters = null,
                            hints = null
                        )
                    )
                )
            ),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "getOwner()",
                    name = "Get contract owner",
                    description = "Fetches contract owner",
                    parameterDecorators = emptyList(),
                    returnDecorators = listOf(
                        ReturnTypeDecorator(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityType = "address",
                            recommendedTypes = emptyList(),
                            parameters = null,
                            hints = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = false
                )
            )
        )
        private val ARTIFACT_JSON = ArtifactJson(
            contractName = "ExampleContract",
            sourceName = "ExampleContract.sol",
            abi = listOf(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        private const val INFO_MD = "# info.md file contents"
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = ContractId("examples.exampleContract"),
            name = "name",
            description = "description",
            binary = ContractBinaryData(ExampleContract.BINARY),
            tags = listOf(ContractTag("example"), ContractTag("simple")),
            implements = listOf(InterfaceId("traits.example"), InterfaceId("traits.exampleOwnable")),
            constructors = listOf(
                ContractConstructor(
                    inputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "owner",
                            solidityType = "address",
                            recommendedTypes = emptyList(),
                            parameters = null,
                            hints = null
                        )
                    ),
                    description = "Main constructor",
                    payable = true
                )
            ),
            functions = listOf(
                ContractFunction(
                    name = "Get contract owner",
                    description = "Fetches contract owner",
                    solidityName = "getOwner",
                    signature = "getOwner()",
                    inputs = emptyList(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = emptyList(),
                            parameters = null,
                            hints = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = emptyList(),
            manifest = MANIFEST_JSON,
            artifact = ARTIFACT_JSON
        )
    }

    @Autowired
    private lateinit var contractDecoratorRepository: ContractDecoratorRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        contractDecoratorRepository.getAll(ContractDecoratorFilters(OrList(), OrList())).forEach {
            contractDecoratorRepository.delete(it.id)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecoratorsWithFilters() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val response = suppose("request to fetch contract decorators is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractDecoratorsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractDecoratorsResponse(
                        listOf(
                            ContractDecoratorResponse(
                                id = CONTRACT_DECORATOR.id.value,
                                name = CONTRACT_DECORATOR.name,
                                description = CONTRACT_DECORATOR.description,
                                binary = CONTRACT_DECORATOR.binary.value,
                                tags = CONTRACT_DECORATOR.tags.map { it.value },
                                implements = CONTRACT_DECORATOR.implements.map { it.value },
                                constructors = CONTRACT_DECORATOR.constructors,
                                functions = CONTRACT_DECORATOR.functions,
                                events = CONTRACT_DECORATOR.events
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestsWithFilters() {
        suppose("some contract manifest.json exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, MANIFEST_JSON)
        }

        val response = suppose("request to fetch contract manifest.json files is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/manifest.json?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ManifestJsonsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(ManifestJsonsResponse(listOf(MANIFEST_JSON)))
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactsWithFilters() {
        suppose("some contract artifact.json exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, ARTIFACT_JSON)
        }

        val response = suppose("request to fetch contract artifact.json files is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/artifact.json?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ArtifactJsonsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(
                response.copy(
                    artifacts = response.artifacts.map {
                        it.copy(linkReferences = null, deployedLinkReferences = null)
                    }
                )
            ).isEqualTo(ArtifactJsonsResponse(listOf(ARTIFACT_JSON)))
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdownsWithFilters() {
        suppose("some contract info.md exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, INFO_MD)
        }

        val response = suppose("request to fetch contract info.md files is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/info.md?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, InfoMarkdownsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(InfoMarkdownsResponse(listOf(INFO_MD)))
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecorator() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val response = suppose("request to fetch contract decorator is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractDecoratorResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractDecoratorResponse(
                        id = CONTRACT_DECORATOR.id.value,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        binary = CONTRACT_DECORATOR.binary.value,
                        tags = CONTRACT_DECORATOR.tags.map { it.value },
                        implements = CONTRACT_DECORATOR.implements.map { it.value },
                        constructors = CONTRACT_DECORATOR.constructors,
                        functions = CONTRACT_DECORATOR.functions,
                        events = CONTRACT_DECORATOR.events
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractDecorator() {
        verify("404 is returned for non-existent contract decorator") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJson() {
        suppose("some contract manifest.json exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, MANIFEST_JSON)
        }

        val response = suppose("request to fetch contract manifest.json is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}/manifest.json")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ManifestJson::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(MANIFEST_JSON)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractManifestJson() {
        verify("404 is returned for non-existent contract manifest.json") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}/manifest.json")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJson() {
        suppose("some contract artifact.json exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, ARTIFACT_JSON)
        }

        val response = suppose("request to fetch contract artifact.json is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}/artifact.json")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ArtifactJson::class.java)
        }

        verify("correct response is returned") {
            expectThat(response.copy(linkReferences = null, deployedLinkReferences = null))
                .isEqualTo(ARTIFACT_JSON)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractArtifactJson() {
        verify("404 is returned for non-existent contract artifact.json") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}/artifact.json")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdown() {
        suppose("some contract info.md exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
            contractDecoratorRepository.store(CONTRACT_DECORATOR.id, INFO_MD)
        }

        val response = suppose("request to fetch contract info.md is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}/info.md")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
                .response
                .contentAsString
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(INFO_MD)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractInfoMarkdown() {
        verify("404 is returned for non-existent contract info.md") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}/info.md")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }
}
