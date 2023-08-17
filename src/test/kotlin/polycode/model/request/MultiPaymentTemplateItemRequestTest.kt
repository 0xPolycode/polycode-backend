package polycode.model.request

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polycode.TestBase
import polycode.config.validation.ValidationConstants
import polycode.features.asset.multisend.model.request.MultiPaymentTemplateItemRequest
import polycode.util.WalletAddress
import java.math.BigInteger
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiPaymentTemplateItemRequestTest : TestBase() {

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
            MultiPaymentTemplateItemRequest(
                walletAddress = "invalid",
                itemName = null,
                amount = BigInteger.ZERO
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
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("a").rawValue + "b",
                itemName = null,
                amount = BigInteger.ZERO
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
            MultiPaymentTemplateItemRequest(
                walletAddress = "",
                itemName = null,
                amount = BigInteger.ZERO
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
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("a").rawValue,
                itemName = null,
                amount = BigInteger.ZERO
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForItemName() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                amount = BigInteger.ZERO
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("itemName")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                amount = BigInteger.ZERO
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowNegativeOrTooBigValueForAmount() {
        val requestWithNegativeUint256 = suppose("request with negative uint256 is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = null,
                amount = BigInteger.valueOf(-1L)
            )
        }

        verify("request with negative uint256 is marked as invalid") {
            val violations = validator.validate(requestWithNegativeUint256).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be within range [0, 2^256]")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("amount")
        }

        val requestWithTooBigUint256 = suppose("request with too big uint256 is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = null,
                amount = BigInteger.TWO.pow(256) + BigInteger.ONE
            )
        }

        verify("request with too big uint256 is marked as invalid") {
            val violations = validator.validate(requestWithTooBigUint256).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be within range [0, 2^256]")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("amount")
        }

        val requestWithValidUint256 = suppose("request with valid uint256 is created") {
            MultiPaymentTemplateItemRequest(
                walletAddress = WalletAddress("0").rawValue,
                itemName = null,
                amount = BigInteger.TWO.pow(256)
            )
        }

        verify("request with valid uint256 is marked as valid") {
            val violations = validator.validate(requestWithValidUint256).toList()

            expectThat(violations)
                .isEmpty()
        }
    }
}
