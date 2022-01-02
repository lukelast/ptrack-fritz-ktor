package app.database

import app.backend.validator
import app.model.ActDto
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object ActTable : LongIdTable() {
    val text = varchar("text", validator.maxTextLength)
    val completed = bool("completed")
    val date = long("date")
}

class ActEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ActEntity>(ActTable)

    var text by ActTable.text
    var completed by ActTable.completed
    var date by ActTable.date

    fun toDto() = ActDto(this.id.value, this.text, this.completed)
}

object Db {

    fun find(id: Long): ActEntity? = database {
        ActEntity.findById(id)
    }

    fun all(): List<ActDto> = database {
        ActEntity.all().map { it.toDto() }
    }

    fun add(actDto: ActDto): ActDto = database {
        ActEntity.new {
            text = actDto.text
            completed = actDto.completed
            date = 0
        }.toDto()
    }

    fun update(old: ActEntity, new: ActDto): ActDto = database {
        old.text = new.text
        old.completed = new.completed
        new
    }

    fun remove(toDelete: ActEntity) = database {
        toDelete.delete()
    }
}

fun <T> database(statement: Transaction.() -> T): T {
    return transaction {
        addLogger(StdOutSqlLogger)
        statement()
    }
}