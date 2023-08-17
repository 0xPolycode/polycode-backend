package polycode.model.result

import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.features.contract.deployment.model.json.AbiInputOutput
import polycode.features.contract.deployment.model.json.AbiObject
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.EventDecorator
import polycode.features.contract.deployment.model.json.EventTypeDecorator
import polycode.features.contract.deployment.model.json.FunctionDecorator
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.json.TypeDecorator
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.model.result.ContractEvent
import polycode.features.contract.deployment.model.result.ContractFunction
import polycode.features.contract.deployment.model.result.ContractParameter
import polycode.features.contract.deployment.model.result.EventParameter
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId

class ContractDecoratorTest : TestBase() {

    companion object {
        private val ARTIFACT_JSON = ArtifactJson(
            contractName = "Test",
            sourceName = "Test.sol",
            abi = listOf(
                constructorAbi("string"),
                constructorAbi("uint"),
                constructorAbi("int"),
                constructorAbi("bool"),
                functionAbi("fromDecorator", "string"),
                functionAbi("fromOverride1", "uint"),
                functionAbi("fromOverride2", "int"),
                functionAbi("extraFunction", "bool"),
                eventAbi("FromDecorator", "string"),
                eventAbi("FromOverride1", "uint"),
                eventAbi("FromOverride2", "int"),
                eventAbi("ExtraEvent", "bool")
            ),
            bytecode = "0",
            deployedBytecode = "0",
            linkReferences = null,
            deployedLinkReferences = null
        )
        private val MANIFEST_JSON = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = setOf("override-1", "override-2", "extra"),
            eventDecorators = listOf(
                eventDecorator("FromDecorator", "string", "not-overridden-1")
            ),
            constructorDecorators = emptyList(),
            functionDecorators = listOf(
                functionDecorator("fromDecorator", "string", "not-overridden-1")
            )
        )
        private val INTERFACE_MANIFEST_OVERRIDE_1 = InterfaceManifestJson(
            name = "name",
            description = "description",
            tags = setOf("interface-tag"),
            eventDecorators = listOf(
                eventDecorator("FromDecorator", "string", "overridden-1-1"),
                eventDecorator("FromOverride1", "uint", "overridden-1-2")
            ),
            functionDecorators = listOf(
                functionDecorator("fromDecorator", "string", "overridden-1-1"),
                functionDecorator("fromOverride1", "uint", "overridden-1-2")
            )
        )
        private val INTERFACE_MANIFEST_OVERRIDE_2 = InterfaceManifestJson(
            name = "name",
            description = "description",
            tags = setOf("interface-tag"),
            eventDecorators = listOf(
                eventDecorator("FromDecorator", "string", "overridden-2-1"),
                eventDecorator("FromOverride1", "uint", "overridden-2-2"),
                eventDecorator("FromOverride2", "int", "overridden-2-3")
            ),
            functionDecorators = listOf(
                functionDecorator("fromDecorator", "string", "overridden-2-1"),
                functionDecorator("fromOverride1", "uint", "overridden-2-2"),
                functionDecorator("fromOverride2", "int", "overridden-2-3")
            )
        )
        private val INTERFACE_MANIFEST_EXTRA = InterfaceManifestJson(
            name = "name",
            description = "description",
            tags = setOf("interface-tag"),
            eventDecorators = listOf(
                eventDecorator("FromDecorator", "string", "extra-1"),
                eventDecorator("FromOverride1", "uint", "extra-2"),
                eventDecorator("FromOverride2", "int", "extra-3"),
                eventDecorator("ExtraEvent", "bool", "extra-4")
            ),
            functionDecorators = listOf(
                functionDecorator("fromDecorator", "string", "extra-1"),
                functionDecorator("fromOverride1", "uint", "extra-2"),
                functionDecorator("fromOverride2", "int", "extra-3"),
                functionDecorator("extraFunction", "bool", "extra-4")
            )
        )

        private fun constructorAbi(argType: String) =
            AbiObject(
                anonymous = null,
                inputs = listOf(
                    AbiInputOutput(
                        components = null,
                        internalType = argType,
                        name = "arg1",
                        type = argType,
                        indexed = null
                    )
                ),
                outputs = null,
                stateMutability = null,
                name = "",
                type = "constructor"
            )

