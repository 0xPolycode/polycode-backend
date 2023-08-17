package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.tx.gas.DefaultGasProvider
import polycode.ControllerTestBase
import polycode.TestData
import polycode.blockchain.ExampleContract
import polycode.config.CustomHeaders
import polycode.exception.ErrorCode
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.params.StoreContractDeploymentRequestParams
import polycode.features.contract.deployment.model.result.ContractConstructor
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.model.result.ContractEvent
import polycode.features.contract.deployment.model.result.ContractFunction
import polycode.features.contract.deployment.model.result.ContractParameter
import polycode.features.contract.deployment.model.result.EventParameter
import polycode.features.contract.deployment.repository.ContractDecoratorRepository
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.functioncall.model.params.StoreContractFunctionCallRequestParams
import polycode.features.contract.functioncall.model.response.ContractFunctionCallRequestResponse
import polycode.features.contract.functioncall.model.response.ContractFunctionCallRequestsResponse
import polycode.features.contract.functioncall.model.result.ContractFunctionCallRequest
import polycode.features.contract.functioncall.repository.ContractFunctionCallRequestRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractFunctionCallRequestId
import polycode.generated.jooq.id.ContractMetadataId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ApiKeyRecord
import polycode.generated.jooq.tables.records.ContractMetadataRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.model.ScreenConfig
import polycode.model.response.EventArgumentResponse
import polycode.model.response.EventArgumentResponseType
import polycode.model.response.EventInfoResponse
import polycode.model.response.TransactionResponse
import polycode.testcontainers.HardhatTestContainer
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.Constants
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import polycode.util.Status
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

class ContractFunctionCallRequestControllerApiTest : ControllerTestBase() {

