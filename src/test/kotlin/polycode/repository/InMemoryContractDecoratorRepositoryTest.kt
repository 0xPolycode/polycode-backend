package polycode.repository

import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.features.contract.deployment.model.filters.ContractDecoratorFilters
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.repository.InMemoryContractDecoratorRepository
import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import java.util.UUID

class InMemoryContractDecoratorRepositoryTest : TestBase() {

    @Test
    fun mustCorrectlyStoreAndThenGetContractDecoratorById() {
        val repository = InMemoryContractDecoratorRepository()
        val decorator = ContractDecorator(
            id = ContractId("example"),
            name = "name",
            description = "description",
            binary = ContractBinaryData("0x0"),
            tags = emptyList(),
            implements = emptyList(),
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )

        val storedDecorator = suppose("some contract decorator is stored") {
            repository.store(decorator)
        }

        verify("correct contract decorator is returned") {
            expectThat(storedDecorator)
                .isEqualTo(decorator)
            expectThat(repository.getById(decorator.id))
                .isEqualTo(decorator)
        }
    }

    @Test
    fun mustCorrectlyStoreAndThenGetContractManifestJsonById() {
        val repository = InMemoryContractDecoratorRepository()
        val manifestJson = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
        val id = ContractId("test")

        val storedManifest = suppose("some contract manifest.json is stored") {
            repository.store(id, manifestJson)
        }

        verify("correct contract manifest.json is returned") {
            expectThat(storedManifest)
                .isEqualTo(manifestJson)
            expectThat(repository.getManifestJsonById(id))
                .isEqualTo(manifestJson)
        }
    }

    @Test
    fun mustCorrectlyStoreAndThenGetContractArtifactJsonById() {
        val repository = InMemoryContractDecoratorRepository()
        val artifactJson = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        val id = ContractId("test")

        val storedArtifact = suppose("some contract artifact.json is stored") {
            repository.store(id, artifactJson)
        }

        verify("correct contract artifact.json is returned") {
            expectThat(storedArtifact)
                .isEqualTo(artifactJson)
            expectThat(repository.getArtifactJsonById(id))
                .isEqualTo(artifactJson)
        }
    }

    @Test
    fun mustCorrectlyStoreAndThenGetContractInfoMarkdownById() {
        val repository = InMemoryContractDecoratorRepository()
        val infoMd = "info-md"
        val id = ContractId("test")

        val storedInfoMd = suppose("some contract info.md is stored") {
            repository.store(id, infoMd)
        }

        verify("correct contract info.md is returned") {
            expectThat(storedInfoMd)
                .isEqualTo(infoMd)
            expectThat(repository.getInfoMarkdownById(id))
                .isEqualTo(infoMd)
        }
    }

    @Test
    fun mustCorrectlyDeleteContractDecoratorAndThenReturnNullWhenGettingById() {
        val repository = InMemoryContractDecoratorRepository()
        val decorator = ContractDecorator(
            id = ContractId("example"),
            name = "name",
            description = "description",
            binary = ContractBinaryData("0x0"),
            tags = emptyList(),
            implements = emptyList(),
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )

        suppose("some contract decorator is stored") {
            repository.store(decorator)
        }

        verify("correct contract decorator is returned") {
            expectThat(repository.getById(decorator.id))
                .isEqualTo(decorator)
        }

        val manifestJson = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )

        suppose("some contract manifest.json is stored") {
            repository.store(decorator.id, manifestJson)
        }

        verify("correct contract manifest.json is returned") {
            expectThat(repository.getManifestJsonById(decorator.id))
                .isEqualTo(manifestJson)
        }

        val artifactJson = ArtifactJson(
            contractName = "example",
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )

        suppose("some contract artifact.json is stored") {
            repository.store(decorator.id, artifactJson)
        }

        verify("correct contract artifact.json is returned") {
            expectThat(repository.getArtifactJsonById(decorator.id))
                .isEqualTo(artifactJson)
        }

        val infoMd = "info-md"

        suppose("some contract info.md is stored") {
            repository.store(decorator.id, infoMd)
        }

        verify("correct contract info.md is returned") {
            expectThat(repository.getInfoMarkdownById(decorator.id))
                .isEqualTo(infoMd)
        }

        suppose("contract decorator is deleted") {
            repository.delete(decorator.id)
        }

