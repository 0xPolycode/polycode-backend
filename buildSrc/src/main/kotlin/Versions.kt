import org.gradle.api.JavaVersion

object Versions {

    const val project = "3.1.0"

    object Compile {
        const val kotlin = "1.7.20"
        val sourceCompatibility = JavaVersion.VERSION_17
        val targetCompatibility = JavaVersion.VERSION_17
        val jvmTarget = targetCompatibility.name.removePrefix("VERSION_").replace('_', '.')
    }

    object Plugins {
        const val ktlint = "11.0.0"
        const val detekt = "1.21.0"
        const val testSets = "4.0.0"
        const val springBoot = "2.7.4"
        const val springDependencyManagement = "1.1.0"
        const val flyway = "9.5.0"
        const val jooq = "7.1.1"
        const val jib = "3.3.0"
        const val asciiDoctor = "3.3.2"
    }

    object Tools {
        const val ktlint = "0.45.2"
        const val jacoco = "0.8.8"
        const val solidity = "0.8.0"
    }

    object Dependencies {
        const val jjwt = "0.11.5"
        const val web3j = "4.9.4"
        const val kethereum = "0.85.7"
        const val okHttp = "4.10.0"
        const val kotlinCoroutines = "1.6.4"
        const val kotlinLogging = "3.0.2"
        const val mockitoKotlin = "4.0.0"
        const val assertk = "0.25"
        const val wireMock = "2.27.2"
        const val jsonSchemaGenerator = "4.27.0"
        const val testContainers = "1.17.5"
        const val postgresDriver = "42.5.0"
        const val sentry = "6.5.0"
        const val facebookSdk = "14.0.0"
    }
}
