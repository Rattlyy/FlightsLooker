package it.rattly.plugins

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import gg.jte.watcher.DirectoryWatcher
import io.ktor.server.application.*
import io.ktor.server.jte.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.delay
import java.nio.file.Path

fun Application.configureJte() {
    val resolver = DirectoryCodeResolver(Path.of("src/main/jte"))
    val templateEngine =
        if (developmentMode) TemplateEngine.create(
            resolver,
            Path.of("jte-classes"),
            ContentType.Html,
            Thread.currentThread().contextClassLoader
        )

        else TemplateEngine.createPrecompiled(ContentType.Html)

    install(Jte) {
        this.templateEngine = templateEngine
    }

    if (developmentMode) {
        val watcher = DirectoryWatcher(templateEngine, resolver)
        val flags = mutableMapOf<Int, Boolean>()

        // starts the watcher that sets the shouldReload flag to true if the template file changes
        watcher.start {
            flags.keys.forEach { flags[it] = true }
        }

        routing {
            // sends the hmr signal to the client, so that it can reload the webpage
            sse("/hmr") {
                // we use a different key for each request to avoid race conditions
                val key = Math.random().toInt()
                flags[key] = false

                while (!flags[key]!!) {
                    delay(100)
                }

                send("hmr", "message")
                flags.remove(key)
            }
        }
    }
}