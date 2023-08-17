package polycode.testcontainers

object SharedTestContainers {
    val postgresContainer by lazy { PostgresTestContainer() }
    val hardhatContainer by lazy { HardhatTestContainer() }
    val manifestServiceContainer by lazy { ManifestServiceTestContainer() }
}
