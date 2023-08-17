package polycode.bugfixes

import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.config.JsonConfig
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.service.EthereumFunctionEncoderService

class FunctionArgumentEncodingBugfixes : TestBase() {

    private val objectMapper = JsonConfig().objectMapper()

    @Test
    fun bugfix1() {
        val json =
            """{
              |  "type": "tuple[]",
              |  "value": [
              |    [
              |      {
              |        "type": "bytes32",
              |        "value": [
              |          "107", "76",  "252", "241", "214",
              |          "241", "26",  "201", "186", "38",
              |          "152", "78",  "56",  "0",   "8",
              |          "101", "118", "9",   "105", "90",
              |          "224", "241", "181", "102", "135",
              |          "216", "149", "117", "253", "160",
              |          "199", "73"
              |        ]
              |      },
              |      {
              |        "type": "address",
              |        "value": "0xe9787e557efb20d70ead824bc91d7d21121756f0"
              |      },
              |      { "type": "uint256", "value": "100000000000000000000" },
              |      { "type": "uint256", "value": "0" },
              |      { "type": "uint256", "value": "1663583544786" }
              |    ]
              |  ]
              |}
            """.trimMargin()

        val deserializedArgument = suppose("type hierarchy will be deserialized") {
            objectMapper.readValue(json, FunctionArgument::class.java)
        }

        val encoder = EthereumFunctionEncoderService()

        verify("bug #1 must be fixed") {
            val encodedValue = encoder.encode("addRewards", listOf(deserializedArgument))

            expectThat(encodedValue.value)
                .isEqualTo(
                    "0x8b5373f9" +
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000001" +
                        "6b4cfcf1d6f11ac9ba26984e380008657609695ae0f1b56687d89575fda0c749" +
                        "000000000000000000000000e9787e557efb20d70ead824bc91d7d21121756f0" +
                        "0000000000000000000000000000000000000000000000056bc75e2d63100000" +
                        "0000000000000000000000000000000000000000000000000000000000000000" +
                        "00000000000000000000000000000000000000000000000000000183554e65d2"
                )
        }
    }
}
