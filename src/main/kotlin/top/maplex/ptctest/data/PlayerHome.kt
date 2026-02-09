package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Key
import taboolib.expansion.Length

/**
 * 玩家家园数据类 —— 主测试模型
 *
 * 演示 @Id、@Key、@Length 注解的组合使用。
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE player_home (
 *     username  TEXT PRIMARY KEY,   -- @Id: 逻辑主键，SQLite 下强制唯一
 *     server_name TEXT,             -- @Key: 索引字段，用于 findByKey/updateByKey 复合定位
 *     world     TEXT,
 *     x         REAL,
 *     y         REAL,
 *     z         REAL,
 *     active    INTEGER             -- Boolean 映射为 INTEGER (0/1)
 * )
 * ```
 *
 * 注意：驼峰字段名会自动转为下划线列名（serverName → server_name）。
 *
 * @property username   玩家名，作为 @Id 逻辑主键。CRUD 操作通过此字段定位记录。
 *                      SQLite 下为 PRIMARY KEY（唯一），MySQL 下为 KEY（普通索引，不唯一）。
 * @property serverName 服务器名，作为 @Key 索引字段。与 @Id 组合用于 findByKey/updateByKey 精确定位。
 *                      @Length(32) 指定 VARCHAR 长度为 32。
 * @property world      世界名。@Length(32) 限制存储长度。var 表示可更新字段。
 * @property x          X 坐标。var 表示可更新字段。
 * @property y          Y 坐标。
 * @property z          Z 坐标。
 * @property active     是否激活。Boolean 在数据库中存储为 0/1。
 */
data class PlayerHome(
    @Id val username: String,
    @Key @Length(32) val serverName: String,
    @Length(32) var world: String,
    var x: Double,
    var y: Double,
    var z: Double,
    var active: Boolean
)
