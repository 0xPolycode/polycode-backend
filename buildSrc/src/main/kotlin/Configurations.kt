import java.math.BigDecimal

object Configurations {

    object Compile {
        val compilerArgs = listOf("-Xjsr305=strict")
    }

    object Database {
        val url = "jdbc:postgresql://localhost:5432/postgres"
        val user = "postgres"
        val password = "postgres"
        val schema = "polycode"
        val driverDependency = "org.postgresql:postgresql:${Versions.Dependencies.postgresDriver}"
        val driverClass = "org.postgresql.Driver"
    }

    object Docker {
        const val baseImage = "gcr.io/distroless/java17"
        const val tag = "latest"
        const val digest = "sha256:6508f83c5424778581bd174de54e8be95868849471c65753f5f1a82cbd49059e"
    }

    object Tests {
        val testSets = listOf("integTest", "apiTest")
        val minimumCoverage = BigDecimal("0.90")
    }

    object Jooq {
        const val packageDir = "polycode/generated/jooq"
        const val packageName = "polycode.generated.jooq"
    }
}
