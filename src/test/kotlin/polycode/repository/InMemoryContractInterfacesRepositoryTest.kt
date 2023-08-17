package polycode.repository

import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.features.contract.deployment.model.json.EventDecorator
import polycode.features.contract.deployment.model.json.FunctionDecorator
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import polycode.features.contract.interfaces.model.filters.ContractInterfaceFilters
import polycode.features.contract.interfaces.repository.InMemoryContractInterfacesRepository
import polycode.model.filters.OrList
import polycode.util.InterfaceId

class InMemoryContractInterfacesRepositoryTest : TestBase() {

    @Test
    fun mustCorrectlyStoreAndThenGetContractInterfaceById() {
        val repository = InMemoryContractInterfacesRepository()
        val id = InterfaceId("id")
        val interfaceManifest = InterfaceManifestJson(
            name = "name",
            description = "description",
            tags = setOf("interface-tag"),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val storedInterface = suppose("some contract interface is stored") {
            repository.store(id, interfaceManifest)
        }

        verify("correct contract decorator is returned") {
            expectThat(storedInterface)
                .isEqualTo(interfaceManifest)
            expectThat(repository.getById(id))
                .isEqualTo(interfaceManifest)
        }
    }

    @Test
    fun mustCorrectlyStoreAndThenGetContractInterfaceInfoMarkdownById() {
        val repository = InMemoryContractInterfacesRepository()
        val infoMd = "info-md"
        val id = InterfaceId("id")

        val storedInfoMd = suppose("some contract interface info.md is stored") {
            repository.store(id, infoMd)
        }

        verify("correct contract interface info.md is returned") {
            expectThat(storedInfoMd)
                .isEqualTo(infoMd)
            expectThat(repository.getInfoMarkdownById(id))
                .isEqualTo(infoMd)
        }
    }

    @Test
    fun mustCorrectlyDeleteContractInterfaceAndThenReturnNullWhenGettingById() {
        val repository = InMemoryContractInterfacesRepository()
        val id = InterfaceId("id")
        val interfaceManifest = InterfaceManifestJson(
            name = "name",
            description = "description",
            tags = setOf("interface-tag"),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interface is stored") {
            repository.store(id, interfaceManifest)
        }

        verify("correct contract interface is returned") {
            expectThat(repository.getById(id))
                .isEqualTo(interfaceManifest)
        }

        val infoMd = "info-md"

        suppose("some contract info.md is stored") {
            repository.store(id, infoMd)
        }

        verify("correct contract interface info.md is returned") {
            expectThat(repository.getInfoMarkdownById(id))
                .isEqualTo(infoMd)
        }

        suppose("contract interface is deleted") {
            repository.delete(id)
        }

        verify("null is returned") {
            expectThat(repository.getById(id))
                .isNull()
            expectThat(repository.getInfoMarkdownById(id))
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyGetAllContractInterfaces() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = InterfaceId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            tags = setOf("interface-tag-1"),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val id2 = InterfaceId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            tags = setOf("interface-tag-2"),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        verify("correct contract interfaces are returned") {
            expectThat(repository.getAll(ContractInterfaceFilters(OrList(emptyList()))))
                .containsExactlyInAnyOrderElementsOf(
                    listOf(
                        InterfaceManifestJsonWithId(
                            id = id1,
                            name = interfaceManifest1.name,
                            description = interfaceManifest1.description,
                            tags = interfaceManifest1.tags,
                            matchingEventDecorators = interfaceManifest1.eventDecorators,
                            matchingFunctionDecorators = interfaceManifest1.functionDecorators
                        ),
                        InterfaceManifestJsonWithId(
                            id = id2,
                            name = interfaceManifest2.name,
                            description = interfaceManifest2.description,
                            tags = interfaceManifest2.tags,
                            matchingEventDecorators = interfaceManifest2.eventDecorators,
                            matchingFunctionDecorators = interfaceManifest2.functionDecorators
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyGetAllContractInterfaceInfoMarkdownFiles() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = InterfaceId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            tags = setOf("interface-tag-1"),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        val id2 = InterfaceId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            tags = setOf("interface-tag-2"),
            eventDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        val infoMd1 = "info-md-1"
        val infoMd2 = "info-md-2"

        suppose("some contract interface info.md files are stored") {
            repository.store(id1, infoMd1)
            repository.store(id2, infoMd2)
        }

        verify("correct contract interfaces are returned") {
            expectThat(repository.getAllInfoMarkdownFiles(ContractInterfaceFilters(OrList(emptyList()))))
                .containsExactlyInAnyOrderElementsOf(listOf(infoMd1, infoMd2))
        }
    }

    @Test
    fun mustCorrectlyGetAllPartiallyContractInterfacesWhenThereIsAFullMatch() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = InterfaceId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            tags = setOf("interface-tag-1"),
            eventDecorators = listOf(
                simpleEventDecorator("Event(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("function(string)")
            )
        )

        val id2 = InterfaceId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            tags = setOf("interface-tag-2"),
            eventDecorators = listOf(
                simpleEventDecorator("NonMatchingEvent(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("nonMatchingFunction(string)")
            )
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        verify("correct contract interfaces are returned") {
            expectThat(
                repository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = setOf("function(string)"),
                    abiEventSignatures = setOf("Event(string)")
                )
            ).containsExactlyInAnyOrderElementsOf(
                listOf(
                    InterfaceManifestJsonWithId(
                        id = id1,
                        name = "name-1",
                        description = "description-1",
                        tags = interfaceManifest1.tags,
                        matchingEventDecorators = interfaceManifest1.eventDecorators,
                        matchingFunctionDecorators = interfaceManifest1.functionDecorators
                    )
                )
            )
        }
    }

    @Test
    fun mustCorrectlyGetAllPartiallyContractInterfacesWhenThereIsAPartialMatch() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = InterfaceId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            tags = setOf("interface-tag-1"),
            eventDecorators = listOf(
                simpleEventDecorator("Event(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("function(string)")
            )
        )

        val id2 = InterfaceId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            tags = setOf("interface-tag-2"),
            eventDecorators = listOf(
                simpleEventDecorator("NonMatchingEvent(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("nonMatchingFunction(string)")
            )
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        verify("correct contract interfaces are returned") {
            expectThat(
                repository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = setOf("function(string)", "anotherFunction(string)"),
                    abiEventSignatures = setOf("Event(string)", "AnotherEvent(string)")
                )
            ).containsExactlyInAnyOrderElementsOf(
                listOf(
                    InterfaceManifestJsonWithId(
                        id = id1,
                        name = "name-1",
                        description = "description-1",
                        tags = interfaceManifest1.tags,
                        matchingEventDecorators = interfaceManifest1.eventDecorators,
                        matchingFunctionDecorators = interfaceManifest1.functionDecorators
                    )
                )
            )
        }
    }

    @Test
    fun mustNotGetPartiallyContractInterfacesWhenInterfaceHasSignaturesMoreThanAbi() {
        val repository = InMemoryContractInterfacesRepository()

        val id1 = InterfaceId("id-1")
        val interfaceManifest1 = InterfaceManifestJson(
            name = "name-1",
            description = "description-1",
            tags = setOf("interface-tag-1"),
            eventDecorators = listOf(
                simpleEventDecorator("Event(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("function(string)")
            )
        )

        val id2 = InterfaceId("id-2")
        val interfaceManifest2 = InterfaceManifestJson(
            name = "name-2",
            description = "description-2",
            tags = setOf("interface-tag-2"),
            eventDecorators = listOf(
                simpleEventDecorator("NonMatchingEvent(string)")
            ),
            functionDecorators = listOf(
                simpleFunctionDecorator("nonMatchingFunction(string)")
            )
        )

        suppose("some contract interfaces are stored") {
            repository.store(id1, interfaceManifest1)
            repository.store(id2, interfaceManifest2)
        }

        verify("correct contract interfaces are returned") {
            expectThat(
                repository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = emptySet(),
                    abiEventSignatures = emptySet()
                )
            ).isEmpty()
        }
    }

    private fun simpleEventDecorator(signature: String) =
        EventDecorator(
            signature = signature,
            name = "",
            description = "",
            parameterDecorators = emptyList()
        )

    private fun simpleFunctionDecorator(signature: String) =
        FunctionDecorator(
            signature = signature,
            name = "",
            description = "",
            parameterDecorators = emptyList(),
            returnDecorators = emptyList(),
            emittableEvents = emptyList(),
            readOnly = false
        )
}
