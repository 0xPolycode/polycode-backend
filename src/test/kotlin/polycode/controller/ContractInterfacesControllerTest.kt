package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.exception.ResourceNotFoundException
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import polycode.features.contract.deployment.model.response.InfoMarkdownsResponse
import polycode.features.contract.interfaces.controller.ContractInterfacesController
import polycode.features.contract.interfaces.model.filters.ContractInterfaceFilters
import polycode.features.contract.interfaces.model.response.ContractInterfaceManifestResponse
import polycode.features.contract.interfaces.model.response.ContractInterfaceManifestsResponse
import polycode.features.contract.interfaces.repository.ContractInterfacesRepository
import polycode.model.filters.OrList
import polycode.util.InterfaceId

class ContractInterfacesControllerTest : TestBase() {

    @Test
    fun mustCorrectlyFetchContractInterfaces() {
        val repository = mock<ContractInterfacesRepository>()
        val result = InterfaceManifestJsonWithId(
            id = InterfaceId("interface-id"),
            name = "name",
            description = "description",
            tags = emptySet(),
            matchingEventDecorators = emptyList(),
            matchingFunctionDecorators = emptyList()
        )

        suppose("some contract interfaces will be fetched") {
            call(repository.getAll(ContractInterfaceFilters(OrList(emptyList()))))
                .willReturn(listOf(result))
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaces(emptyList())

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractInterfaceManifestsResponse(
                            listOf(
                                ContractInterfaceManifestResponse(
                                    id = result.id.value,
                                    name = result.name,
                                    description = result.description,
                                    tags = result.tags.toList(),
                                    eventDecorators = result.matchingEventDecorators,
                                    functionDecorators = result.matchingFunctionDecorators
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterfaceInfoMarkdowns() {
        val repository = mock<ContractInterfacesRepository>()
        val result = "info-md"

        suppose("some contract interface info.md files will be fetched") {
            call(repository.getAllInfoMarkdownFiles(ContractInterfaceFilters(OrList(emptyList()))))
                .willReturn(listOf(result))
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaceInfoMarkdownFiles(emptyList())

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(InfoMarkdownsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterface() {
        val id = InterfaceId("example")
        val repository = mock<ContractInterfacesRepository>()
        val result = InterfaceManifestJson(
            name = "name",
            description = "description",
            tags = setOf("interface-tag"),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interface will be fetched") {
            call(repository.getById(id))
                .willReturn(result)
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterface(id.value)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractInterfaceManifestResponse(
                            id = id.value,
                            name = result.name,
                            description = result.description,
                            tags = result.tags.toList(),
                            eventDecorators = result.eventDecorators,
                            functionDecorators = result.functionDecorators
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractInterfaceIsNotFound() {
        val repository = mock<ContractInterfacesRepository>()
        val id = InterfaceId("example")

        suppose("null will be returned from the repository") {
            call(repository.getById(id))
                .willReturn(null)
        }

        val controller = ContractInterfacesController(repository)

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getContractInterface(id.value)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractInterfaceInfoMarkdown() {
        val id = InterfaceId("example")
        val repository = mock<ContractInterfacesRepository>()
        val result = "info-md"

        suppose("some contract interface info.md will be fetched") {
            call(repository.getInfoMarkdownById(id))
                .willReturn(result)
        }

        val controller = ContractInterfacesController(repository)

        verify("controller returns correct response") {
            val response = controller.getContractInterfaceInfoMarkdown(id.value)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(result))
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractInterfaceInfoMarkdownIsNotFound() {
        val repository = mock<ContractInterfacesRepository>()
        val id = InterfaceId("example")

        suppose("null will be returned from the repository") {
            call(repository.getInfoMarkdownById(id))
                .willReturn(null)
        }

        val controller = ContractInterfacesController(repository)

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getContractInterfaceInfoMarkdown(id.value)
            }
        }
    }
}
