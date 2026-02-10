package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Key
import taboolib.expansion.Length
import taboolib.expansion.TableName

/**
 * PostgreSQL 测试数据类
 *
 * 使用 @TableName 手动指定表名为 "pg_player_home"，
 * 而非默认的类名转换 "pg_home" → "pg_home"。
 *
 * 字段与 PlayerHome 一致，用于验证 PostgreSQL 的基础 CRUD 操作。
 */
@TableName("pg_player_home")
data class PgHome(
    @Id val username: String,
    @Key @Length(32) val serverName: String,
    @Length(32) var world: String,
    var x: Double,
    var y: Double,
    var z: Double,
    var active: Boolean
)
