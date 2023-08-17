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
import polycode.features.contract.deployment.model.response.ContractDeploymentRequestResponse
import polycode.features.contract.deployment.model.response.ContractDeploymentRequestsResponse
import polycode.features.contract.deployment.model.result.ContractConstructor
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.model.result.ContractEvent
import polycode.features.contract.deployment.model.result.ContractFunction
import polycode.features.contract.deployment.model.result.ContractParameter
import polycode.features.contract.deployment.model.result.EventParameter
import polycode.features.contract.deployment.repository.ContractDecoratorRepository
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
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
import polycode.util.ZeroAddress
import java.math.BigInteger
import java.util.UUID

class ContractDeploymentRequestControllerApiTest : ControllerTestBase() {

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
                        value = listOf(WalletAddress("a").rawValue),
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "indexedStruct",
                        type = EventArgumentResponseType.HASH,
                        value = null,
                        hash = "0xc65a7bb8d6351c1cf70c95a316cc6a92839c986682d98bc35f958f4883f9d2a8"
                    )
                )
            )
        )
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

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
    fun mustCorrectlyCreateContractDeploymentRequest() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val deployerAddress = WalletAddress("b")
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract deployment request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractDeploymentRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = response.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        status = Status.PENDING,
                        contractId = CONTRACT_DECORATOR.id.value,
                        contractDeploymentData = response.contractDeploymentData,
                        constructorParams = objectMapper.readTree(paramsJson),
                        contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                        contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                        initialEthAmount = initialEthAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = null,
                        deployerAddress = deployerAddress.rawValue,
                        deployTx = TransactionResponse(
                            txHash = null,
                            from = deployerAddress.rawValue,
                            to = ZeroAddress.rawValue,
                            data = response.deployTx.data,
                            value = initialEthAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null,
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract deployment request is correctly stored in database") {
            val storedRequest = contractDeploymentRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = response.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        contractId = CONTRACT_DECORATOR.id,
                        contractData = ContractBinaryData(response.contractDeploymentData),
                        constructorParams = objectMapper.readTree(paramsJson),
                        contractTags = CONTRACT_DECORATOR.tags,
                        contractImplements = CONTRACT_DECORATOR.implements,
                        initialEthAmount = initialEthAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-deploy/${response.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = null,
                        deployerAddress = deployerAddress,
                        txHash = null,
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateContractDeploymentRequestWithRedirectUrl() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val deployerAddress = WalletAddress("b")
        val initialEthAmount = Balance.ZERO
        val redirectUrl = "https://custom-url/\${id}"

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to create contract deployment request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "redirect_url": "$redirectUrl",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractDeploymentRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = response.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        status = Status.PENDING,
                        contractId = CONTRACT_DECORATOR.id.value,
                        contractDeploymentData = response.contractDeploymentData,
                        constructorParams = objectMapper.readTree(paramsJson),
                        contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                        contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                        initialEthAmount = initialEthAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        projectId = PROJECT_ID,
                        createdAt = response.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = null,
                        deployerAddress = deployerAddress.rawValue,
                        deployTx = TransactionResponse(
                            txHash = null,
                            from = deployerAddress.rawValue,
                            to = ZeroAddress.rawValue,
                            data = response.deployTx.data,
                            value = initialEthAmount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null,
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("contract deployment request is correctly stored in database") {
            val storedRequest = contractDeploymentRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    ContractDeploymentRequest(
                        id = response.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        contractId = CONTRACT_DECORATOR.id,
                        contractData = ContractBinaryData(response.contractDeploymentData),
                        constructorParams = objectMapper.readTree(paramsJson),
                        contractTags = CONTRACT_DECORATOR.tags,
                        contractImplements = CONTRACT_DECORATOR.implements,
                        initialEthAmount = initialEthAmount,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        projectId = PROJECT_ID,
                        createdAt = storedRequest!!.createdAt,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = null,
                        deployerAddress = deployerAddress,
                        txHash = null,
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenCreatingContractDeploymentRequestForNonExistentContractDecorator() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val deployerAddress = WalletAddress("b")
        val initialEthAmount = Balance.ZERO

        verify("404 is returned for non-existent contract decorator") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "non-existent-contract-id",
                                "constructor_params": [
                                    {
                                        "type": "address",
                                        "value": "${ownerAddress.rawValue}"
                                    }
                                ],
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
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
    fun mustReturn401UnauthorizedWhenCreatingContractDeploymentRequestWithInvalidApiKey() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val deployerAddress = WalletAddress("b")
        val initialEthAmount = Balance.ZERO

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": [
                                    {
                                        "type": "address",
                                        "value": "${ownerAddress.rawValue}"
                                    }
                                ],
                                "deployer_address": "${deployerAddress.rawValue}"
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
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
    fun mustCorrectlyDeleteContractDeploymentRequestById() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        verify("contract deployment request exists in the database") {
            expectThat(contractDeploymentRequestRepository.getById(createResponse.id))
                .isNotNull()
        }

        suppose("request to delete contract deployment request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/deploy/${createResponse.id.value}")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("contract deployment request does not exist in the database") {
            expectThat(contractDeploymentRequestRepository.getById(createResponse.id))
                .isNull()
        }
    }

    @Test
    fun mustReturn404NotFoundWhenDeletingNonOwnedContractDeploymentRequestById() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        verify("contract deployment request exists in the database") {
            expectThat(contractDeploymentRequestRepository.getById(createResponse.id))
                .isNotNull()
        }

        val (_, _, apiKey) = insertProjectWithCustomRpcUrl()

        verify("404 is returned for non-existent contract deployment request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/deploy/${createResponse.id.value}")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }

        verify("contract deployment request still exists in the database") {
            expectThat(contractDeploymentRequestRepository.getById(createResponse.id))
                .isNotNull()
        }
    }

    @Test
    fun mustReturn404NotFoundWhenDeletingNonExistentContractDeploymentRequestById() {
        verify("404 is returned for non-existent contract deployment request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/deploy/${UUID.randomUUID()}")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestById() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction info is attached to contract deployment request") {
            contractDeploymentRequestRepository.setTxInfo(createResponse.id, txHash, deployerAddress)
        }

        val fetchResponse = suppose("request to fetch contract deployment request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deploy/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = createResponse.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        status = Status.SUCCESS,
                        contractId = CONTRACT_DECORATOR.id.value,
                        contractDeploymentData = createResponse.contractDeploymentData,
                        constructorParams = objectMapper.readTree(paramsJson),
                        contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                        contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                        initialEthAmount = initialEthAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-deploy/${createResponse.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = createResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress).rawValue,
                        deployerAddress = deployerAddress.rawValue,
                        deployTx = TransactionResponse(
                            txHash = txHash.value,
                            from = deployerAddress.rawValue,
                            to = ZeroAddress.rawValue,
                            data = createResponse.deployTx.data,
                            value = initialEthAmount.rawValue,
                            blockConfirmations = fetchResponse.deployTx.blockConfirmations,
                            timestamp = fetchResponse.deployTx.timestamp
                        ),
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null,
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.deployTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.deployTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestByProjectIdAndAlias() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction info is attached to contract deployment request") {
            contractDeploymentRequestRepository.setTxInfo(createResponse.id, txHash, deployerAddress)
        }

        val fetchResponse = suppose("request to fetch contract deployment request is made") {
            val fetchResponse = mockMvc
                .perform(
                    MockMvcRequestBuilders.get(
                        "/v1/deploy/by-project/${createResponse.projectId.value}/by-alias/${createResponse.alias}"
                    )
                )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = createResponse.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        status = Status.SUCCESS,
                        contractId = CONTRACT_DECORATOR.id.value,
                        contractDeploymentData = createResponse.contractDeploymentData,
                        constructorParams = objectMapper.readTree(paramsJson),
                        contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                        contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                        initialEthAmount = initialEthAmount.rawValue,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-deploy/${createResponse.id.value}/action",
                        projectId = PROJECT_ID,
                        createdAt = createResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress).rawValue,
                        deployerAddress = deployerAddress.rawValue,
                        deployTx = TransactionResponse(
                            txHash = txHash.value,
                            from = deployerAddress.rawValue,
                            to = ZeroAddress.rawValue,
                            data = createResponse.deployTx.data,
                            value = initialEthAmount.rawValue,
                            blockConfirmations = fetchResponse.deployTx.blockConfirmations,
                            timestamp = fetchResponse.deployTx.timestamp
                        ),
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null,
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.deployTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.deployTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestByIdWhenCustomRpcUrlIsSpecified() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction info is attached to contract deployment request") {
            contractDeploymentRequestRepository.setTxInfo(createResponse.id, txHash, deployerAddress)
        }

        val fetchResponse = suppose("request to fetch contract deployment request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deploy/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = createResponse.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        status = Status.SUCCESS,
                        contractId = CONTRACT_DECORATOR.id.value,
                        contractDeploymentData = createResponse.contractDeploymentData,
                        constructorParams = objectMapper.readTree(paramsJson),
                        contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                        contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                        initialEthAmount = initialEthAmount.rawValue,
                        chainId = chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-deploy/${createResponse.id.value}/action",
                        projectId = projectId,
                        createdAt = createResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress).rawValue,
                        deployerAddress = deployerAddress.rawValue,
                        deployTx = TransactionResponse(
                            txHash = txHash.value,
                            from = deployerAddress.rawValue,
                            to = ZeroAddress.rawValue,
                            data = createResponse.deployTx.data,
                            value = initialEthAmount.rawValue,
                            blockConfirmations = fetchResponse.deployTx.blockConfirmations,
                            timestamp = fetchResponse.deployTx.timestamp
                        ),
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null,
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.deployTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.deployTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestByProjectIdAndAliasWhenCustomRpcUrlIsSpecified() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction info is attached to contract deployment request") {
            contractDeploymentRequestRepository.setTxInfo(createResponse.id, txHash, deployerAddress)
        }

        val fetchResponse = suppose("request to fetch contract deployment request is made") {
            val fetchResponse = mockMvc
                .perform(
                    MockMvcRequestBuilders.get(
                        "/v1/deploy/by-project/${createResponse.projectId.value}/by-alias/${createResponse.alias}"
                    )
                )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractDeploymentRequestResponse(
                        id = createResponse.id,
                        alias = alias,
                        name = CONTRACT_DECORATOR.name,
                        description = CONTRACT_DECORATOR.description,
                        status = Status.SUCCESS,
                        contractId = CONTRACT_DECORATOR.id.value,
                        contractDeploymentData = createResponse.contractDeploymentData,
                        constructorParams = objectMapper.readTree(paramsJson),
                        contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                        contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                        initialEthAmount = initialEthAmount.rawValue,
                        chainId = chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-deploy/${createResponse.id.value}/action",
                        projectId = projectId,
                        createdAt = createResponse.createdAt,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        contractAddress = ContractAddress(contract.contractAddress).rawValue,
                        deployerAddress = deployerAddress.rawValue,
                        deployTx = TransactionResponse(
                            txHash = txHash.value,
                            from = deployerAddress.rawValue,
                            to = ZeroAddress.rawValue,
                            data = createResponse.deployTx.data,
                            value = initialEthAmount.rawValue,
                            blockConfirmations = fetchResponse.deployTx.blockConfirmations,
                            timestamp = fetchResponse.deployTx.timestamp
                        ),
                        imported = false,
                        proxy = false,
                        implementationContractAddress = null,
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.deployTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.deployTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractDeploymentRequestById() {
        verify("404 is returned for non-existent contract deployment request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deploy/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractDeploymentRequestByProjectIdAndAlias() {
        verify("404 is returned for non-existent contract deployment request") {
            val response = mockMvc
                .perform(
                    MockMvcRequestBuilders.get(
                        "/v1/deploy/by-project/${UUID.randomUUID()}/by-alias/random-alias"
                    )
                )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestsByProjectIdAndFilters() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction info is attached to contract deployment request") {
            contractDeploymentRequestRepository.setTxInfo(createResponse.id, txHash, deployerAddress)
        }

        val fetchResponse = suppose("request to fetch contract deployment requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deploy/by-project/${createResponse.projectId.value}" +
                        "?contractIds=${CONTRACT_DECORATOR.id.value}" +
                        "&contractTags=example AND simple,other" +
                        "&contractImplements=traits/example AND traits/exampleOwnable,traits/other" +
                        "&deployedOnly=true"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractDeploymentRequestsResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractDeploymentRequestsResponse(
                        listOf(
                            ContractDeploymentRequestResponse(
                                id = createResponse.id,
                                alias = alias,
                                name = CONTRACT_DECORATOR.name,
                                description = CONTRACT_DECORATOR.description,
                                status = Status.SUCCESS,
                                contractId = CONTRACT_DECORATOR.id.value,
                                contractDeploymentData = createResponse.contractDeploymentData,
                                constructorParams = objectMapper.readTree(paramsJson),
                                contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                                contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                                initialEthAmount = initialEthAmount.rawValue,
                                chainId = PROJECT.chainId.value,
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-deploy/${createResponse.id.value}/action",
                                projectId = PROJECT_ID,
                                createdAt = createResponse.createdAt,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                contractAddress = ContractAddress(contract.contractAddress).rawValue,
                                deployerAddress = deployerAddress.rawValue,
                                deployTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = deployerAddress.rawValue,
                                    to = ZeroAddress.rawValue,
                                    data = createResponse.deployTx.data,
                                    value = initialEthAmount.rawValue,
                                    blockConfirmations = fetchResponse.requests[0].deployTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].deployTx.timestamp
                                ),
                                imported = false,
                                proxy = false,
                                implementationContractAddress = null,
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].deployTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].deployTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestsByProjectIdAndFiltersWhenCustomRpcUrlIsSpecified() {
        val alias = "alias"
        val ownerAddress = WalletAddress("a")
        val mainAccount = accounts[0]
        val deployerAddress = WalletAddress(mainAccount.address)
        val initialEthAmount = Balance.ZERO

        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val paramsJson =
            """
                [
                    {
                        "type": "address",
                        "value": "${ownerAddress.rawValue}"
                    }
                ]
            """.trimIndent()

        val createResponse = suppose("request to create contract deployment request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/deploy")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "contract_id": "${CONTRACT_DECORATOR.id.value}",
                                "constructor_params": $paramsJson,
                                "deployer_address": "${deployerAddress.rawValue}",
                                "initial_eth_amount": "${initialEthAmount.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                createResponse.response.contentAsString,
                ContractDeploymentRequestResponse::class.java
            )
        }

        val contract = suppose("contract is deployed") {
            ExampleContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                ownerAddress.rawValue
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction info is attached to contract deployment request") {
            contractDeploymentRequestRepository.setTxInfo(createResponse.id, txHash, deployerAddress)
        }

        val fetchResponse = suppose("request to fetch contract deployment requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deploy/by-project/${createResponse.projectId.value}" +
                        "?contractIds=${CONTRACT_DECORATOR.id.value}" +
                        "&contractTags=example AND simple,other" +
                        "&contractImplements=traits/example AND traits/exampleOwnable,traits/other" +
                        "&deployedOnly=true"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(
                fetchResponse.response.contentAsString,
                ContractDeploymentRequestsResponse::class.java
            )
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    ContractDeploymentRequestsResponse(
                        listOf(
                            ContractDeploymentRequestResponse(
                                id = createResponse.id,
                                alias = alias,
                                name = CONTRACT_DECORATOR.name,
                                description = CONTRACT_DECORATOR.description,
                                status = Status.SUCCESS,
                                contractId = CONTRACT_DECORATOR.id.value,
                                contractDeploymentData = createResponse.contractDeploymentData,
                                constructorParams = objectMapper.readTree(paramsJson),
                                contractTags = CONTRACT_DECORATOR.tags.map { it.value },
                                contractImplements = CONTRACT_DECORATOR.implements.map { it.value },
                                initialEthAmount = initialEthAmount.rawValue,
                                chainId = chainId.value,
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-deploy/${createResponse.id.value}/action",
                                projectId = projectId,
                                createdAt = createResponse.createdAt,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                contractAddress = ContractAddress(contract.contractAddress).rawValue,
                                deployerAddress = deployerAddress.rawValue,
                                deployTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = deployerAddress.rawValue,
                                    to = ZeroAddress.rawValue,
                                    data = createResponse.deployTx.data,
                                    value = initialEthAmount.rawValue,
                                    blockConfirmations = fetchResponse.requests[0].deployTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].deployTx.timestamp
                                ),
                                imported = false,
                                proxy = false,
                                implementationContractAddress = null,
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].deployTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].deployTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val id = ContractDeploymentRequestId(UUID.randomUUID())
        val alias = "alias"
        val deployerAddress = WalletAddress("b")

        suppose("some contract deployment request without transaction info exists in database") {
            contractDeploymentRequestRepository.store(
                params = StoreContractDeploymentRequestParams(
                    id = id,
                    alias = alias,
                    contractId = CONTRACT_DECORATOR.id,
                    contractData = CONTRACT_DECORATOR.binary,
                    constructorParams = TestData.EMPTY_JSON_ARRAY,
                    deployerAddress = WalletAddress("a"),
                    initialEthAmount = Balance(BigInteger.TEN),
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    imported = false,
                    proxy = false,
                    implementationContractAddress = null
                ),
                metadataProjectId = Constants.NIL_PROJECT_ID
            )
        }

        val txHash = TransactionHash("0x1")

        suppose("request to attach transaction info to contract deployment request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/deploy/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${txHash.value}",
                                "caller_address": "${deployerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("transaction info is correctly attached to contract deployment request") {
            val storedRequest = contractDeploymentRequestRepository.getById(id)

            expectThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionInfoIsNotAttached() {
        val id = ContractDeploymentRequestId(UUID.randomUUID())
        val alias = "alias"
        val txHash = TransactionHash("0x1")
        val deployerAddress = WalletAddress("b")

        suppose("some contract deployment request with transaction info exists in database") {
            contractDeploymentRequestRepository.store(
                params = StoreContractDeploymentRequestParams(
                    id = id,
                    alias = alias,
                    contractId = CONTRACT_DECORATOR.id,
                    contractData = CONTRACT_DECORATOR.binary,
                    constructorParams = TestData.EMPTY_JSON_ARRAY,
                    deployerAddress = WalletAddress("a"),
                    initialEthAmount = Balance(BigInteger.TEN),
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    projectId = PROJECT_ID,
                    createdAt = TestData.TIMESTAMP,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    imported = false,
                    proxy = false,
                    implementationContractAddress = null
                ),
                metadataProjectId = Constants.NIL_PROJECT_ID
            )
            contractDeploymentRequestRepository.setTxInfo(id, txHash, deployerAddress)
        }

        verify("400 is returned when attaching transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/deploy/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "0x2",
                                "caller_address": "${deployerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.TX_INFO_ALREADY_SET)
        }

        verify("transaction info is not changed in database") {
            val storedRequest = contractDeploymentRequestRepository.getById(id)

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
