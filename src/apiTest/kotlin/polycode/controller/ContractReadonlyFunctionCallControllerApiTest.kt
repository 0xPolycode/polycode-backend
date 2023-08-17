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
import polycode.blockchain.ReadonlyFunctionCallsContract
import polycode.config.CustomHeaders
import polycode.exception.ErrorCode
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.params.StoreContractDeploymentRequestParams
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.readcall.model.response.ReadonlyFunctionCallResponse
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
import polycode.testcontainers.HardhatTestContainer
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.Constants
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

class ContractReadonlyFunctionCallControllerApiTest : ControllerTestBase() {

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
        private val CONTRACT_DECORATOR_ID = ContractId("decorator.id")
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
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

    @Autowired
    private lateinit var contractDeploymentRequestRepository: ContractDeploymentRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            ContractMetadataRecord(
                id = ContractMetadataId(UUID.randomUUID()),
                contractId = CONTRACT_DECORATOR_ID,
                contractTags = emptyArray(),
                contractImplements = emptyArray(),
                name = null,
                description = null,
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
    fun mustCorrectlyCallReadonlyContractFunctionViaDeployedContractId() {
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ReadonlyFunctionCallsContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val blockNumber = hardhatContainer.blockNumber()

        val value = BigInteger.TEN
        val callerAddress = WalletAddress("b")
        val functionName = "returningUint"

        val contractAddress = ContractAddress(contract.contractAddress)

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "uint256",
                        "value": "$value"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to call readonly contract function is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "block_number": "${blockNumber.value}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "output_params": ["uint256"],
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ReadonlyFunctionCallResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ReadonlyFunctionCallResponse(
                        deployedContractId = DEPLOYED_CONTRACT.id,
                        contractAddress = contractAddress.rawValue,
                        blockNumber = blockNumber.value,
                        timestamp = response.timestamp,
                        outputParams = response.outputParams,
                        returnValues = listOf(value.toString()),
                        rawReturnValue = response.rawReturnValue
                    )
                )

            expectThat(response.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCallReadonlyContractFunctionViaDeployedContractAlias() {
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ReadonlyFunctionCallsContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val blockNumber = hardhatContainer.blockNumber()

        val value = BigInteger.TEN
        val callerAddress = WalletAddress("b")
        val functionName = "returningUint"

        val contractAddress = ContractAddress(contract.contractAddress)

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID).apply {
                contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
            }
        }

        val paramsJson =
            """
                [
                    {
                        "type": "uint256",
                        "value": "$value"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to call readonly contract function is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "block_number": "${blockNumber.value}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "output_params": ["uint256"],
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ReadonlyFunctionCallResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ReadonlyFunctionCallResponse(
                        deployedContractId = DEPLOYED_CONTRACT.id,
                        contractAddress = contractAddress.rawValue,
                        blockNumber = blockNumber.value,
                        timestamp = response.timestamp,
                        outputParams = response.outputParams,
                        returnValues = listOf(value.toString()),
                        rawReturnValue = response.rawReturnValue
                    )
                )

            expectThat(response.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCallReadonlyContractFunctionViaDeployedContractAddress() {
        val mainAccount = accounts[0]

        val contract = suppose("contract is deployed") {
            ReadonlyFunctionCallsContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val blockNumber = hardhatContainer.blockNumber()

        val value = BigInteger.TEN
        val callerAddress = WalletAddress("b")
        val functionName = "returningUint"
        val contractAddress = ContractAddress(contract.contractAddress)

        val paramsJson =
            """
                [
                    {
                        "type": "uint256",
                        "value": "$value"
                    }
                ]
            """.trimIndent()

        val response = suppose("request to call readonly contract function is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "contract_address": "${contractAddress.rawValue}",
                                "block_number": "${blockNumber.value}",
                                "function_name": "$functionName",
                                "function_params": $paramsJson,
                                "output_params": ["uint256"],
                                "caller_address": "${callerAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ReadonlyFunctionCallResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ReadonlyFunctionCallResponse(
                        deployedContractId = null,
                        contractAddress = contractAddress.rawValue,
                        blockNumber = blockNumber.value,
                        timestamp = response.timestamp,
                        outputParams = response.outputParams,
                        returnValues = listOf(value.toString()),
                        rawReturnValue = response.rawReturnValue
                    )
                )

            expectThat(response.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenCallingReadonlyContractFunctionWithAllContractIdentifiers() {
        val callerAddress = WalletAddress("b")
        val contractAddress = ContractAddress("cafebabe")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
            contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
        }

        verify("400 is returned when calling readonly contract function request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "contract_address": "${contractAddress.rawValue}",
                                "block_number": "1",
                                "function_name": "example",
                                "function_params": [],
                                "output_params": [],
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
    fun mustReturn400BadRequestWhenCallingReadonlyContractFunctionWithNoContractIdentifiers() {
        val callerAddress = WalletAddress("b")
        val contractAddress = ContractAddress("cafebabe")

        suppose("some deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
            contractDeploymentRequestRepository.setContractAddress(DEPLOYED_CONTRACT.id, contractAddress)
        }

        verify("400 is returned when calling readonly contract function request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "block_number": "1",
                                "function_name": "example",
                                "function_params": [],
                                "output_params": [],
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
    fun mustReturn404NotFoundWhenCallingReadonlyContractFunctionForNonExistentContractId() {
        val callerAddress = WalletAddress("b")

        verify("404 is returned when calling readonly contract function request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${UUID.randomUUID()}",
                                "block_number": "1",
                                "function_name": "example",
                                "function_params": [],
                                "output_params": [],
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
    fun mustReturn404NotFoundWhenCallingReadonlyContractFunctionForNonExistentContractAlias() {
        val callerAddress = WalletAddress("b")

        verify("404 is returned when calling readonly contract function request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "non-existent-alias",
                                "block_number": "1",
                                "function_name": "example",
                                "function_params": [],
                                "output_params": [],
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
    fun mustReturn400BadRequestWhenCallingReadonlyContractFunctionViaDeployedContractIdForNonDeployedContract() {
        val callerAddress = WalletAddress("b")

        suppose("some non-deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
        }

        verify("400 is returned when calling readonly contract function request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${DEPLOYED_CONTRACT.id.value}",
                                "block_number": "1",
                                "function_name": "example",
                                "function_params": [],
                                "output_params": [],
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
    fun mustReturn400BadRequestWhenCallingReadonlyContractFunctionViaDeployedContractAliasForNonDeployedContract() {
        val callerAddress = WalletAddress("b")

        suppose("some non-deployed contract exists in the database") {
            contractDeploymentRequestRepository.store(DEPLOYED_CONTRACT, Constants.NIL_PROJECT_ID)
        }

        verify("400 is returned when calling readonly contract function request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_alias": "${DEPLOYED_CONTRACT.alias}",
                                "block_number": "1",
                                "function_name": "example",
                                "function_params": [],
                                "output_params": [],
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
    fun mustReturn401UnauthorizedWhenCallingReadonlyContractFunctionWithInvalidApiKey() {
        val callerAddress = WalletAddress("b")

        verify("401 is returned when creating contract function call request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/readonly-function-call")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "deployed_contract_id": "${UUID.randomUUID()}",
                                "block_number": "1",
                                "function_name": "example",
                                "function_params": [],
                                "output_params": [],
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
}
