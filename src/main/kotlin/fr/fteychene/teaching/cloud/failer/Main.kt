package fr.fteychene.teaching.cloud.failer

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.http4k.core.*
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.PERMANENT_REDIRECT
import org.http4k.filter.CachingFilters
import org.http4k.filter.MicrometerMetrics
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson.auto
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.ResourceLoader.Companion.Directory
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess

val currentServerId: UUID = UUID.randomUUID()
val logger = LoggerFactory.getLogger("$currentServerId");

data class Health(
    val now: Instant,
    val serverId: UUID = currentServerId
)

data class CustomError(
    val message: String,
    val cause: Throwable? = null
)

fun Api(clock: Clock): HttpHandler {
    val healthLens = Body.auto<Health>().toLens()
    val errorLens = Body.auto<CustomError>().toLens()

    val metrics = PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
        config().commonTags("application", "cloud-failer")
        config().commonTags("app-id", currentServerId.toString())
        ClassLoaderMetrics().bindTo(this)
        JvmMemoryMetrics().bindTo(this)
        JvmGcMetrics().bindTo(this)
        ProcessorMetrics().bindTo(this)
        JvmThreadMetrics().bindTo(this)
        LogbackMetrics().bindTo(this)
    }

    return ServerFilters.MicrometerMetrics.RequestCounter(metrics)
        .then(ServerFilters.MicrometerMetrics.RequestTimer(metrics))
        .then(routes(
            "/fail" bind Method.POST to {
                GlobalScope.launch {
                    logger.warn("Received failure request. Shutting down in 3s")
                    delay(3000)
                    exitProcess(128)
                }
                Response(INTERNAL_SERVER_ERROR).with(errorLens of CustomError("Fail generated an error, server will shutdown in 3s"))
            },
            "/fail" bind Method.GET to {
                Response(BAD_REQUEST).with(errorLens of CustomError("This route should be called with a POST"))
            },
            "/" bind Method.GET to {
                Response(PERMANENT_REDIRECT).header("Location", "index.html")
            },
            "/" bind CachingFilters.Response.NoCache().then(static(Classpath("public"))),
            "/health" bind Method.GET to {
                Response(OK).with(healthLens of Health(Instant.now(clock)))
            },
            "/metrics" bind Method.GET to {
                Response(OK).body(metrics.scrape())
            }
        ))
}

fun main() {
    logger.info("Starting server with id $currentServerId")

    GlobalScope.launch {
        val nextCrash = Random.nextInt(30, 300)
        logger.warn("Application will crash in ${nextCrash}s")
        delay(nextCrash * 1000L)
        logger.warn("Shutting down")
        exitProcess(128)
    }
    Api(Clock.systemDefaultZone()).asServer(Netty(8080)).start()
}