    companion object {
        private val PROJECT_ID = ProjectId(UUID.randomUUID())
        private val OWNER_ID = UserId(UUID.randomUUID())
        private val PROJECT = Project(
            id = PROJECT_ID,
            ownerId = OWNER_ID,
            baseRedirectUrl = BaseUrl("https://example.com/"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )
        private const val API_KEY = "api-key"
        private val CONTRACT_DECORATOR_ID = ContractId("examples.exampleContract")
        private val DEPLOYED_CONTRACT = StoreContractDeploymentRequestParams(
            id = ContractDeploymentRequestId(UUID.randomUUID()),
            alias = "contract-alias",
            contractData = ContractBinaryData("00"),
            contractId = CONTRACT_DECORATOR_ID,
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            deployerAddress = null,
            initialEthAmount = Balance.ZERO,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "redirect-url",
            projectId = PROJECT_ID,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = CONTRACT_DECORATOR_ID,
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
            events = listOf(
                ContractEvent(
                    name = "Example event",
                    description = "Example event",
                    solidityName = "ExampleEvent",
                    signature = "ExampleEvent(tuple(address),tuple(address))",
                    inputs = listOf(
                        EventParameter(
                            name = "Non-indexed struct",
                            description = "Non-indexed struct",
                            indexed = false,
                            solidityName = "nonIndexedStruct",
                            solidityType = "tuple",
                            recommendedTypes = emptyList(),
                            parameters = listOf(
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
                            hints = null
                        ),
                        EventParameter(
                            name = "Indexed struct",
                            description = "Indexed struct",
                            indexed = true,
                            solidityName = "indexedStruct",
                            solidityType = "tuple",
                            recommendedTypes = emptyList(),
                            parameters = listOf(
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
                            hints = null
                        )
                    )
                )
            ),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )
        private val EVENTS = listOf(
            EventInfoResponse(
                signature = "ExampleEvent(tuple(address),tuple(address))",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "nonIndexedStruct",
                        type = EventArgumentResponseType.VALUE,
                        value = listOf(HardhatTestContainer.ACCOUNT_ADDRESS_1),
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "indexedStruct",
                        type = EventArgumentResponseType.HASH,
                        value = null,
                        hash = "0xb3ab459503754d7ffbceb026c9a12971f082734e588c5e09df0875237fd918a2"
                    )
                )
            )
        )
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

    @Autowired
    private lateinit var contractFunctionCallRequestRepository: ContractFunctionCallRequestRepository

    @Autowired
    private lateinit var contractDeploymentRequestRepository: ContractDeploymentRequestRepository

    @Autowired
    private lateinit var contractDecoratorRepository: ContractDecoratorRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            ContractMetadataRecord(
                id = ContractMetadataId(UUID.randomUUID()),
                name = CONTRACT_DECORATOR.name,
                description = CONTRACT_DECORATOR.description,
                contractId = CONTRACT_DECORATOR.id,
                contractTags = CONTRACT_DECORATOR.tags.map { it.value }.toTypedArray(),
                contractImplements = CONTRACT_DECORATOR.implements.map { it.value }.toTypedArray(),
                projectId = Constants.NIL_PROJECT_ID
            )
        )

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT.id,
                ownerId = PROJECT.ownerId,
                baseRedirectUrl = PROJECT.baseRedirectUrl,
                chainId = PROJECT.chainId,
                customRpcUrl = PROJECT.customRpcUrl,
                createdAt = PROJECT.createdAt
            )
        )

        dslContext.executeInsert(
            ApiKeyRecord(
                id = ApiKeyId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                apiKey = API_KEY,
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequestViaDeployedContractId() {
        val callerAddress = WalletAddress("b")
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")
        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract function call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractFunctionCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = response.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = response.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract function call request is correctly stored in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress,
                        txHash = null
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequestViaDeployedContractAlias() {
        val callerAddress = WalletAddress("b")
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")
        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract function call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractFunctionCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = response.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = response.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract function call request is correctly stored in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress,
                        txHash = null
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequestViaDeployedContractAddress() {
        val callerAddress = WalletAddress("b")
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val contractAddress = ContractAddress("cafebabe")

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract function call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractFunctionCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = null,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = response.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = response.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract function call request is correctly stored in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = response.id,
                        deployedContractId = null,
                        contractAddress = contractAddress,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress,
                        txHash = null
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateContractFunctionCallRequestWithRedirectUrl() {
        val callerAddress = WalletAddress("b")
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO
        val redirectUrl = "https://custom-url/\${id}"

        val contractAddress = ContractAddress("cafebabe")
        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract function call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "redirect_url": "$redirectUrl",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractFunctionCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = response.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = response.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract function call request is correctly stored in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractFunctionCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress,
                        txHash = null
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenCreatingContractFunctionCallRequestWithAllContractIdentifiers() {
        val callerAddress = WalletAddress("b")
        val contractAddress = ContractAddress("cafebabe")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
            contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
        }

        verify("400 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "contract_address": "${contractAddress.rawValue}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.INVALID_REQUEST_BODY)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenCreatingContractFunctionCallRequestWithNoContractIdentifiers() {
        val callerAddress = WalletAddress("b")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
        }

        verify("400 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.INVALID_REQUEST_BODY)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenCreatingContractFunctionCallRequestForNonExistentContractId() {
        val callerAddress = WalletAddress("b")

        verify("404 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${UUID.randomUUID()}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenCreatingContractFunctionCallRequestForNonExistentContractAlias() {
        val callerAddress = WalletAddress("b")

        verify("404 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "non-existent-alias",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenCreatingContractFunctionCallRequestViaDeployedContractIdForNonDeployedContract() {
        val callerAddress = WalletAddress("b")

        suppose("some non-deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
        }

        verify("400 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.CONTRACT_NOT_DEPLOYED)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenCreatingContractFunctionCallRequestViaDeployedContractAliasForNonDeployedContract() {
        val callerAddress = WalletAddress("b")

        suppose("some non-deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
        }

        verify("400 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.CONTRACT_NOT_DEPLOYED)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenCreatingContractFunctionCallRequestWithInvalidApiKey() {
        val callerAddress = WalletAddress("b")

        verify("401 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${UUID.randomUUID()}",
                                "function_name": "setOwner",
                                "function_params": [],
                                "eth_amount": "0",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequest() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val callerAddress = WalletAddress(mainAccount.address)
        val contractAddress = ContractAddress(contract.contractAddress)
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract function call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract function call request") {
            contractFunctionCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract function call request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/function-call/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = createResponse.id,
                        status = Status.SUCCESS,
                        deployedContractId = DEPLOYED_CONTRACT.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = createResponse.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${createResponse.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = fetchResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = txHash.value,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = createResponse.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = fetchResponse.functionCallTx.blockConfirmations,
                            timestamp = fetchResponse.functionCallTx.timestamp
                        ),
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.functionCallTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.functionCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestWhenCustomRpcUrlIsSpecified() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val callerAddress = WalletAddress(mainAccount.address)
        val contractAddress = ContractAddress(contract.contractAddress)
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(
                params = DEPLOYED_CONTRACT.copy(
                    projectId = projectId,
                    chainId = chainId
                ),
                metadataProjectId = Constants.NIL_PROJECT_ID
            ).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract function call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract function call request") {
            contractFunctionCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract function call request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/function-call/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractFunctionCallRequestResponse(
                        id = createResponse.id,
                        status = Status.SUCCESS,
                        deployedContractId = DEPLOYED_CONTRACT.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = createResponse.functionCallData,
                        ethAmount = ethAmount.rawValue,
                        chainId = chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-function-call/${createResponse.id.value}/action",
                        projectId = projectId,
                        createdAt = fetchResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        functionCallTx = TransactionResponse(
                            txHash = txHash.value,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = createResponse.functionCallTx.data,
                            value = ethAmount.rawValue,
                            blockConfirmations = fetchResponse.functionCallTx.blockConfirmations,
                            timestamp = fetchResponse.functionCallTx.timestamp
                        ),
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.functionCallTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.functionCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractFunctionCallRequest() {
        verify("404 is returned for non-existent contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/function-call/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestsByProjectIdAndFilters() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val callerAddress = WalletAddress(mainAccount.address)
        val contractAddress = ContractAddress(contract.contractAddress)
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract function call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${storedContract.id.value}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract function call request") {
            contractFunctionCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract function call requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/function-call/by-project/${createResponse.projectId.value}" +
                        "?deployedContractId=${storedContract.id.value}" +
                        "&contractAddress=${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractFunctionCallRequestsResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractFunctionCallRequestsResponse(
                        listOf(
                            ContractFunctionCallRequestResponse(
                                id = createResponse.id,
                                status = Status.SUCCESS,
                                deployedContractId = storedContract.id,
                                contractAddress = contractAddress.rawValue,
                                functionName = functionName,
                                functionParams = objectMapper.readTree(paramsJson),
                                functionCallData = createResponse.functionCallData,
                                ethAmount = ethAmount.rawValue,
                                chainId = PROJECT.chainId.value,
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-function-call/${createResponse.id.value}/action",
                                projectId = PROJECT_ID,
                                createdAt = fetchResponse.requests[0].createdAt,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                callerAddress = callerAddress.rawValue,
                                functionCallTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = callerAddress.rawValue,
                                    to = contractAddress.rawValue,
                                    data = createResponse.functionCallTx.data,
                                    value = ethAmount.rawValue,
                                    blockConfirmations = fetchResponse.requests[0].functionCallTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].functionCallTx.timestamp
                                ),
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].functionCallTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].functionCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractFunctionCallRequestsByProjectIdAndFiltersWhenCustomRpcUrlIsSpecified() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        val callerAddress = WalletAddress(mainAccount.address)
        val contractAddress = ContractAddress(contract.contractAddress)
        val functionName = "setOwner"
        val ethAmount = Balance.ZERO

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val storedContract = suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${callerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract function call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/function-call")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${storedContract.id.value}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "eth_amount": "${ethAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                },
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractFunctionCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract function call request") {
            contractFunctionCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract function call requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/function-call/by-project/${createResponse.projectId.value}" +
                        "?deployedContractId=${storedContract.id.value}" +
                        "&contractAddress=${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractFunctionCallRequestsResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractFunctionCallRequestsResponse(
                        listOf(
                            ContractFunctionCallRequestResponse(
                                id = createResponse.id,
                                status = Status.SUCCESS,
                                deployedContractId = storedContract.id,
                                contractAddress = contractAddress.rawValue,
                                functionName = functionName,
                                functionParams = objectMapper.readTree(paramsJson),
                                functionCallData = createResponse.functionCallData,
                                ethAmount = ethAmount.rawValue,
                                chainId = chainId.value,
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-function-call/${createResponse.id.value}/action",
                                projectId = projectId,
                                createdAt = fetchResponse.requests[0].createdAt,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                callerAddress = callerAddress.rawValue,
                                functionCallTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = callerAddress.rawValue,
                                    to = contractAddress.rawValue,
                                    data = createResponse.functionCallTx.data,
                                    value = ethAmount.rawValue,
                                    blockConfirmations = fetchResponse.requests[0].functionCallTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].functionCallTx.timestamp
                                ),
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].functionCallTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].functionCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val id = ContractFunctionCallRequestId(UUID.randomUUID())
        val callerAddress = WalletAddress("c")

        suppose("some contract function call request without transaction info exists in database") {
            contractFunctionCallRequestRepository.store(
                StoreContractFunctionCallRequestParams(
                    id = id,
                    deployedContractId = null,
                    contractAddress = ContractAddress("a"),
                    functionName = "test",
                    functionParams = TestData.EMPTY_JSON_ARRAY,
                    ethAmount = Balance(BigInteger.TEN),
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    callerAddress = WalletAddress("b")
                )
            )
        }

        val txHash = TransactionHash("0x1")

        suppose("request to attach transaction info to contract deployment request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/function-call/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${txHash.value}",
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("transaction info is correctly attached to contract deployment request") {
            val storedRequest = contractFunctionCallRequestRepository.getById(id)

            expectThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionInfoIsNotAttached() {
        val id = ContractFunctionCallRequestId(UUID.randomUUID())
        val txHash = TransactionHash("0x1")
        val callerAddress = WalletAddress("c")

        suppose("some contract function call request with transaction info exists in database") {
            contractFunctionCallRequestRepository.store(
                StoreContractFunctionCallRequestParams(
                    id = id,
                    deployedContractId = null,
                    contractAddress = ContractAddress("a"),
                    functionName = "test",
                    functionParams = TestData.EMPTY_JSON_ARRAY,
                    ethAmount = Balance(BigInteger.TEN),
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    callerAddress = WalletAddress("b")
                )
            )
            contractFunctionCallRequestRepository.setTxInfo(id, txHash, callerAddress)
        }

        verify("400 is returned when attaching transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/function-call/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "0x2",
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.TX_INFO_ALREADY_SET)
        }

        verify("transaction info is not changed in database") {
            val storedRequest = contractFunctionCallRequestRepository.getById(id)

            expectThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    private fun insertProjectWithCustomRpcUrl(): Triple<ProjectId, ChainId, String> {
        val projectId = ProjectId(UUID.randomUUID())
        val chainId = ChainId(1337L)

        dslContext.executeInsert(
            ProjectRecord(
                id = projectId,
                ownerId = PROJECT.ownerId,
                baseRedirectUrl = PROJECT.baseRedirectUrl,
                chainId = chainId,
                customRpcUrl = "http://localhost:${hardhatContainer.mappedPort}",
                createdAt = PROJECT.createdAt
            )
        )

        val apiKey = "another-api-key"

        dslContext.executeInsert(
            ApiKeyRecord(
                id = ApiKeyId(UUID.randomUUID()),
                projectId = projectId,
                apiKey = apiKey,
                createdAt = TestData.TIMESTAMP
            )
        )

        return Triple(projectId, chainId, apiKey)
    }
}
