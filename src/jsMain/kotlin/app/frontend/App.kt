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
import dev.fritz2.styling.*
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
import kotlin.js.Date
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


const val endpoint = "/api/todos"
val validator = ToDoValidator()
val router = router("all")

@ExperimentalTime
val period = Duration.minutes(10)

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


@ExperimentalCoroutinesApi
fun RenderContext.createActButton(type: ActType, close: SimpleHandler<Unit>) {
    clickButton({
        margin { "1rem" }
        fontSize { "2rem" }
        padding { "2rem" }
        minWidth { "16rem" }
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
    ActType.PEE -> ColorScheme("yellow", "black", "#ffffcc", "black")
    ActType.POO -> ColorScheme("#663300", "white", "#ffcc99", "black")
    ActType.WATER -> ColorScheme("skyblue", "black", "#cae4ea", "#2d3748")
    ActType.FOOD -> ColorScheme("#e6005c", "white", "#ffb3d1", "black")

    ActType.EXERCISE -> ColorScheme("#006600", "white", "#ccffcc", "black")

    ActType.ACCIDENT_PEE -> Theme().button.types.danger
    ActType.ACCIDENT_POO -> Theme().button.types.danger

    ActType.ACCIDENT_VOMIT -> ColorScheme("yellowgreen", "black", "#cae4ea", "#2d3748")
    //else -> Theme().button.types.primary
}


@ExperimentalCoroutinesApi
fun RenderContext.inputHeader() {

    header({ position { absolute { right { "0" } } } }) {

        clickButton({
            margin { large }
            fontSize { larger }
        }) {
            text("Add Activity 新的")

        } handledBy modal {
            closeButtonStyle {
                fontSize { larger }
            }
            width { large }
            placement { stretch }
            content { close ->
                h1({ margin { normal } }) { +"Add new activity 新的" }
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
    section {
        ActListStore.data.render { dtos ->
            table({ margins { left { smaller } } }) {

                var time = Clock.System.now()
                val local = time.toLocalDateTime(TimeZone.currentSystemDefault())
                time = time.minus(Duration.seconds(local.second))
                time = time.minus(Duration.minutes(local.minute % period.inWholeMinutes))

                val trStyle = style {
                    fontSize { "1.5rem" }
                    borders {
                        top {
                            width { thin }
                        }
                        bottom { width { thin } }
                    }
                }
                tr(trStyle.plus(style { fontWeight { bold } }).name) {
                    td { ClockStore.data.asText() }
                }

                for (i in 1..100) {
                    tr(trStyle.name) {
                        val hourMin = toHourMin(time)
                        if (hourMin.endsWith(":00")) {
                            td({ fontWeight { bold } }) { +hourMin }
                        } else {
                            td { +hourMin }
                        }
                        td {
                            for (dto in dtos) {
                                val nowLimit = Clock.System.now().minus(period)
                                val top =
                                    if (nowLimit.toEpochMilliseconds() < time.toEpochMilliseconds()) {
                                        Clock.System.now()
                                    } else {
                                        time.plus(period.div(2))
                                    }
                                val bottom = time.minus(period.div(2))
                                if (dto.time.toEpochMilliseconds() <= top.toEpochMilliseconds() &&
                                    bottom.toEpochMilliseconds() < dto.time.toEpochMilliseconds()
                                ) {
                                    clickButton({
                                        margin { "8px" }
                                        padding { "8px" }
                                        lineHeight { "1rem" }
                                        minWidth { "128px" }
                                        height { unset }
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
                    time = time.minus(period)
                }
            }


        }

    }



    section({ display { none } }) {
        ul {

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
        val min = Date().getMinutes() % period.inWholeMinutes.toInt()
        if (min == 0 || min == 1) {
            document.location?.reload()
        }
    }, 1_000 * 60)

    render("#app") {
        inputHeader()
        mainSection()
    }
}
