package app.model

import dev.fritz2.identification.Inspector
import dev.fritz2.lenses.IdProvider
import dev.fritz2.lenses.Lenses
import dev.fritz2.resource.Resource
import dev.fritz2.validation.ValidationMessage
import dev.fritz2.validation.Validator
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Lenses
@Serializable
data class ActDto(
    val id: Long = -1,
    val time: Instant = Clock.System.now(),
    val type: ActType,
    val text: String = type.description,
)

enum class ActType(
    val id: Short,
    val description: String,
) {
    PEE(1, "Pee"),
    POO(2, "Poop"),

    FOOD(10, "Food"),
    WATER(11, "Water"),

    EXERCISE(20, "Exercise"),

    ACCIDENT_PEE(100, "Accident (Pee)"),
    ACCIDENT_POO(101, "Accident (Poop)"),
    ACCIDENT_VOMIT(102, "Accident (Vomit)"),
}

fun idToActType(id: Short): ActType = ActType.values().single { it.id == id }

data class ToDoMessage(val id: String, val text: String) : ValidationMessage {
    override fun isError(): Boolean = true
}

class ToDoValidator : Validator<ActDto, ToDoMessage, Unit>() {

    val maxTextLength = 50


    //override fun validate(data: ActDto, metadata: Unit): List<ToDoMessage> {
    override fun validate(inspector: Inspector<ActDto>, metadata: Unit): List<ToDoMessage> {
        val msgs = mutableListOf<ToDoMessage>()
        val textInspector = inspector.sub(L.ActDto.text)

        if (textInspector.data.trim().length < 3) msgs.add(
            ToDoMessage(
                textInspector.data,
                "Text length must be at least 3 characters."
            )
        )
        if (textInspector.data.length > maxTextLength) msgs.add(
            ToDoMessage(
                textInspector.data,
                "Text length is to long (max $maxTextLength chars)."
            )
        )

        return msgs
    }


}

object ToDoResource : Resource<ActDto, Long> {
    override val idProvider: IdProvider<ActDto, Long> = ActDto::id
    override fun deserialize(source: String): ActDto = Json.decodeFromString(ActDto.serializer(), source)
    override fun deserializeList(source: String): List<ActDto> =
        Json.decodeFromString(ListSerializer(ActDto.serializer()), source)

    override fun serialize(item: ActDto): String = Json.encodeToString(ActDto.serializer(), item)
    override fun serializeList(items: List<ActDto>): String =
        Json.encodeToString(ListSerializer(ActDto.serializer()), items)
}