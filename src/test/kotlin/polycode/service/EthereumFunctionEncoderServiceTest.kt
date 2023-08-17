package polycode.service

import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.service.EthereumFunctionEncoderService
import polycode.util.Balance
import polycode.util.FunctionData
import polycode.util.WalletAddress
import java.math.BigInteger

class EthereumFunctionEncoderServiceTest : TestBase() {

    @Test
    fun mustCorrectlyEncodeFunctionCall() {
        val service = EthereumFunctionEncoderService()
        val toAddress = WalletAddress("0x495d96FaaaCEe16Dd3ca62cAB20a0F9548CdddB4")
        val amount = Balance(BigInteger("1000"))

        val encodedData = suppose("some test data will be encoded") {
            service.encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(toAddress),
                    FunctionArgument(amount),
                )
            )
        }

        val expectedData = "0xa9059cbb000000000000000000000000495d96faaacee16dd3ca62cab20a0f9548cdddb4000000000000000" +
            "00000000000000000000000000000000000000000000003e8"

        verify("data is correctly encoded") {
            expectThat(encodedData)
                .isEqualTo(FunctionData(expectedData))
        }
    }

    @Test
    fun mustCorrectlyEncodeConstructorCall() {
        val service = EthereumFunctionEncoderService()
        val toAddress = WalletAddress("0x495d96FaaaCEe16Dd3ca62cAB20a0F9548CdddB4")
        val amount = Balance(BigInteger("1000"))

        val encodedData = suppose("some test data will be encoded") {
            service.encodeConstructor(
                arguments = listOf(
                    FunctionArgument(toAddress),
                    FunctionArgument(amount),
                )
            )
        }

        val expectedData = "000000000000000000000000495d96faaacee16dd3ca62cab20a0f9548cdddb40000000000000000000000000" +
            "0000000000000000000000000000000000003e8"

        verify("data is correctly encoded") {
            expectThat(encodedData)
                .isEqualTo(FunctionData(expectedData))
        }
    }

    @Test
    fun mustCorrectlyEncodeEmptyConstructorCall() {
        val service = EthereumFunctionEncoderService()

        val encodedData = suppose("some empty constructor will be encoded") {
            service.encodeConstructor(arguments = listOf())
        }

        verify("data is correctly encoded") {
            expectThat(encodedData)
                .isEqualTo(FunctionData(""))
        }
    }
}
