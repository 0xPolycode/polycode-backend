package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.blockchain.ExampleContract
import polycode.exception.ResourceNotFoundException
import polycode.features.contract.deployment.controller.ContractDecoratorController
import polycode.features.contract.deployment.model.filters.ContractDecoratorFilters
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
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
import polycode.features.contract.deployment.repository.ImportedContractDecoratorRepository
import polycode.generated.jooq.id.ProjectId
import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import java.util.UUID

class ContractDecoratorControllerTest : TestBase() {

    @Test
    fun mustCorrectlyFetchContractDecoratorsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = ContractDecorator(
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
                            recommendedTypes = listOf(),
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
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null,
                            hints = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = listOf(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )

        suppose("some contract decorators will be fetched with filters") {
            call(repository.getAll(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractDecorators(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = null
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDecoratorsResponse(
                            listOf(
                                ContractDecoratorResponse(
                                    id = result.id.value,
                                    name = result.name,
                                    description = result.description,
                                    binary = result.binary.value,
                                    tags = result.tags.map { it.value },
                                    implements = result.implements.map { it.value },
                                    constructors = result.constructors,
                                    functions = result.functions,
                                    events = result.events
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecoratorsWithFiltersAndProjectId() {
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ContractDecorator(
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
                            recommendedTypes = listOf(),
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
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null,
                            hints = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = listOf(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )
        val projectId = ProjectId(UUID.randomUUID())

        suppose("some contract decorators will be fetched with filters") {
            call(repository.getAll(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(emptyRepository(filters), repository)

        verify("controller returns correct response") {
            val response = controller.getContractDecorators(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = projectId
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDecoratorsResponse(
                            listOf(
                                ContractDecoratorResponse(
                                    id = result.id.value,
                                    name = result.name,
                                    description = result.description,
                                    binary = result.binary.value,
                                    tags = result.tags.map { it.value },
                                    implements = result.implements.map { it.value },
                                    constructors = result.constructors,
                                    functions = result.functions,
                                    events = result.events
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJsonsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )

        suppose("some contract manifest.json files will be fetched with filters") {
            call(repository.getAllManifestJsonFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractManifestJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = null
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ManifestJsonsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJsonsWithFiltersAndProjectId() {
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )
        val projectId = ProjectId(UUID.randomUUID())

        suppose("some contract manifest.json files will be fetched with filters") {
            call(repository.getAllManifestJsonFiles(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(emptyRepository(filters), repository)

        verify("controller returns correct response") {
            val response = controller.getContractManifestJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = projectId
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ManifestJsonsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJsonsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )

        suppose("some contract artifact.json files will be fetched with filters") {
            call(repository.getAllArtifactJsonFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = null
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ArtifactJsonsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJsonsWithFiltersAndProjectId() {
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )
        val projectId = ProjectId(UUID.randomUUID())

        suppose("some contract artifact.json files will be fetched with filters") {
            call(repository.getAllArtifactJsonFiles(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(emptyRepository(filters), repository)

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJsonFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = projectId
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ArtifactJsonsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdownsWithFilters() {
        val repository = mock<ContractDecoratorRepository>()
        val result = "info-md"

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )

        suppose("some contract info.md files will be fetched with filters") {
            call(repository.getAllInfoMarkdownFiles(filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdownFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = null
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(InfoMarkdownsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdownsWithFiltersAndProjectId() {
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = "info-md"

        val filters = ContractDecoratorFilters(
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2")))
        )
        val projectId = ProjectId(UUID.randomUUID())

        suppose("some contract info.md files will be fetched with filters") {
            call(repository.getAllInfoMarkdownFiles(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDecoratorController(emptyRepository(filters), repository)

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdownFiles(
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                projectId = projectId
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(InfoMarkdownsResponse(listOf(result))))
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecorator() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = ContractDecorator(
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
                            recommendedTypes = listOf(),
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
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null,
                            hints = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = listOf(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )

        suppose("some contract decorator will be fetched") {
            call(repository.getById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractDecorator(id.value, null)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDecoratorResponse(
                            id = result.id.value,
                            name = result.name,
                            description = result.description,
                            binary = result.binary.value,
                            tags = result.tags.map { it.value },
                            implements = result.implements.map { it.value },
                            constructors = result.constructors,
                            functions = result.functions,
                            events = result.events
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecoratorWithProjectId() {
        val id = ContractId("example")
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ContractDecorator(
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
                            recommendedTypes = listOf(),
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
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf(),
                            parameters = null,
                            hints = null
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = listOf(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )
        val projectId = ProjectId(UUID.randomUUID())

        suppose("some contract decorator will be fetched") {
            call(repository.getByContractIdAndProjectId(id, projectId))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(mock(), repository)

        verify("controller returns correct response") {
            val response = controller.getContractDecorator(id.value, projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDecoratorResponse(
                            id = result.id.value,
                            name = result.name,
                            description = result.description,
                            binary = result.binary.value,
                            tags = result.tags.map { it.value },
                            implements = result.implements.map { it.value },
                            constructors = result.constructors,
                            functions = result.functions,
                            events = result.events
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractDecoratorIsNotFound() {
        val repository = mock<ContractDecoratorRepository>()
        val id = ContractId("example")

        suppose("null will be returned from the repository") {
            call(repository.getById(id))
                .willReturn(null)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getContractDecorator(id.value, null)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJson() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract manifest.json will be fetched") {
            call(repository.getManifestJsonById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractManifestJson(id.value, null)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractManifestJsonWithProjectId() {
        val id = ContractId("example")
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
        val projectId = ProjectId(UUID.randomUUID())

        suppose("some contract manifest.json will be fetched") {
            call(repository.getManifestJsonByContractIdAndProjectId(id, projectId))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(mock(), repository)

        verify("controller returns correct response") {
            val response = controller.getContractManifestJson(id.value, projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractManifestJsonIsNotFound() {
        val repository = mock<ContractDecoratorRepository>()
        val id = ContractId("example")

        suppose("null will be returned from the repository") {
            call(repository.getManifestJsonById(id))
                .willReturn(null)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getContractManifestJson(id.value, null)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJson() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )

        suppose("some contract artifact.json will be fetched") {
            call(repository.getArtifactJsonById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJson(id.value, null)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractArtifactJsonWithProjectId() {
        val id = ContractId("example")
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        val projectId = ProjectId(UUID.randomUUID())

        suppose("some contract artifact.json will be fetched") {
            call(repository.getArtifactJsonByContractIdAndProjectId(id, projectId))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(mock(), repository)

        verify("controller returns correct response") {
            val response = controller.getContractArtifactJson(id.value, projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractArtifactJsonIsNotFound() {
        val repository = mock<ContractDecoratorRepository>()
        val id = ContractId("example")

        suppose("null will be returned from the repository") {
            call(repository.getArtifactJsonById(id))
                .willReturn(null)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getContractArtifactJson(id.value, null)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdown() {
        val id = ContractId("example")
        val repository = mock<ContractDecoratorRepository>()
        val result = "info-md"

        suppose("some contract info.md will be fetched") {
            call(repository.getInfoMarkdownById(id))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdown(id.value, null)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractInfoMarkdownWithProjectId() {
        val id = ContractId("example")
        val repository = mock<ImportedContractDecoratorRepository>()
        val result = "info-md"
        val projectId = ProjectId(UUID.randomUUID())

        suppose("some contract info.md will be fetched") {
            call(repository.getInfoMarkdownByContractIdAndProjectId(id, projectId))
                .willReturn(result)
        }

        val controller = ContractDecoratorController(mock(), repository)

        verify("controller returns correct response") {
            val response = controller.getContractInfoMarkdown(id.value, projectId)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(result)
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenContractInfoMarkdownIsNotFound() {
        val repository = mock<ContractDecoratorRepository>()
        val id = ContractId("example")

        suppose("null will be returned from the repository") {
            call(repository.getInfoMarkdownById(id))
                .willReturn(null)
        }

        val controller = ContractDecoratorController(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getContractInfoMarkdown(id.value, null)
            }
        }
    }

    private fun emptyRepository(filters: ContractDecoratorFilters): ContractDecoratorRepository {
        val repository = mock<ContractDecoratorRepository>()

        suppose("contract decorator repository is empty") {
            call(repository.getAll(filters)).willReturn(emptyList())
            call(repository.getAllManifestJsonFiles(filters)).willReturn(emptyList())
            call(repository.getAllArtifactJsonFiles(filters)).willReturn(emptyList())
            call(repository.getAllInfoMarkdownFiles(filters)).willReturn(emptyList())
        }

        return repository
    }
}
