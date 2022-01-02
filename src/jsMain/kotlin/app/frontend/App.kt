package app.frontend

import app.model.ActDto
import app.model.L
import app.model.ToDoResource
import app.model.ToDoValidator
import dev.fritz2.binding.RootStore
import dev.fritz2.binding.storeOf
import dev.fritz2.binding.watch
import dev.fritz2.components.clickButton
import dev.fritz2.components.modal
import dev.fritz2.components.toast
import dev.fritz2.dom.html.Keys
import dev.fritz2.dom.html.RenderContext
import dev.fritz2.dom.html.render
import dev.fritz2.dom.key
import dev.fritz2.dom.states
import dev.fritz2.dom.values
import dev.fritz2.repositories.rest.restQuery
import dev.fritz2.routing.router
import dev.fritz2.styling.p
import dev.fritz2.styling.theme.ColorScheme
import kotlinx.browser.window
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.js.Date

data class Filter(val text: String, val function: (List<ActDto>) -> List<ActDto>)

val filters = mapOf(
    "all" to Filter("All") { it },
    "active" to Filter("Active") { toDos -> toDos.filter { !it.completed } },
    "completed" to Filter("Completed") { toDos -> toDos.filter { it.completed } }
)

const val endpoint = "/api/todos"
val validator = ToDoValidator()
val router = router("all")

object ClockStore : RootStore<String>("clock time")


object ToDoListStore : RootStore<List<ActDto>>(emptyList(), id = "todos") {

    private val restRepo = restQuery<ActDto, Long, Unit>(ToDoResource, endpoint, -1)

    private val query = handle { restRepo.query(Unit) }

    val save = handle<ActDto> { toDos, new ->
        if (validator.isValid(new, Unit)) restRepo.addOrUpdate(toDos, new)
        else toDos
    }

    val remove = handle { toDos, id: Long ->
        restRepo.delete(toDos, id)
    }

    init {
        query()
    }
}

fun RenderContext.filter(text: String, route: String) {
    li {
        a {
            className(router.data.map { if (it == route) "selected" else "" })
            href("#$route")
            +text
        }
    }
}




@ExperimentalCoroutinesApi
fun RenderContext.inputHeader() {


    header {
        h2 { ClockStore.data.asText() }

        clickButton({
            margin { "32px" }
        }) {
            text("Add Activity")

        } handledBy modal {
            width { large }
            placement { stretch }
            content { close ->
                h1 { +"Add new activity" }
                clickButton {
                    text("Pee")
                    type {
                        ColorScheme("yellow", "grey", "#ffedcb", "black")
                    }
                } handledBy {
                    close.invoke()
                    ToDoListStore.save.invoke(ActDto(text = "Pee"))
                }
            }
        }
    }
}

@ExperimentalCoroutinesApi
fun RenderContext.mainSection() {
    section("main") {
        ul("todo-list") {
            ToDoListStore.data.combine(router.data) { all, route ->
                filters[route]?.function?.invoke(all) ?: all
            }.renderEach(ActDto::id) { toDo ->
                val toDoStore = storeOf(toDo)
                toDoStore.syncBy(ToDoListStore.save)
                val textStore = toDoStore.sub(L.ActDto.text)

                val editingStore = storeOf(false)

                li {
                    attr("data-id", toDoStore.id)
                    classMap(toDoStore.data.combine(editingStore.data) { toDo, isEditing ->
                        mapOf(
                            "completed" to toDo.completed,
                            "editing" to isEditing
                        )
                    })
                    div("view") {
                        label {
                            textStore.data.asText()

                            dblclicks.map { true } handledBy editingStore.update
                        }
                        button("destroy") {
                            clicks.events.map { toDo.id } handledBy ToDoListStore.remove
                        }
                    }
                }
            }
        }
    }
}


@ExperimentalCoroutinesApi
fun main() {
    window.setInterval({
        ClockStore.update(Date().toTimeString())
    }, 1_000)


    render("#todoapp") {
        inputHeader()
        mainSection()
    }
}