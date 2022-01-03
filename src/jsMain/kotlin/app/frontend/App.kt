package app.frontend

import app.model.ActDto
import app.model.ActType
import app.model.ToDoResource
import app.model.ToDoValidator
import dev.fritz2.binding.RootStore
import dev.fritz2.binding.SimpleHandler
import dev.fritz2.binding.storeOf
import dev.fritz2.components.clickButton
import dev.fritz2.components.modal
import dev.fritz2.dom.html.RenderContext
import dev.fritz2.dom.html.render
import dev.fritz2.repositories.rest.restQuery
import dev.fritz2.routing.router
import dev.fritz2.styling.style
import dev.fritz2.styling.theme.ColorScheme
import dev.fritz2.styling.theme.Theme
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


const val endpoint = "/api/todos"
val validator = ToDoValidator()
val router = router("all")

object ClockStore : RootStore<String>("clock")


object ActListStore : RootStore<List<ActDto>>(emptyList(), id = "acts") {

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
fun RenderContext.createActButton(type: ActType, close: SimpleHandler<Unit>) {
    clickButton({
        margin { "16px" }
        fontSize { "2rem" }
        padding { "2rem" }
    }) {
        text(type.description)
        type {
            type.toColorScheme()
        }
    } handledBy {
        close()
        ActListStore.save.invoke(ActDto(type = type))
    }
}

@ExperimentalCoroutinesApi
fun ActType.toColorScheme(): ColorScheme = when (this) {
    ActType.PEE -> ColorScheme("yellow", "black", "#cae4ea", "#2d3748")
    ActType.POO -> ColorScheme("saddlebrown", "white", "#cae4ea", "#2d3748")
    ActType.WATER -> ColorScheme("skyblue", "black", "#cae4ea", "#2d3748")
    ActType.FOOD -> ColorScheme("darkgreen", "white", "#cae4ea", "#2d3748")

    ActType.ACCIDENT_PEE -> Theme().button.types.danger
    ActType.ACCIDENT_POO -> Theme().button.types.danger

    ActType.ACCIDENT_VOMIT -> ColorScheme("yellowgreen", "black", "#cae4ea", "#2d3748")
    else -> Theme().button.types.primary
}


@ExperimentalCoroutinesApi
fun RenderContext.inputHeader() {

    val headerStyle = style {
        position { absolute { right { "0" } } }
    }

    header(headerStyle.name) {
//        h2 { ClockStore.data.asText() }


        clickButton({
            margin { "32px" }
        }) {
            text("Add Activity")

        } handledBy modal {
            closeButtonStyle {
                fontSize { "2rem" }
            }
            width { large }
            placement { stretch }
            content { close ->
                h1 { +"Add new activity" }
                ActType.values().forEach { createActButton(it, close) }
            }
        }
    }
}

fun toHourMin(time: Instant): String {
    val local = time.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val min = local.minute.toString().padStart(2, '0')
    return "${hour}:${min}"
}


@ExperimentalTime
@ExperimentalCoroutinesApi
fun RenderContext.mainSection() {
    section("cal") {
        ActListStore.data.render { dtos ->
            println(dtos)
            table {

                var time = Clock.System.now()
                val local = time.toLocalDateTime(TimeZone.currentSystemDefault())
                time = time.minus(Duration.seconds(local.second))
                time = time.minus(Duration.minutes(local.minute % 10))

                val trStyle = style {
                    fontSize { "1.5rem" }
                    border {
                        width { thin }
                    }
                }
                val timeStyle = style { fontWeight { bold } }
                tr(trStyle.plus(timeStyle).name) {
                    td { ClockStore.data.asText() }
                }

                for (i in 1..100) {
                    tr(trStyle.name) {

                        //td { +i.toString() }
                        td { +toHourMin(time) }
                        td {
                            for (dto in dtos) {
                                val nowLimit = Clock.System.now().minus(Duration.minutes(10))
                                var top = time.plus(Duration.minutes(5))
                                if (nowLimit.toEpochMilliseconds() < time.toEpochMilliseconds()) {
                                    top = Clock.System.now()
                                }
                                val bottom = time.minus(Duration.minutes(5))
                                if (dto.time.toEpochMilliseconds() <= top.toEpochMilliseconds() &&
                                    bottom.toEpochMilliseconds() < dto.time.toEpochMilliseconds()
                                ) {
                                    clickButton({
                                        margin { "8px" }
                                        padding { "8px" }
                                        lineHeight { "1rem" }
                                        minWidth { "64px" }
                                    }) {
                                        text(dto.type.description)
                                        type {
                                            dto.type.toColorScheme()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    time = time.minus(Duration.minutes(10))
                }
            }


        }

    }



    section("main") {
        ul("todo-list") {

            ActListStore.data.renderEach(ActDto::id) { act ->

                val actStore = storeOf(act)
                actStore.syncBy(ActListStore.save)

                li {
                    attr("data-id", actStore.id)
                    div("view") {
                        label {
                            actStore.data
                                .map {
                                    it.time.toLocalDateTime(TimeZone.currentSystemDefault()).toString() +
                                            " " +
                                            it.type.description
                                }.asText()

                            clicks.events.map { act.id } handledBy ActListStore.remove
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalTime
@ExperimentalCoroutinesApi
fun main() {
    window.setInterval({
        ClockStore.update(toHourMin(Clock.System.now()))
    }, 1_000)
    window.setInterval({
        document.location?.reload()
    }, 1_000 * 60 * 2)


    render("#todoapp") {
        inputHeader()
        mainSection()
    }
}