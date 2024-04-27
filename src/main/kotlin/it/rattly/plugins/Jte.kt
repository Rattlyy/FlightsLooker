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
    val templateEngine = TemplateEngine.create(resolver, ContentType.Html)
    val watcher = DirectoryWatcher(templateEngine, resolver)
    var shouldReload = false

    watcher.start {
        shouldReload = true
    }

    install(Jte) {
        this.templateEngine = templateEngine
    }

    routing {
        sse("/hmr") {
            while (!shouldReload) {
                delay(100)
            }

            send("hmr", "message")
            shouldReload = false
        }
    }
}