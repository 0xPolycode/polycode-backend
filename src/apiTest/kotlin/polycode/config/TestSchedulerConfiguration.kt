package polycode.config

import mu.KLogging
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import polycode.features.payout.service.AssetSnapshotQueueServiceImpl
import polycode.features.payout.service.ManualFixedScheduler
import polycode.service.ScheduledExecutorServiceProvider

@TestConfiguration
class TestSchedulerConfiguration {

    companion object : KLogging()

    @Bean
    fun snapshotQueueScheduler() = ManualFixedScheduler()

    @Bean
    @Primary
    fun scheduledExecutorServiceProvider(
        snapshotQueueScheduler: ManualFixedScheduler
    ): ScheduledExecutorServiceProvider {
        logger.info { "Using manual schedulers for tests" }

        return mock {
            given(it.newSingleThreadScheduledExecutor(AssetSnapshotQueueServiceImpl.QUEUE_NAME))
                .willReturn(snapshotQueueScheduler)
        }
    }
}
