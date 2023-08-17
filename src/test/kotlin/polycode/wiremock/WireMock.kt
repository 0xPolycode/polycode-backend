package polycode.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

object WireMock {

    val server = WireMockServer(WireMockConfiguration.wireMockConfig().port(8090))

    fun start() = server.start()

    fun stop() = server.stop()

    fun reset() = server.resetAll()
}
