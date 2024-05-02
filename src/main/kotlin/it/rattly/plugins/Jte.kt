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
        if (developmentMode) TemplateEngine.create(resolver, ContentType.Html)
        else TemplateEngine.createPrecompiled(ContentType.Html)

    install(Jte) {
        this.templateEngine = templateEngine
    }

    if (developmentMode) {
        val watcher = DirectoryWatcher(templateEngine, resolver)
        var shouldReload = false

        // starts the watcher that sets the shouldReload flag to true if the template file changes
        watcher.start {
            shouldReload = true
        }

        routing {
            // sends the hmr signal to the client, so that it can reload the webpage
            sse("/hmr") {
                while (!shouldReload) {
                    delay(100)
                }

                send("hmr", "message")
                shouldReload = false
            }
        }
    }
}