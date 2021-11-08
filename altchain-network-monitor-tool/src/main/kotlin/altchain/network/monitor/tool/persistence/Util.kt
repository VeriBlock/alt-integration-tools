package altchain.network.monitor.tool.persistence

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import kotlin.reflect.KClass

fun Table.varchar(name: String): Column<String> = varchar(name, 255)
fun Table.bigVarchar(name: String): Column<String> = varchar(name, 5000)
fun <T : Enum<T>> Table.enumerationByName(name: String, klass: KClass<T>) = enumerationByName(name, 255, klass)