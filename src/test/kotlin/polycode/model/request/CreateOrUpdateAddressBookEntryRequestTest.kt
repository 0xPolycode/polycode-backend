package polycode.model.request

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polycode.TestBase
import polycode.config.validation.ValidationConstants
import polycode.features.wallet.addressbook.model.request.CreateOrUpdateAddressBookEntryRequest
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateOrUpdateAddressBookEntryRequestTest : TestBase() {

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
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                address = "0",
                phoneNumber = null,
                email = null
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
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a",
                address = "0",
                phoneNumber = null,
                email = null
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
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a&%?",
                address = "0",
                phoneNumber = null,
                email = null
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
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a-b_3.1/",
                address = "0",
                phoneNumber = null,
                email = null
            )
        }

        verify("request with valid alias is marked as valid") {
            val violations = validator.validate(requestWithValidAlias).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidAddress() {
        val requestWithInvalidAddress = suppose("request with invalid address is created") {
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a-b_3.1",
                address = "invalid",
                phoneNumber = null,
                email = null
            )
        }

        verify("request with invalid address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("address")
        }

        val requestWithValidLengthString = suppose("request with valid address is created") {
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a-b_3.1",
                address = "a",
                phoneNumber = null,
                email = null
            )
        }

        verify("request with valid address is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForPhoneNumber() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a-b_3.1",
                address = "0",
                phoneNumber = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                email = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("phoneNumber")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a-b_3.1",
                address = "0",
                phoneNumber = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                email = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForEmail() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a-b_3.1",
                address = "0",
                phoneNumber = null,
                email = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1)
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("email")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateOrUpdateAddressBookEntryRequest(
                alias = "a-b_3.1",
                address = "0",
                phoneNumber = null,
                email = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }
}
