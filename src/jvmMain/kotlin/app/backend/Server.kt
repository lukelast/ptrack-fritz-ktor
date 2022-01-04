package app.backend

import app.database.Db
import app.database.ActTable
import app.database.database
import app.model.ActDto
import app.model.ToDoValidator
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import java.io.File

val validator = ToDoValidator()

fun Application.main() {
    val currentDir = File(".").absoluteFile
    environment.log.info("Current directory: $currentDir")

    install(ContentNegotiation) {
        json()
    }

    Database.connect("jdbc:h2:./ptdb", "org.h2.Driver")

    database {
        SchemaUtils.create(ActTable)
    }

    routing {
        get("/") {
            call.resolveResource("index.html")?.let {
                call.respond(it)
            }
        }

        static("/") {
            resources("/")
        }

        route("/api") {

            get("/todos") {
                //environment.log.info("getting all ToDos")
                call.respond(Db.all())
            }

            post("/todos") {
                val actDto = call.receive<ActDto>()
                if (validator.isValid(actDto, Unit)) {
                    environment.log.info("save new ToDo: $actDto")
                    call.respond(HttpStatusCode.Created, Db.add(actDto))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "data is not valid"))
                }
            }

            put("/todos/{id}") {
                val oldToDo = call.parameters["id"]?.toLongOrNull()?.let { Db.find(it) }
                val newActDto = call.receive<ActDto>()
                if (oldToDo == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                } else if (!validator.isValid(newActDto, Unit)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "data is not valid"))
                } else {
                    environment.log.info("update ToDo[id=${oldToDo.id.value}] to: $newActDto")
                    call.respond(HttpStatusCode.Created, Db.update(oldToDo, newActDto.copy(id = oldToDo.id.value)))
                }
            }

            delete("/todos/{id}") {
                val oldToDo = call.parameters["id"]?.toLongOrNull()?.let { Db.find(it) }
                if (oldToDo == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                } else {
                    environment.log.info("remove ToDo with id: ${oldToDo.id.value}")
                    call.respond(HttpStatusCode.OK, Db.remove(oldToDo))
                }
            }
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8181
    embeddedServer(Netty, port = port) {
        main()
    }.start(wait = true)
}