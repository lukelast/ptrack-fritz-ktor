package app.frontend

import app.model.ActDto
import app.model.ActResource
import app.model.ActType
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
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime


const val endpoint = "/api/todos"
val validator = ToDoValidator()
val router = router("all")

@ExperimentalTime
val period = Duration.minutes(10)

object ClockStore : RootStore<String>("clock")


object ActListStore : RootStore<List<ActDto>>(emptyList(), id = "acts") {

    private val restRepo = restQuery<ActDto, Long, Unit>(ActResource, endpoint, -1)

    val query = handle { restRepo.query(Unit) }

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
        margin { large }
        fontSize { huge }
        padding { huge }
        minWidth { "18rem" }
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

@ExperimentalTime
@ExperimentalCoroutinesApi
fun RenderContext.rightSide() {

    header({
        position { absolute { right { "0" } } }
        textAlign { right }
        margin { large }
    }) {

        clickButton({
            //margins { bottom { normal } }
            margin { huge }
            fontSize { giant }
            width { "5rem" }
            height { "5rem" }
        }) {
            text("+")
        } handledBy modal {
            closeButtonStyle {
                fontSize { giant }
            }
            width { "85vw" }
            placement { center }
            content { close ->
                h1({ margin { normal } }) { +"Add new activity 新的" }
                ActType.values().forEach { createActButton(it, close) }
            }
        }

        ActListStore.data.render { acts ->
            ActType.values()
                .filter { it != ActType.ACCIDENT_VOMIT }
                .mapNotNull { type -> acts.firstOrNull { it.type == type } }
                .forEach { act ->
                    p({
                        fontSize { larger }
                        margins { top { huge } }
                        borders {
                            bottom {
                                width { fat }
                                color { act.type.toColorScheme().main }
                            }
                        }
                    })
                    {
                        val time = Clock.System.now()
                        val since = time.minus(act.time)
                        +"${act.type.description}: "
                        b { +since.toString(DurationUnit.HOURS, 1) }
                    }
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
            table({
                width { "60vw" }
                margins { left { smaller } }
            }) {

                var time = Clock.System.now()
                val local = time.toLocalDateTime(TimeZone.currentSystemDefault())
                time = time.minus(Duration.seconds(local.second))
                time = time.minus(Duration.minutes(local.minute % period.inWholeMinutes))

                val trStyle = style {
                    fontSize { large }
                    borders {
                        bottom { width { hair } }
                    }
                    position { relative { } }
                }
                tr(trStyle.plus(style { fontWeight { bold } }).name) {
                    td({
                        width { "56px" }
                    }) { ClockStore.data.asText() }
                }

                for (i in 1..100) {
                    tr(trStyle.name) {
                        val hourMin = toHourMin(time)
                        if (hourMin.endsWith(":00")) {
                            td({ fontWeight { bold } }) { +hourMin }
                        } else {
                            td { +hourMin }
                        }
                        td({
                            display { flex }
                            height { "100%" }
                            position { absolute { } }
                            alignItems { center }
                        }) {
                            for (dto in dtos) {
                                val nowLimit = Clock.System.now().minus(period)
                                val top =
                                    if (nowLimit.toEpochMilliseconds() < time.toEpochMilliseconds()) {
                                        Clock.System.now()
                                    } else {
                                        time.plus(period.div(2))
                                    }
                                val bottomTime = time.minus(period.div(2))
                                if (dto.time.toEpochMilliseconds() <= top.toEpochMilliseconds() &&
                                    bottomTime.toEpochMilliseconds() < dto.time.toEpochMilliseconds()
                                ) {
                                    div({
                                        minWidth { "128px" }
                                        textAlign { center }
                                        color { dto.type.toColorScheme().mainContrast }
                                        background { color { dto.type.toColorScheme().main } }
                                        fontSize { "16px" }
                                        radius { "12px" }
                                        display { inlineBlock }
                                        margins { left { "16px" } }
                                    }) {
                                        +dto.type.description
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
    }, 5_000)

    window.setInterval({
        ActListStore.query()
    }, 10_000)

    window.setInterval({
        val min = Date().getMinutes() % period.inWholeMinutes.toInt()
        if (min == 0 || min == 1) {
            document.location?.reload()
        }
    }, 1_000 * 60)

    render("#app") {
        rightSide()
        mainSection()
    }
}
