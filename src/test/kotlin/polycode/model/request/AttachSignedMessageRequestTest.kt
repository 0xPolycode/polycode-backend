package polycode.model.request

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polycode.TestBase
import polycode.config.validation.ValidationConstants
import polycode.util.WalletAddress
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachSignedMessageRequestTest : TestBase() {

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
    fun mustNotAllowInvalidEthAddressForWalletAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            AttachSignedMessageRequest(
                walletAddress = "invalid",
                signedMessage = "message"
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("walletAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            AttachSignedMessageRequest(
                walletAddress = WalletAddress("a").rawValue + "b",
                signedMessage = "message"
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("walletAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            AttachSignedMessageRequest(
                walletAddress = "",
                signedMessage = "message"
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("walletAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            AttachSignedMessageRequest(
                walletAddress = WalletAddress("a").rawValue,
                signedMessage = "message"
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForSignedMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            AttachSignedMessageRequest(
                walletAddress = WalletAddress("a").rawValue,
                signedMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1)
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("signedMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            AttachSignedMessageRequest(
                walletAddress = WalletAddress("a").rawValue,
                signedMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }
}
