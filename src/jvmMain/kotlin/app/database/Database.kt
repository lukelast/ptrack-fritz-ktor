package app.database

import app.backend.validator
import app.model.ActDto
import app.model.idToActType
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object ActTable : LongIdTable() {
    val time = long("time")
    val type = short("completed")
    val text = varchar("text", validator.maxTextLength)
}

class ActEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ActEntity>(ActTable)

    var time by ActTable.time
    var type by ActTable.type
    var text by ActTable.text


    fun toDto() = ActDto(
        this.id.value,
        time = Instant.fromEpochMilliseconds(this.time),
        text = this.text,
        type = idToActType(this.type)
    )
}

object Db {

    fun find(id: Long): ActEntity? = database {
        ActEntity.findById(id)
    }

    fun all(): List<ActDto> = database {
        ActEntity.all()
            .orderBy(ActTable.time to SortOrder.DESC)
            .limit(50)
            .map { it.toDto() }
    }

    fun add(actDto: ActDto): ActDto = database {
        ActEntity.new {
            text = actDto.text
            type = actDto.type.id
            time = actDto.time.toEpochMilliseconds()
        }.toDto()
    }

    fun update(old: ActEntity, new: ActDto): ActDto = database {
        old.text = new.text
        old.type = new.type.id
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
