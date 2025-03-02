package dev.nohus.rift.di

import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module
import org.koin.logger.SLF4JLogger

lateinit var koin: Koin

fun startKoin() {
    koin = startKoin {
        logger(SLF4JLogger())
        modules(
            KoinModule().module,
            factoryModule,
            platformModule,
        )
    }.koin
}
