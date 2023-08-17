package polycode.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class ManifestServiceTestContainer : GenericContainer<ManifestServiceTestContainer>(
    "polyflowdev/contracts-manifest-service:0.8.2"
) {

    @Suppress("unused")
    companion object {
        private const val SERVICE_PORT = 42070
    }

    init {
        waitStrategy = LogMessageWaitStrategy()
            .withRegEx("Example app listening at .*")
            .withTimes(1)
            .withStartupTimeout(60.seconds.toJavaDuration())

        addExposedPort(SERVICE_PORT)
        start()

        val mappedPort = getMappedPort(SERVICE_PORT).toString()

        System.setProperty("MANIFEST_SERVICE_PORT", mappedPort)
    }
}
