package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import polycode.ControllerTestBase
import polycode.features.wallet.login.model.params.StoreWalletLoginRequestParams
import polycode.features.wallet.login.model.response.JwtTokenResponse
import polycode.features.wallet.login.model.response.WalletLoginRequestResponse
import polycode.features.wallet.login.model.result.WalletLoginRequest
import polycode.features.wallet.login.repository.WalletLoginRequestRepository
import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.util.SignedMessage
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.time.OffsetDateTime
import java.util.UUID

class WalletLoginRequestControllerApiTest : ControllerTestBase() {

    @Autowired
    private lateinit var walletLoginRequestRepository: WalletLoginRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyCreateWalletLoginRequest() {
        val walletAddress = WalletAddress("a")

        val response = suppose("request to create wallet login request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/wallet-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${walletAddress.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, WalletLoginRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    WalletLoginRequestResponse(
                        id = response.id,
                        walletAddress = walletAddress.rawValue,
                        messageToSign = "Sign this message to confirm that you are the owner of the wallet:" +
                            " ${response.walletAddress}\nID to sign: ${response.id.value}," +
                            " timestamp: ${UtcDateTime(response.createdAt).iso}",
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("wallet login request is correctly stored in database") {
            val storedRequest = walletLoginRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    WalletLoginRequest(
                        id = response.id,
                        walletAddress = walletAddress,
                        messageToSign = response.messageToSign,
                        signedMessage = null,
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyVerifyWalletLoginRequest() {
        val id = WalletLoginRequestId(UUID.randomUUID())
        val walletAddress = WalletAddress("0x865f603f42ca1231e5b5f90e15663b0fe19f0b21")

        suppose("some wallet login request without signed message exists in database") {
            walletLoginRequestRepository.store(
                StoreWalletLoginRequestParams(
                    id = id,
                    walletAddress = walletAddress,
                    messageToSign = "Authorization message ID to sign: 7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73",
                    createdAt = UtcDateTime(OffsetDateTime.now())
                )
            )
        }

        val signedMessage = SignedMessage(
            "0xcf6025b67228271aaed15b17d42d6258e3b47ad5dcd27088a81b2f36b9d7ff5d2d133a652297bc19bea94d10750076fbdc529a" +
                "b03540e2ea6be02903645a98531b"
        )

        val response = suppose("request to attach signed message to authorization request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/wallet-login/verify-message/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "signed_message": "${signedMessage.value}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, JwtTokenResponse::class.java)
        }

        verify("JWT token is correctly returned") {
            expectThat(response.token)
                .isNotBlank()

            val storedRequest = walletLoginRequestRepository.getById(id)

            expectThat(storedRequest?.signedMessage)
                .isEqualTo(signedMessage)
        }
    }
}
