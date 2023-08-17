package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import polycode.ControllerTestBase
import polycode.exception.ErrorCode
import polycode.features.contract.deployment.model.json.FunctionDecorator
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.ReturnTypeDecorator
import polycode.features.contract.deployment.model.response.InfoMarkdownsResponse
import polycode.features.contract.interfaces.model.filters.ContractInterfaceFilters
import polycode.features.contract.interfaces.model.response.ContractInterfaceManifestResponse
import polycode.features.contract.interfaces.model.response.ContractInterfaceManifestsResponse
import polycode.features.contract.interfaces.repository.ContractInterfacesRepository
import polycode.model.filters.OrList
import polycode.util.InterfaceId
import java.util.UUID

class ContractInterfacesControllerApiTest : ControllerTestBase() {

    companion object {
        private val ID = InterfaceId("example.interface")
        private val INTERFACE_MANIFEST_JSON = InterfaceManifestJson(
            name = "name",
            description = "description",
            tags = setOf("interface-tag", "another-interface-tag"),
            eventDecorators = emptyList(),
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
        private val INFO_MD = "# info.md file contents"
    }

    @Autowired
    private lateinit var contractInterfacesRepository: ContractInterfacesRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        contractInterfacesRepository.getAll(ContractInterfaceFilters(OrList(emptyList()))).forEach {
            contractInterfacesRepository.delete(it.id)
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterfaces() {
        suppose("some contract interface exists in the database") {
            contractInterfacesRepository.store(ID, INTERFACE_MANIFEST_JSON)
        }

        val response = suppose("request to fetch contract interfaces is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/contract-interfaces?tags=interface-tag AND another-interface-tag,other-tag"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractInterfaceManifestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractInterfaceManifestsResponse(
                        listOf(
                            ContractInterfaceManifestResponse(
                                id = ID.value,
                                name = INTERFACE_MANIFEST_JSON.name,
                                tags = INTERFACE_MANIFEST_JSON.tags.toList(),
                                description = INTERFACE_MANIFEST_JSON.description,
                                eventDecorators = INTERFACE_MANIFEST_JSON.eventDecorators,
                                functionDecorators = INTERFACE_MANIFEST_JSON.functionDecorators
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterfaceInfoMarkdowns() {
        suppose("some contract interface info.md exists in the database") {
            contractInterfacesRepository.store(ID, INTERFACE_MANIFEST_JSON)
            contractInterfacesRepository.store(ID, INFO_MD)
        }

        val response = suppose("request to fetch contract interface info.md files is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/contract-interfaces/info.md?tags=interface-tag AND another-interface-tag,other-tag"
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
    fun mustCorrectlyFetchContractInterfaceById() {
        suppose("some contract interface exists in the database") {
            contractInterfacesRepository.store(ID, INTERFACE_MANIFEST_JSON)
        }

        val response = suppose("request to fetch contract interface is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/contract-interfaces/${ID.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractInterfaceManifestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractInterfaceManifestResponse(
                        id = ID.value,
                        name = INTERFACE_MANIFEST_JSON.name,
                        tags = INTERFACE_MANIFEST_JSON.tags.toList(),
                        description = INTERFACE_MANIFEST_JSON.description,
                        eventDecorators = INTERFACE_MANIFEST_JSON.eventDecorators,
                        functionDecorators = INTERFACE_MANIFEST_JSON.functionDecorators
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractInterface() {
        verify("404 is returned for non-existent contract interface") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/contract-interfaces/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterfaceInfoMarkdownById() {
        suppose("some contract interface info.md exists in the database") {
            contractInterfacesRepository.store(ID, INFO_MD)
        }

        val response = suppose("request to fetch contract interface info.md is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/contract-interfaces/${ID.value}/info.md")
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
    fun mustReturn404NotFoundForNonExistentContractInfoInterfaceMarkdown() {
        verify("404 is returned for non-existent contract interface info.md") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/contract-interfaces/${UUID.randomUUID()}/info.md")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }
}
