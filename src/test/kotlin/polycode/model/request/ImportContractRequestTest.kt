package polycode.model.request

import org.jooq.JSON
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polycode.TestBase
import polycode.TestData
import polycode.config.validation.ValidationConstants
import polycode.config.validation.ValidationConstants.REQUEST_BODY_MAX_JSON_CHARS
import polycode.features.contract.importing.model.request.ImportContractRequest
import polycode.model.ScreenConfig
import polycode.util.ContractAddress
import polycode.util.JsonNodeConverter
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImportContractRequestTest : TestBase() {

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
    fun mustNotAllowInvalidAlias() {
        val requestWithTooLongAlias = suppose("request with too long alias is created") {
            ImportContractRequest(
                alias = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too long alias is marked as invalid") {
            val violations = validator.validate(requestWithTooLongAlias).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo(
                    "value must be between 3 and 256 characters long and contain only" +
                        " letters, digits and characters '-', '_', '.', '/'",
                )
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("alias")
        }

        val requestWithTooShortAlias = suppose("request with too short alias is created") {
            ImportContractRequest(
                alias = "a",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too short alias is marked as invalid") {
            val violations = validator.validate(requestWithTooShortAlias).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo(
                    "value must be between 3 and 256 characters long and contain only" +
                        " letters, digits and characters '-', '_', '.', '/'",
                )
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("alias")
        }

        val requestWithInvalidAlias = suppose("request with invalid alias is created") {
            ImportContractRequest(
                alias = "a&%?",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with invalid alias is marked as invalid") {
            val violations = validator.validate(requestWithInvalidAlias).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo(
                    "value must be between 3 and 256 characters long and contain only" +
                        " letters, digits and characters '-', '_', '.', '/'",
                )
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("alias")
        }

        val requestWithValidAlias = suppose("request with valid alias is created") {
            ImportContractRequest(
                alias = "a-b_3.1/",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid alias is marked as valid") {
            val violations = validator.validate(requestWithValidAlias).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForContractId() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("contractId")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
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
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = "invalid",
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
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
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("a").rawValue + "b",
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
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
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = "",
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
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
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("a").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForRedirectUrl() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("redirectUrl")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                arbitraryData = null,
                screenConfig = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongJsonForArbitraryData() {
        val tooLongValue = "a".repeat(REQUEST_BODY_MAX_JSON_CHARS + 1)
        val requestWithTooLongJson = suppose("request with too long JSON is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = JsonNodeConverter().from(JSON.valueOf("{\"value\":\"$tooLongValue\"}")),
                screenConfig = null
            )
        }

        verify("request with too long JSON is marked as invalid") {
            val violations = validator.validate(requestWithTooLongJson).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid JSON of at most $REQUEST_BODY_MAX_JSON_CHARS characters")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("arbitraryData")
        }

        val requestWithValidLengthJson = suppose("request with valid length JSON is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = null
            )
        }

        verify("request with valid JSON string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthJson).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForScreenConfigBeforeActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                    afterActionMessage = null
                )
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("screenConfig.beforeActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                    afterActionMessage = null
                )
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForScreenConfigAfterActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = null,
                    afterActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1)
                )
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("screenConfig.afterActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            ImportContractRequest(
                alias = "a-b_3.1",
                contractId = "",
                contractAddress = ContractAddress("0").rawValue,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = null,
                    afterActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
                )
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }
}
