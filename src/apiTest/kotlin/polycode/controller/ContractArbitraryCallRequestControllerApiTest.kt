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
import polycode.features.contract.arbitrarycall.model.params.StoreContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.response.ContractArbitraryCallRequestResponse
import polycode.features.contract.arbitrarycall.model.response.ContractArbitraryCallRequestsResponse
import polycode.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import polycode.features.contract.arbitrarycall.repository.ContractArbitraryCallRequestRepository
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
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
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
import polycode.testcontainers.SharedTestContainers
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.Constants
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.FunctionData
import polycode.util.InterfaceId
import polycode.util.Status
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

class ContractArbitraryCallRequestControllerApiTest : ControllerTestBase() {

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

    @Suppress("unused")
    protected val manifestServiceContainer = SharedTestContainers.manifestServiceContainer

    @Autowired
    private lateinit var contractArbitraryCallRequestRepository: ContractArbitraryCallRequestRepository

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
    fun mustCorrectlyCreateContractArbitraryCallRequestViaDeployedContractId() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")
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

        val response = suppose("request to create contract arbitrary call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "function_data": "${functionData.value}",
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

            objectMapper.readValue(response.response.contentAsString, ContractArbitraryCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractArbitraryCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = functionData.value,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-arbitrary-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        arbitraryCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = functionData.value,
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

        verify("contract arbitrary call request is correctly stored in database") {
            val storedRequest = contractArbitraryCallRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractArbitraryCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionData = functionData,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-arbitrary-call/${response.id.value}/action",
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
    fun mustCorrectlyCreateContractArbitraryCallRequestViaDeployedContractAlias() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")
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

        val response = suppose("request to create contract arbitrary call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "function_data": "${functionData.value}",
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

            objectMapper.readValue(response.response.contentAsString, ContractArbitraryCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractArbitraryCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = functionData.value,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-arbitrary-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        arbitraryCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = functionData.value,
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

        verify("contract arbitrary call request is correctly stored in database") {
            val storedRequest = contractArbitraryCallRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractArbitraryCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionData = functionData,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-arbitrary-call/${response.id.value}/action",
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
    fun mustCorrectlyCreateContractArbitraryCallRequestViaDeployedContractAddress() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")
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

        val response = suppose("request to create contract arbitrary call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_data": "${functionData.value}",
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

            objectMapper.readValue(response.response.contentAsString, ContractArbitraryCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractArbitraryCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = null,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = functionData.value,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-arbitrary-call/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        arbitraryCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = functionData.value,
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

        verify("contract arbitrary call request is correctly stored in database") {
            val storedRequest = contractArbitraryCallRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractArbitraryCallRequest(
                        id = response.id,
                        deployedContractId = null,
                        contractAddress = contractAddress,
                        functionData = functionData,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        ethAmount = ethAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-arbitrary-call/${response.id.value}/action",
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
    fun mustCorrectlyCreateContractArbitraryCallRequestWithRedirectUrl() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")
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

        val response = suppose("request to create contract arbitrary call request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "function_data": "${functionData.value}",
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

            objectMapper.readValue(response.response.contentAsString, ContractArbitraryCallRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractArbitraryCallRequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = functionData.value,
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
                        arbitraryCallTx = TransactionResponse(
                            txHash = null,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = functionData.value,
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

        verify("contract arbitrary call request is correctly stored in database") {
            val storedRequest = contractArbitraryCallRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractArbitraryCallRequest(
                        id = response.id,
                        deployedContractId = storedContract.id,
                        contractAddress = contractAddress,
                        functionData = functionData,
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
    fun mustReturn400BadRequestWhenCreatingContractArbitraryCallRequestWithAllContractIdentifiers() {
        val callerAddress = WalletAddress("b")
        val contractAddress = ContractAddress("cafebabe")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
            contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
        }

        verify("400 is returned when creating contract arbitrary call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "contract_address": "${contractAddress.rawValue}",
                                "function_data": "${functionData.value}",
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
    fun mustReturn400BadRequestWhenCreatingContractArbitraryCallRequestWithNoContractIdentifiers() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
        }

        verify("400 is returned when creating contract arbitrary call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "function_data": "${functionData.value}",
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
    fun mustReturn404NotFoundWhenCreatingContractArbitraryCallRequestForNonExistentContractId() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        verify("404 is returned when creating contract arbitrary call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${UUID.randomUUID()}",
                                "function_data": "${functionData.value}",
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
    fun mustReturn404NotFoundWhenCreatingContractArbitraryCallRequestForNonExistentContractAlias() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        verify("404 is returned when creating contract arbitrary call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "non-existent-alias",
                                "function_data": "${functionData.value}",
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
    fun mustReturn400BadRequestWhenCreatingContractArbitraryCallRequestViaDeployedContractIdForNonDeployedContract() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        suppose("some non-deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
        }

        verify("400 is returned when creating contract arbitrary call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "function_data": "${functionData.value}",
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
    fun mustReturn400BadRequestWhenCreatingContractArbitraryCallRequestViaDeployedContractAliasForNonDeployedContract() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        suppose("some non-deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
        }

        verify("400 is returned when creating contract arbitrary call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "function_data": "${functionData.value}",
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
    fun mustReturn401UnauthorizedWhenCreatingContractArbitraryCallRequestWithInvalidApiKey() {
        val callerAddress = WalletAddress("b")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        verify("401 is returned when creating contract arbitrary call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${UUID.randomUUID()}",
                                "function_data": "${functionData.value}",
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
    fun mustCorrectlyFetchContractArbitraryCallRequest() {
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
        val functionData = FunctionData("0x13af4035000000000000000000000000959fd7ef9089b7142b6b908dc3a8af7aa8ff0fa1")
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

        val createResponse = suppose("request to create contract arbitrary call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_data": "${functionData.value}",
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
                ContractArbitraryCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract arbitrary call request") {
            contractArbitraryCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract arbitrary call request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/arbitrary-call/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractArbitraryCallRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractArbitraryCallRequestResponse(
                        id = createResponse.id,
                        status = Status.SUCCESS,
                        deployedContractId = DEPLOYED_CONTRACT.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = functionData.value,
                        ethAmount = ethAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-arbitrary-call/${createResponse.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = fetchResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        arbitraryCallTx = TransactionResponse(
                            txHash = txHash.value,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = functionData.value,
                            value = ethAmount.rawValue,
                            blockConfirmations = fetchResponse.arbitraryCallTx.blockConfirmations,
                            timestamp = fetchResponse.arbitraryCallTx.timestamp
                        ),
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.arbitraryCallTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.arbitraryCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractArbitraryCallRequestWhenCustomRpcUrlIsSpecified() {
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
        val functionData = FunctionData("0x13af4035000000000000000000000000959fd7ef9089b7142b6b908dc3a8af7aa8ff0fa1")
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

        val createResponse = suppose("request to create contract arbitrary call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "function_data": "${functionData.value}",
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
                ContractArbitraryCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract arbitrary call request") {
            contractArbitraryCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract arbitrary call request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/arbitrary-call/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractArbitraryCallRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractArbitraryCallRequestResponse(
                        id = createResponse.id,
                        status = Status.SUCCESS,
                        deployedContractId = DEPLOYED_CONTRACT.id,
                        contractAddress = contractAddress.rawValue,
                        functionName = functionName,
                        functionParams = objectMapper.readTree(paramsJson),
                        functionCallData = functionData.value,
                        ethAmount = ethAmount.rawValue,
                        chainId = chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-arbitrary-call/${createResponse.id.value}/action",
                        projectId = projectId,
                        createdAt = fetchResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        callerAddress = callerAddress.rawValue,
                        arbitraryCallTx = TransactionResponse(
                            txHash = txHash.value,
                            from = callerAddress.rawValue,
                            to = contractAddress.rawValue,
                            data = functionData.value,
                            value = ethAmount.rawValue,
                            blockConfirmations = fetchResponse.arbitraryCallTx.blockConfirmations,
                            timestamp = fetchResponse.arbitraryCallTx.timestamp
                        ),
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.arbitraryCallTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.arbitraryCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractArbitraryCallRequest() {
        verify("404 is returned for non-existent contract arbitrary call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/arbitrary-call/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractArbitraryCallRequestsByProjectIdAndFilters() {
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
        val functionData = FunctionData("0x13af4035000000000000000000000000959fd7ef9089b7142b6b908dc3a8af7aa8ff0fa1")
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

        val createResponse = suppose("request to create contract arbitrary call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${storedContract.id.value}",
                                "function_data": "${functionData.value}",
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
                ContractArbitraryCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract arbitrary call request") {
            contractArbitraryCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract arbitrary call requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/arbitrary-call/by-project/${createResponse.projectId.value}" +
                        "?deployedContractId=${storedContract.id.value}&contractAddress=${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractArbitraryCallRequestsResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractArbitraryCallRequestsResponse(
                        listOf(
                            ContractArbitraryCallRequestResponse(
                                id = createResponse.id,
                                status = Status.SUCCESS,
                                deployedContractId = storedContract.id,
                                contractAddress = contractAddress.rawValue,
                                functionName = functionName,
                                functionParams = objectMapper.readTree(paramsJson),
                                functionCallData = functionData.value,
                                ethAmount = ethAmount.rawValue,
                                chainId = PROJECT.chainId.value,
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-arbitrary-call/${createResponse.id.value}/action",
                                projectId = PROJECT_ID,
                                createdAt = fetchResponse.requests[0].createdAt,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                callerAddress = callerAddress.rawValue,
                                arbitraryCallTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = callerAddress.rawValue,
                                    to = contractAddress.rawValue,
                                    data = functionData.value,
                                    value = ethAmount.rawValue,
                                    blockConfirmations = fetchResponse.requests[0].arbitraryCallTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].arbitraryCallTx.timestamp
                                ),
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].arbitraryCallTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].arbitraryCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractArbitraryCallRequestsByProjectIdAndFiltersWhenCustomRpcUrlIsSpecified() {
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
        val functionData = FunctionData("0x13af4035000000000000000000000000959fd7ef9089b7142b6b908dc3a8af7aa8ff0fa1")
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

        val createResponse = suppose("request to create contract arbitrary call request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/arbitrary-call")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${storedContract.id.value}",
                                "function_data": "${functionData.value}",
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
                ContractArbitraryCallRequestResponse::class.java
            )
        }

        val txHash = suppose("function is called") {
            contract.setOwner(callerAddress.rawValue).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to contract arbitrary call request") {
            contractArbitraryCallRequestRepository.setTxInfo(createResponse.id, txHash, callerAddress)
        }

        val fetchResponse = suppose("request to fetch contract arbitrary call requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/arbitrary-call/by-project/${createResponse.projectId.value}" +
                        "?deployedContractId=${storedContract.id.value}&contractAddress=${contractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractArbitraryCallRequestsResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractArbitraryCallRequestsResponse(
                        listOf(
                            ContractArbitraryCallRequestResponse(
                                id = createResponse.id,
                                status = Status.SUCCESS,
                                deployedContractId = storedContract.id,
                                contractAddress = contractAddress.rawValue,
                                functionName = functionName,
                                functionParams = objectMapper.readTree(paramsJson),
                                functionCallData = functionData.value,
                                ethAmount = ethAmount.rawValue,
                                chainId = chainId.value,
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-arbitrary-call/${createResponse.id.value}/action",
                                projectId = projectId,
                                createdAt = fetchResponse.requests[0].createdAt,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                callerAddress = callerAddress.rawValue,
                                arbitraryCallTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = callerAddress.rawValue,
                                    to = contractAddress.rawValue,
                                    data = functionData.value,
                                    value = ethAmount.rawValue,
                                    blockConfirmations = fetchResponse.requests[0].arbitraryCallTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].arbitraryCallTx.timestamp
                                ),
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].arbitraryCallTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].arbitraryCallTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
        val callerAddress = WalletAddress("c")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        suppose("some contract arbitrary call request without transaction info exists in database") {
            contractArbitraryCallRequestRepository.store(
                StoreContractArbitraryCallRequestParams(
                    id = id,
                    deployedContractId = null,
                    contractAddress = ContractAddress("a"),
                    functionData = functionData,
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
                MockMvcRequestBuilders.put("/v1/arbitrary-call/${id.value}")
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
            val storedRequest = contractArbitraryCallRequestRepository.getById(id)

            expectThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionInfoIsNotAttached() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
        val txHash = TransactionHash("0x1")
        val callerAddress = WalletAddress("c")
        val functionData = FunctionData("0x13af4035000000000000000000000000000000000000000000000000000000000000000b")

        suppose("some contract arbitrary call request with transaction info exists in database") {
            contractArbitraryCallRequestRepository.store(
                StoreContractArbitraryCallRequestParams(
                    id = id,
                    deployedContractId = null,
                    contractAddress = ContractAddress("a"),
                    functionData = functionData,
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
            contractArbitraryCallRequestRepository.setTxInfo(id, txHash, callerAddress)
        }

        verify("400 is returned when attaching transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/arbitrary-call/${id.value}")
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
            val storedRequest = contractArbitraryCallRequestRepository.getById(id)

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
