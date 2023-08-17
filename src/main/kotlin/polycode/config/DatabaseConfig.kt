package polycode.config

import org.jooq.ConnectionProvider
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import org.jooq.impl.DefaultExecuteListenerProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.jooq.JooqExceptionTranslator
import org.springframework.boot.autoconfigure.jooq.JooqProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource
import org.jooq.Configuration as JooqConfiguration

@Configuration
class DatabaseConfig {

    // Polycode

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.polycode")
    fun polycodeDataSourceProperties(): DataSourceProperties =
        DataSourceProperties()

    @Bean
    @Primary
    fun polycodeDataSource(@Qualifier("polycodeDataSourceProperties") properties: DataSourceProperties): DataSource =
        properties.initializeDataSourceBuilder()
            .build()

    @Bean
    @Primary
    fun polycodeConnectionProvider(@Qualifier("polycodeDataSource") dataSource: DataSource): ConnectionProvider =
        DataSourceConnectionProvider(dataSource)

    @Bean
    @Primary
    fun polycodeJooqConfiguration(
        @Qualifier("polycodeConnectionProvider") connectionProvider: ConnectionProvider,
        @Qualifier("polycodeDataSource") dataSource: DataSource
    ): JooqConfiguration {
        val configuration = DefaultConfiguration()

        configuration.set(JooqProperties().determineSqlDialect(dataSource))
        configuration.set(DefaultExecuteListenerProvider(JooqExceptionTranslator()))
        configuration.set(connectionProvider)

        return configuration
    }

    @Bean
    @Primary
    fun polycodeDslContext(@Qualifier("polycodeJooqConfiguration") configuration: JooqConfiguration) =
        DefaultDSLContext(configuration)

    // Polyflow

    @Bean
    @ConfigurationProperties("spring.datasource.polyflow")
    fun polyflowDataSourceProperties(): DataSourceProperties =
        DataSourceProperties()

    @Bean
    fun polyflowDataSource(@Qualifier("polyflowDataSourceProperties") properties: DataSourceProperties): DataSource =
        properties.initializeDataSourceBuilder()
            .build()

    @Bean
    fun polyflowConnectionProvider(@Qualifier("polyflowDataSource") dataSource: DataSource): ConnectionProvider =
        DataSourceConnectionProvider(dataSource)

    @Bean
    fun polyflowJooqConfiguration(
        @Qualifier("polyflowConnectionProvider") connectionProvider: ConnectionProvider,
        @Qualifier("polyflowDataSource") dataSource: DataSource
    ): JooqConfiguration {
        val configuration = DefaultConfiguration()

        configuration.set(JooqProperties().determineSqlDialect(dataSource))
        configuration.set(DefaultExecuteListenerProvider(JooqExceptionTranslator()))
        configuration.set(connectionProvider)

        return configuration
    }

    @Bean
    fun polyflowDslContext(@Qualifier("polyflowJooqConfiguration") configuration: JooqConfiguration) =
        DefaultDSLContext(configuration)
}
