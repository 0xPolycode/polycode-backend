package polycode.model.request

import org.jooq.JSON
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.abi.datatypes.generated.Uint256
import polycode.TestBase
import polycode.TestData
import polycode.config.validation.ValidationConstants
import polycode.config.validation.ValidationConstants.FUNCTION_ARGUMENT_MAX_JSON_CHARS
import polycode.features.contract.abi.model.BoolType
import polycode.features.contract.abi.model.UintType
import polycode.features.contract.readcall.model.params.OutputParameter
import polycode.features.contract.readcall.model.request.ReadonlyFunctionCallRequest
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.util.JsonNodeConverter
import polycode.util.WalletAddress
import java.math.BigInteger
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadonlyFunctionCallRequestTest : TestBase() {

    private lateinit var validatorFactory: ValidatorFactory
    private lateinit var validator: Validator

    @BeforeAll
    fun beforeAll() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.validator
    }

    @AfterAll
    fun afterAll() {
        validatorFactory.close()
    }

    @Test
    fun mustNotAllowTooLongStringForDeployedContractAlias() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                contractAddress = null,
                blockNumber = BigInteger.ZERO,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("deployedContractAlias")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                contractAddress = null,
                blockNumber = BigInteger.ZERO,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForContractAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = "invalid",
                blockNumber = BigInteger.ZERO,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("contractAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = WalletAddress("a").rawValue + "b",
                blockNumber = BigInteger.ZERO,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("contractAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = "",
                blockNumber = BigInteger.ZERO,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("contractAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = WalletAddress("a").rawValue,
                blockNumber = BigInteger.ZERO,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowNegativeOrTooBigValueForBlockNumber() {
        val requestWithNegativeUint256 = suppose("request with negative uint256 is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = BigInteger.valueOf(-1L),
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with negative uint256 is marked as invalid") {
            val violations = validator.validate(requestWithNegativeUint256).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be within range [0, 2^256]")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("blockNumber")
        }

        val requestWithTooBigUint256 = suppose("request with too big uint256 is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = BigInteger.TWO.pow(256) + BigInteger.ONE,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with too big uint256 is marked as invalid") {
            val violations = validator.validate(requestWithTooBigUint256).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be within range [0, 2^256]")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("blockNumber")
        }

        val requestWithValidUint256 = suppose("request with valid uint256 is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = BigInteger.TWO.pow(256),
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with valid uint256 is marked as valid") {
            val violations = validator.validate(requestWithValidUint256).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForFunctionName() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("functionName")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidFunctionParams() {
        val requestWithTooLongListOfArguments = suppose("request with too long list of arguments is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = MutableList(ValidationConstants.REQUEST_BODY_MAX_ARGS_LENGTH + 1) {
                    FunctionArgument(Uint256.DEFAULT)
                },
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with too long list of arguments is marked as invalid") {
            val violations = validator.validate(requestWithTooLongListOfArguments).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_ARGS_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("functionParams")
        }

        val tooLongValue = "a".repeat(FUNCTION_ARGUMENT_MAX_JSON_CHARS + 1)
        val requestWithTooLongArgumentJson = suppose("request with too long argument JSON is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = listOf(
                    FunctionArgument(
                        value = Uint256.DEFAULT,
                        rawJson = JsonNodeConverter().from(JSON.valueOf("{\"value\":\"$tooLongValue\"}"))
                    )
                ),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with too long argument JSON is marked as invalid") {
            val violations = validator.validate(requestWithTooLongArgumentJson).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid JSON of at most $FUNCTION_ARGUMENT_MAX_JSON_CHARS characters")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("functionParams[0].rawJson")
        }

        val requestWithValidArguments = suppose("request with valid arguments is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = listOf(
                    FunctionArgument(
                        value = Uint256.DEFAULT,
                        rawJson = TestData.EMPTY_JSON_OBJECT
                    )
                ),
                outputParams = emptyList(),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with valid arguments is marked as invalid") {
            val violations = validator.validate(requestWithValidArguments).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidOutputParams() {
        val requestWithTooLongListOfArguments = suppose("request with too long list of arguments is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = emptyList(),
                outputParams = MutableList(ValidationConstants.REQUEST_BODY_MAX_ARGS_LENGTH + 1) {
                    OutputParameter(UintType)
                },
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with too long list of arguments is marked as invalid") {
            val violations = validator.validate(requestWithTooLongListOfArguments).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_ARGS_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("outputParams")
        }

        val requestWithValidArguments = suppose("request with valid arguments is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = emptyList(),
                outputParams = listOf(OutputParameter(BoolType)),
                callerAddress = WalletAddress("0").rawValue
            )
        }

        verify("request with valid arguments is marked as invalid") {
            val violations = validator.validate(requestWithValidArguments).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForCallerAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = "invalid"
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("callerAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("a").rawValue + "b"
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("callerAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = ""
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("callerAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            ReadonlyFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                blockNumber = null,
                functionName = "",
                functionParams = emptyList(),
                outputParams = emptyList(),
                callerAddress = WalletAddress("a").rawValue
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            expectThat(violations)
                .isEmpty()
        }
    }
}
