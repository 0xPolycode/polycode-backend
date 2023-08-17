package polycode.config

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.boot.devtools.filewatch.FileSystemWatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import polycode.features.contract.deployment.repository.ContractDecoratorRepository
import polycode.features.contract.deployment.repository.ContractMetadataRepository
import polycode.features.contract.interfaces.repository.ContractInterfacesRepository
import polycode.service.UuidProvider

@Configuration
class ContractDecoratorFileWatcherConfig {

    companion object : KLogging()

    @Bean
    @Suppress("LongParameterList")
    fun setUpContractDecoratorFileWatcher(
        uuidProvider: UuidProvider,
        contractDecoratorRepository: ContractDecoratorRepository,
        contractInterfacesRepository: ContractInterfacesRepository,
        contractMetadataRepository: ContractMetadataRepository,
        objectMapper: ObjectMapper,
        contractDecoratorProperties: ContractDecoratorProperties
    ): FileSystemWatcher? {
        val interfacesDir = contractDecoratorProperties.interfacesDirectory

        if (interfacesDir == null) {
            logger.warn { "Contract interfaces directory not set, no contract interfaces will be loaded" }
        }

        logger.info { "Watching for contract interface changes in $interfacesDir" }

        val contractsDir = contractDecoratorProperties.contractsDirectory

        if (contractsDir == null) {
            logger.warn { "Contract decorator contracts directory not set, no contract decorators will be loaded" }
            return null
        }

        logger.info { "Watching for contract decorator changes in $contractsDir" }

        val listener = ContractDecoratorFileChangeListener(
            uuidProvider = uuidProvider,
            contractDecoratorRepository = contractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = contractMetadataRepository,
            objectMapper = objectMapper,
            contractsDir = contractsDir,
            interfacesDir = interfacesDir,
            ignoredDirs = contractDecoratorProperties.ignoredDirs
        )

        return FileSystemWatcher(
            true,
            contractDecoratorProperties.fillChangePollInterval,
            contractDecoratorProperties.fileChangeQuietInterval
        ).apply {
            addSourceDirectories(listOfNotNull(interfacesDir?.toFile(), contractsDir.toFile()))
            addListener(listener)
            start()
        }
    }
}
