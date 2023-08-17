package polycode.features.payout.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import polycode.TestBase
import polycode.config.ContractManifestServiceProperties
import polycode.config.IpfsProperties
import polycode.config.WebConfig
import polycode.exception.IpfsUploadFailedException
import polycode.features.api.access.repository.ApiKeyRepository
import polycode.features.api.access.repository.PolyflowUserRepository
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.api.access.repository.UserIdentifierRepository
import polycode.features.api.usage.repository.ApiRateLimitRepository
import polycode.features.api.usage.repository.UserIdResolverRepository
import polycode.features.payout.util.IpfsHash
import polycode.service.UtcDateTimeProvider
import polycode.service.UuidProvider
import polycode.wiremock.WireMock

@RestClientTest
@Import(PinataIpfsService::class, WebConfig::class)
@MockBeans(
    MockBean(UuidProvider::class),
    MockBean(UtcDateTimeProvider::class),
    MockBean(UserIdentifierRepository::class),
    MockBean(ApiKeyRepository::class),
    MockBean(ApiRateLimitRepository::class),
    MockBean(UserIdResolverRepository::class),
    MockBean(PolyflowUserRepository::class),
    MockBean(ProjectRepository::class)
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableConfigurationProperties(IpfsProperties::class, ContractManifestServiceProperties::class)
class PinataIpfsServiceIntegTest : TestBase() {

    @Autowired
    private lateinit var service: IpfsService

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun beforeEach() {
        WireMock.start()
    }

    @AfterEach
    fun afterEach() {
        WireMock.stop()
    }

    @Test
    fun mustCorrectlyUploadJsonToIpfs() {
        val requestJson = "{\"test\":1}"
        val ipfsHash = IpfsHash("test-hash")
        val responseJson =
            """
            {
                "IpfsHash": "${ipfsHash.value}",
                "PinSize": 1,
                "Timestamp": "2022-01-01T00:00:00Z"
            }
            """.trimIndent()

        suppose("IPFS JSON upload will succeed") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .withRequestBody(equalToJson(requestJson))
                    .willReturn(
                        aResponse()
                            .withBody(responseJson)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(200)
                    )
            )
        }

        verify("correct IPFS hash is returned for JSON upload") {
            val result = service.pinJsonToIpfs(objectMapper.readTree(requestJson))

            expectThat(result)
                .isEqualTo(ipfsHash)
        }
    }

    @Test
    fun mustThrowExceptionWhenIpfsHashIsMissingInResponse() {
        val requestJson = "{\"test\":1}"
        val responseJson =
            """
            {
                "PinSize": 1,
                "Timestamp": "2022-01-01T00:00:00Z"
            }
            """.trimIndent()

        suppose("IPFS JSON upload will succeed without IPFS hash in response") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .withRequestBody(equalToJson(requestJson))
                    .willReturn(
                        aResponse()
                            .withBody(responseJson)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(200)
                    )
            )
        }

        verify("exception is thrown when IPFS hash is missing in response") {
            expectThrows<IpfsUploadFailedException> {
                service.pinJsonToIpfs(objectMapper.readTree(requestJson))
            }
        }
    }

    @Test
    fun mustThrowExceptionForNon2xxResponseCode() {
        val requestJson = "{\"test\":1}"

        suppose("IPFS JSON upload will succeed without IPFS hash in response") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .withRequestBody(equalToJson(requestJson))
                    .willReturn(
                        aResponse()
                            .withBody("{}")
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(400)
                    )
            )
        }

        verify("exception is thrown when non 2xx response is returned") {
            expectThrows<IpfsUploadFailedException> {
                service.pinJsonToIpfs(objectMapper.readTree(requestJson))
            }
        }
    }
}
