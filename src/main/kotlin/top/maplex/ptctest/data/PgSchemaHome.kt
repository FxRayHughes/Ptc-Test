package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Key
import taboolib.expansion.Length
import taboolib.expansion.TableName

/**
 * PostgreSQL Schema 测试数据类
 *
 * 使用 `@TableName(value = "pg_schema_home", schema = "game")` 指定自定义 Schema。
 * 框架在建表前会自动执行 `CREATE SCHEMA IF NOT EXISTS "game"`，
 * 表名解析为 `game.pg_schema_home`，SQL 中格式化为 `"game"."pg_schema_home"`。
 *
 * @property username   玩家名，逻辑主键
 * @property serverName 服务器名，索引字段
 * @property world      世界名
 * @property x          X 坐标
 * @property y          Y 坐标
 * @property z          Z 坐标
 */
@TableName("pg_schema_home", schema = "game")
data class PgSchemaHome(
    @Id val username: String,
    @Key @Length(32) val serverName: String,
    @Length(32) var world: String,
    var x: Double,
    var y: Double,
    var z: Double
)