        verify("null is returned") {
            expectThat(repository.getById(decorator.id))
                .isNull()
            expectThat(repository.getManifestJsonById(decorator.id))
                .isNull()
            expectThat(repository.getArtifactJsonById(decorator.id))
                .isNull()
            expectThat(repository.getInfoMarkdownById(decorator.id))
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyGetAllContractDecoratorsWithSomeTagFilters() {
        val matching = listOf(
            decorator(tags = listOf(ContractTag("1"), ContractTag("2"))),
            decorator(tags = listOf(ContractTag("3"))),
            decorator(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"))),
            decorator(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("extra"))),
            decorator(tags = listOf(ContractTag("3"), ContractTag("extra"))),
            decorator(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"), ContractTag("extra")))
        )
        val nonMatching = listOf(
            decorator(tags = listOf(ContractTag("1"))),
            decorator(tags = listOf(ContractTag("2"))),
            decorator(tags = listOf(ContractTag("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators are stored") {
            all.forEach { repository.store(it) }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(
                AndList(ContractTag("1"), ContractTag("2")),
                AndList(ContractTag("3"))
            ),
            contractImplements = OrList()
        )

        verify("correct contract decorators are returned") {
            expectThat(repository.getAll(filters))
                .containsExactlyInAnyOrderElementsOf(matching)
            expectThat(repository.getAll(ContractDecoratorFilters(OrList(), OrList())))
                .containsExactlyInAnyOrderElementsOf(all)
        }
    }

    @Test
    fun mustCorrectlyGetAllContractDecoratorsWithSomeImplementsFilters() {
        val matching = listOf(
            decorator(implements = listOf(InterfaceId("1"), InterfaceId("2"))),
            decorator(implements = listOf(InterfaceId("3"))),
            decorator(implements = listOf(InterfaceId("1"), InterfaceId("2"), InterfaceId("3"))),
            decorator(implements = listOf(InterfaceId("1"), InterfaceId("2"), InterfaceId("extra"))),
            decorator(implements = listOf(InterfaceId("3"), InterfaceId("tag"))),
            decorator(
                implements = listOf(
                    InterfaceId("1"), InterfaceId("2"), InterfaceId("3"), InterfaceId("extra")
                )
            )
        )
        val nonMatching = listOf(
            decorator(implements = listOf(InterfaceId("1"))),
            decorator(implements = listOf(InterfaceId("2"))),
            decorator(implements = listOf(InterfaceId("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators are stored") {
            all.forEach { repository.store(it) }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(),
            contractImplements = OrList(
                AndList(InterfaceId("1"), InterfaceId("2")),
                AndList(InterfaceId("3"))
            )
        )

        verify("correct contract decorators are returned") {
            expectThat(repository.getAll(filters))
                .containsExactlyInAnyOrderElementsOf(matching)
            expectThat(repository.getAll(ContractDecoratorFilters(OrList(), OrList())))
                .containsExactlyInAnyOrderElementsOf(all)
        }
    }

    @Test
    fun mustCorrectlyGetAllContractManifestJsonsWithSomeTagFilters() {
        val matching = listOf(
            decoratorAndManifest(tags = listOf(ContractTag("1"), ContractTag("2"))),
            decoratorAndManifest(tags = listOf(ContractTag("3"))),
            decoratorAndManifest(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"))),
            decoratorAndManifest(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("extra"))),
            decoratorAndManifest(tags = listOf(ContractTag("3"), ContractTag("extra"))),
            decoratorAndManifest(
                tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"), ContractTag("extra"))
            )
        )
        val nonMatching = listOf(
            decoratorAndManifest(tags = listOf(ContractTag("1"))),
            decoratorAndManifest(tags = listOf(ContractTag("2"))),
            decoratorAndManifest(tags = listOf(ContractTag("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators and manifest.json files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(
                AndList(ContractTag("1"), ContractTag("2")),
                AndList(ContractTag("3"))
            ),
            contractImplements = OrList()
        )

        verify("correct contract manifest.json files are returned") {
            expectThat(repository.getAllManifestJsonFiles(filters))
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            expectThat(repository.getAllManifestJsonFiles(ContractDecoratorFilters(OrList(), OrList())))
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractManifestJsonsWithSomeImplementsFilters() {
        val matching = listOf(
            decoratorAndManifest(implements = listOf(InterfaceId("1"), InterfaceId("2"))),
            decoratorAndManifest(implements = listOf(InterfaceId("3"))),
            decoratorAndManifest(implements = listOf(InterfaceId("1"), InterfaceId("2"), InterfaceId("3"))),
            decoratorAndManifest(implements = listOf(InterfaceId("1"), InterfaceId("2"), InterfaceId("extra"))),
            decoratorAndManifest(implements = listOf(InterfaceId("3"), InterfaceId("tag"))),
            decoratorAndManifest(
                implements = listOf(
                    InterfaceId("1"), InterfaceId("2"), InterfaceId("3"), InterfaceId("extra")
                )
            )
        )
        val nonMatching = listOf(
            decoratorAndManifest(implements = listOf(InterfaceId("1"))),
            decoratorAndManifest(implements = listOf(InterfaceId("2"))),
            decoratorAndManifest(implements = listOf(InterfaceId("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract manifest.json files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(),
            contractImplements = OrList(
                AndList(InterfaceId("1"), InterfaceId("2")),
                AndList(InterfaceId("3"))
            )
        )

        verify("correct contract manifest.json files are returned") {
            expectThat(repository.getAllManifestJsonFiles(filters))
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            expectThat(repository.getAllManifestJsonFiles(ContractDecoratorFilters(OrList(), OrList())))
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractArtifactJsonsWithSomeTagFilters() {
        val matching = listOf(
            decoratorAndArtifact(tags = listOf(ContractTag("1"), ContractTag("2"))),
            decoratorAndArtifact(tags = listOf(ContractTag("3"))),
            decoratorAndArtifact(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"))),
            decoratorAndArtifact(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("extra"))),
            decoratorAndArtifact(tags = listOf(ContractTag("3"), ContractTag("extra"))),
            decoratorAndArtifact(
                tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"), ContractTag("extra"))
            )
        )
        val nonMatching = listOf(
            decoratorAndArtifact(tags = listOf(ContractTag("1"))),
            decoratorAndArtifact(tags = listOf(ContractTag("2"))),
            decoratorAndArtifact(tags = listOf(ContractTag("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators and artifact.json files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(
                AndList(ContractTag("1"), ContractTag("2")),
                AndList(ContractTag("3"))
            ),
            contractImplements = OrList()
        )

        verify("correct contract artifact.json files are returned") {
            expectThat(repository.getAllArtifactJsonFiles(filters))
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            expectThat(repository.getAllArtifactJsonFiles(ContractDecoratorFilters(OrList(), OrList())))
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractArtifactJsonsWithSomeImplementsFilters() {
        val matching = listOf(
            decoratorAndArtifact(implements = listOf(InterfaceId("1"), InterfaceId("2"))),
            decoratorAndArtifact(implements = listOf(InterfaceId("3"))),
            decoratorAndArtifact(implements = listOf(InterfaceId("1"), InterfaceId("2"), InterfaceId("3"))),
            decoratorAndArtifact(implements = listOf(InterfaceId("1"), InterfaceId("2"), InterfaceId("extra"))),
            decoratorAndArtifact(implements = listOf(InterfaceId("3"), InterfaceId("tag"))),
            decoratorAndArtifact(
                implements = listOf(
                    InterfaceId("1"), InterfaceId("2"), InterfaceId("3"), InterfaceId("extra")
                )
            )
        )
        val nonMatching = listOf(
            decoratorAndArtifact(implements = listOf(InterfaceId("1"))),
            decoratorAndArtifact(implements = listOf(InterfaceId("2"))),
            decoratorAndArtifact(implements = listOf(InterfaceId("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract artifact.json files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(),
            contractImplements = OrList(
                AndList(InterfaceId("1"), InterfaceId("2")),
                AndList(InterfaceId("3"))
            )
        )

        verify("correct contract artifact.json files are returned") {
            expectThat(repository.getAllArtifactJsonFiles(filters))
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            expectThat(repository.getAllArtifactJsonFiles(ContractDecoratorFilters(OrList(), OrList())))
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractInfoMarkdownsWithSomeTagFilters() {
        val matching = listOf(
            decoratorAndInfoMd(tags = listOf(ContractTag("1"), ContractTag("2"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("3"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("extra"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("3"), ContractTag("extra"))),
            decoratorAndInfoMd(
                tags = listOf(ContractTag("1"), ContractTag("2"), ContractTag("3"), ContractTag("extra"))
            )
        )
        val nonMatching = listOf(
            decoratorAndInfoMd(tags = listOf(ContractTag("1"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("2"))),
            decoratorAndInfoMd(tags = listOf(ContractTag("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract decorators and info.md files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(
                AndList(ContractTag("1"), ContractTag("2")),
                AndList(ContractTag("3"))
            ),
            contractImplements = OrList()
        )

        verify("correct contract info.md files are returned") {
            expectThat(repository.getAllInfoMarkdownFiles(filters))
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            expectThat(repository.getAllInfoMarkdownFiles(ContractDecoratorFilters(OrList(), OrList())))
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    @Test
    fun mustCorrectlyGetAllContractInfoMarkdownsWithSomeImplementsFilters() {
        val matching = listOf(
            decoratorAndInfoMd(implements = listOf(InterfaceId("1"), InterfaceId("2"))),
            decoratorAndInfoMd(implements = listOf(InterfaceId("3"))),
            decoratorAndInfoMd(implements = listOf(InterfaceId("1"), InterfaceId("2"), InterfaceId("3"))),
            decoratorAndInfoMd(implements = listOf(InterfaceId("1"), InterfaceId("2"), InterfaceId("extra"))),
            decoratorAndInfoMd(implements = listOf(InterfaceId("3"), InterfaceId("tag"))),
            decoratorAndInfoMd(
                implements = listOf(
                    InterfaceId("1"), InterfaceId("2"), InterfaceId("3"), InterfaceId("extra")
                )
            )
        )
        val nonMatching = listOf(
            decoratorAndInfoMd(implements = listOf(InterfaceId("1"))),
            decoratorAndInfoMd(implements = listOf(InterfaceId("2"))),
            decoratorAndInfoMd(implements = listOf(InterfaceId("extra")))
        )
        val all = matching + nonMatching

        val repository = InMemoryContractDecoratorRepository()

        suppose("some contract info.md files are stored") {
            all.forEach {
                repository.store(it.first)
                repository.store(it.first.id, it.second)
            }
        }

        val filters = ContractDecoratorFilters(
            contractTags = OrList(),
            contractImplements = OrList(
                AndList(InterfaceId("1"), InterfaceId("2")),
                AndList(InterfaceId("3"))
            )
        )

        verify("correct contract info.md files are returned") {
            expectThat(repository.getAllInfoMarkdownFiles(filters))
                .containsExactlyInAnyOrderElementsOf(matching.map { it.second })
            expectThat(repository.getAllInfoMarkdownFiles(ContractDecoratorFilters(OrList(), OrList())))
                .containsExactlyInAnyOrderElementsOf(all.map { it.second })
        }
    }

    private fun decorator(tags: List<ContractTag> = emptyList(), implements: List<InterfaceId> = emptyList()) =
        ContractDecorator(
            id = ContractId(UUID.randomUUID().toString()),
            name = "name",
            description = "description",
            binary = ContractBinaryData("0x0"),
            tags = tags,
            implements = implements,
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )

    private fun decoratorAndManifest(
        tags: List<ContractTag> = emptyList(),
        implements: List<InterfaceId> = emptyList()
    ) = Pair(
        decorator(tags, implements),
        ManifestJson(
            name = "name",
            description = "description",
            tags = tags.map { it.value }.toSet(),
            implements = implements.map { it.value }.toSet(),
            eventDecorators = emptyList(),
            constructorDecorators = emptyList(),
            functionDecorators = emptyList()
        )
    )

    private fun decoratorAndArtifact(
        tags: List<ContractTag> = emptyList(),
        implements: List<InterfaceId> = emptyList()
    ) = Pair(
        decorator(tags, implements),
        ArtifactJson(
            contractName = UUID.randomUUID().toString(),
            sourceName = "Example",
            abi = emptyList(),
            bytecode = "0x0",
            deployedBytecode = "0x0",
            linkReferences = null,
            deployedLinkReferences = null
        )
    )

    private fun decoratorAndInfoMd(
        tags: List<ContractTag> = emptyList(),
        implements: List<InterfaceId> = emptyList()
    ) = Pair(
        decorator(tags, implements),
        "info-md-${UUID.randomUUID()}"
    )
}
