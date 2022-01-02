package app.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ToDoTests {
    @Test
    fun testToDoValidator() {
        val validator = ToDoValidator()

        assertTrue(
            validator.validate(ActDto(text = "a"), Unit).size == 1,
            "validation for \"too short text\" failed"
        )

        assertTrue(
            validator.validate(ActDto(text = "a".repeat(validator.maxTextLength + 1)), Unit).size == 1,
            "validation for \"max length\" failed"
        )

    }
}