        private fun functionAbi(name: String, argType: String) =
            AbiObject(
                anonymous = null,
                inputs = listOf(
                    AbiInputOutput(
                        components = null,
                        internalType = argType,
                        name = "arg1",
                        type = argType,
                        indexed = null
                    )
                ),
                outputs = emptyList(),
                stateMutability = null,
                name = name,
                type = "function"
            )

        private fun eventAbi(name: String, argType: String) =
            AbiObject(
                anonymous = null,
                inputs = listOf(
                    AbiInputOutput(
                        components = null,
                        internalType = argType,
                        name = "arg1",
                        type = argType,
                        indexed = null
                    )
                ),
                outputs = emptyList(),
                stateMutability = null,
                name = name,
                type = "event"
            )

        private fun functionDecorator(name: String, argType: String, description: String) =
            FunctionDecorator(
                signature = "$name($argType)",
                name = description,
                description = description,
                parameterDecorators = listOf(
                    TypeDecorator(
                        name = "Arg1",
                        description = "Arg1",
                        recommendedTypes = emptyList(),
                        parameters = null,
                        hints = null
                    )
                ),
                returnDecorators = emptyList(),
                emittableEvents = emptyList(),
                readOnly = false
            )

        private fun eventDecorator(name: String, argType: String, description: String) =
            EventDecorator(
                signature = "$name($argType)",
                name = description,
                description = description,
                parameterDecorators = listOf(
                    EventTypeDecorator(
                        name = "Arg1",
                        description = "Arg1",
                        indexed = false,
                        recommendedTypes = emptyList(),
                        parameters = null,
                        hints = null
                    )
                )
            )
    }

    @Test
    fun mustCorrectlyOverrideFunctionsAndEventsForNonImportedDecorator() {
        val map = mapOf(
            InterfaceId("override-1") to INTERFACE_MANIFEST_OVERRIDE_1,
            InterfaceId("override-2") to INTERFACE_MANIFEST_OVERRIDE_2,
            InterfaceId("extra") to INTERFACE_MANIFEST_EXTRA
        )
        val interfacesProvider: (InterfaceId) -> InterfaceManifestJson? = { id -> map[id] }

        verify("functions and events are correctly overridden") {
            val id = ContractId("contract-id")
            val result = ContractDecorator(
                id = id,
                artifact = ARTIFACT_JSON,
                manifest = MANIFEST_JSON,
                imported = false,
                interfacesProvider = interfacesProvider
            )

            expectThat(result)
                .isEqualTo(
                    ContractDecorator(
                        id = id,
                        name = MANIFEST_JSON.name,
                        description = MANIFEST_JSON.description,
                        binary = ContractBinaryData(ARTIFACT_JSON.bytecode),
                        tags = MANIFEST_JSON.tags.map { ContractTag(it) } + ContractTag("interface-tag"),
                        implements = MANIFEST_JSON.implements.map { InterfaceId(it) },
                        constructors = emptyList(),
                        functions = listOf(
                            ContractFunction(
                                name = "not-overridden-1",
                                description = "not-overridden-1",
                                solidityName = "fromDecorator",
                                signature = "fromDecorator(string)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "string",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "overridden-1-2",
                                description = "overridden-1-2",
                                solidityName = "fromOverride1",
                                signature = "fromOverride1(uint)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "uint",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "overridden-2-3",
                                description = "overridden-2-3",
                                solidityName = "fromOverride2",
                                signature = "fromOverride2(int)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "int",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "extra-4",
                                description = "extra-4",
                                solidityName = "extraFunction",
                                signature = "extraFunction(bool)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "bool",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            )
                        ),
                        events = listOf(
                            ContractEvent(
                                name = "not-overridden-1",
                                description = "not-overridden-1",
                                solidityName = "FromDecorator",
                                signature = "FromDecorator(string)",
                                inputs = listOf(
                                    EventParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        indexed = false,
                                        solidityType = "string",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "overridden-1-2",
                                description = "overridden-1-2",
                                solidityName = "FromOverride1",
                                signature = "FromOverride1(uint)",
                                inputs = listOf(
                                    EventParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        indexed = false,
                                        solidityType = "uint",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "overridden-2-3",
                                description = "overridden-2-3",
                                solidityName = "FromOverride2",
                                signature = "FromOverride2(int)",
                                inputs = listOf(
                                    EventParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        indexed = false,
                                        solidityType = "int",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "extra-4",
                                description = "extra-4",
                                solidityName = "ExtraEvent",
                                signature = "ExtraEvent(bool)",
                                inputs = listOf(
                                    EventParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        indexed = false,
                                        solidityType = "bool",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                )
                            )
                        ),
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyOverrideFunctionsAndEventsForImportedDecorator() {
        val map = mapOf(
            InterfaceId("override-1") to INTERFACE_MANIFEST_OVERRIDE_1,
            InterfaceId("override-2") to INTERFACE_MANIFEST_OVERRIDE_2,
            InterfaceId("extra") to INTERFACE_MANIFEST_EXTRA
        )
        val interfacesProvider: (InterfaceId) -> InterfaceManifestJson? = { id -> map[id] }

        verify("functions and events are correctly overridden") {
            val id = ContractId("contract-id")
            val result = ContractDecorator(
                id = id,
                artifact = ARTIFACT_JSON,
                manifest = MANIFEST_JSON,
                imported = true,
                interfacesProvider = interfacesProvider
            )

            expectThat(result)
                .isEqualTo(
                    ContractDecorator(
                        id = id,
                        name = MANIFEST_JSON.name,
                        description = MANIFEST_JSON.description,
                        binary = ContractBinaryData(ARTIFACT_JSON.bytecode),
                        tags = MANIFEST_JSON.tags.map { ContractTag(it) } + ContractTag("interface-tag"),
                        implements = MANIFEST_JSON.implements.map { InterfaceId(it) },
                        constructors = emptyList(),
                        functions = listOf(
                            ContractFunction(
                                name = "overridden-1-1",
                                description = "overridden-1-1",
                                solidityName = "fromDecorator",
                                signature = "fromDecorator(string)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "string",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "overridden-1-2",
                                description = "overridden-1-2",
                                solidityName = "fromOverride1",
                                signature = "fromOverride1(uint)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "uint",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "overridden-2-3",
                                description = "overridden-2-3",
                                solidityName = "fromOverride2",
                                signature = "fromOverride2(int)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "int",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            ),
                            ContractFunction(
                                name = "extra-4",
                                description = "extra-4",
                                solidityName = "extraFunction",
                                signature = "extraFunction(bool)",
                                inputs = listOf(
                                    ContractParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        solidityType = "bool",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                ),
                                outputs = emptyList(),
                                emittableEvents = emptyList(),
                                readOnly = false
                            )
                        ),
                        events = listOf(
                            ContractEvent(
                                name = "overridden-1-1",
                                description = "overridden-1-1",
                                solidityName = "FromDecorator",
                                signature = "FromDecorator(string)",
                                inputs = listOf(
                                    EventParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        indexed = false,
                                        solidityType = "string",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "overridden-1-2",
                                description = "overridden-1-2",
                                solidityName = "FromOverride1",
                                signature = "FromOverride1(uint)",
                                inputs = listOf(
                                    EventParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        indexed = false,
                                        solidityType = "uint",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "overridden-2-3",
                                description = "overridden-2-3",
                                solidityName = "FromOverride2",
                                signature = "FromOverride2(int)",
                                inputs = listOf(
                                    EventParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        indexed = false,
                                        solidityType = "int",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                )
                            ),
                            ContractEvent(
                                name = "extra-4",
                                description = "extra-4",
                                solidityName = "ExtraEvent",
                                signature = "ExtraEvent(bool)",
                                inputs = listOf(
                                    EventParameter(
                                        name = "Arg1",
                                        description = "Arg1",
                                        indexed = false,
                                        solidityType = "bool",
                                        solidityName = "arg1",
                                        recommendedTypes = emptyList(),
                                        parameters = null,
                                        hints = null
                                    )
                                )
                            )
                        ),
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON
                    )
                )
        }
    }
}
