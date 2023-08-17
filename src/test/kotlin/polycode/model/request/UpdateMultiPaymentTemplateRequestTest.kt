package polycode.model.request

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polycode.TestBase
import polycode.config.validation.ValidationConstants
import polycode.features.asset.multisend.model.request.UpdateMultiPaymentTemplateRequest
import polycode.util.AssetType
import polycode.util.WalletAddress
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateMultiPaymentTemplateRequestTest : TestBase() {

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
    fun mustNotAllowTooLongStringForTemplateName() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            UpdateMultiPaymentTemplateRequest(
                templateName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                assetType = AssetType.NATIVE,
                tokenAddress = null,
                chainId = 1L
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("templateName")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            UpdateMultiPaymentTemplateRequest(
                templateName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                assetType = AssetType.NATIVE,
                tokenAddress = null,
                chainId = 1L
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            expectThat(violations)
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForTokenAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            UpdateMultiPaymentTemplateRequest(
                templateName = "",
                tokenAddress = "invalid",
                assetType = AssetType.TOKEN,
                chainId = 1L
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("tokenAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            UpdateMultiPaymentTemplateRequest(
                templateName = "",
                tokenAddress = WalletAddress("a").rawValue + "b",
                assetType = AssetType.TOKEN,
                chainId = 1L
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("tokenAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            UpdateMultiPaymentTemplateRequest(
                templateName = "",
                tokenAddress = "",
                assetType = AssetType.TOKEN,
                chainId = 1L
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            expectThat(violations.size)
                .isOne()
            expectThat(violations[0].message)
                .isEqualTo("value must be a valid Ethereum address")
            expectThat(violations[0].propertyPath.toString())
                .isEqualTo("tokenAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            UpdateMultiPaymentTemplateRequest(
                templateName = "",
                tokenAddress = WalletAddress("a").rawValue,
                assetType = AssetType.TOKEN,
                chainId = 1L
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            expectThat(violations)
                .isEmpty()
        }
    }
}
