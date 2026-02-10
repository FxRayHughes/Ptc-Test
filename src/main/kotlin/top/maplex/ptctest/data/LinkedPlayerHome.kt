package top.maplex.ptctest.data

import taboolib.expansion.ColumnType
import taboolib.expansion.Id
import taboolib.expansion.Length
import taboolib.expansion.LinkTable
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnTypeSQLite

/**
 * 关联表测试数据类 —— 演示 @LinkTable + @ColumnType 组合使用
 *
 * **@LinkTable 行为：**
 * - 建表时创建外键列（stats_username），类型与关联类 @Id 字段一致
 * - 查询时自动 LEFT JOIN player_stats 表，构建完整对象
 * - 写入时级联保存关联对象（不存在则插入，已存在则更新）
 *
 * **@ColumnType 行为：**
 * - 显式指定 description 列为 TEXT 类型，可存储超过 VARCHAR 默认长度的长文本
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE linked_player_home (
 *     username        TEXT PRIMARY KEY,
 *     server_name     TEXT,
 *     world           TEXT,
 *     description     TEXT,             -- @ColumnType 显式指定为 TEXT
 *     stats_username  TEXT              -- @LinkTable 外键列
 * )
 * ```
 *
 * @property username    玩家名，@Id 逻辑主键。
 * @property serverName  服务器名。
 * @property world       世界名。
 * @property description 描述信息。@ColumnType 指定为 TEXT，支持长文本存储。
 * @property stats       关联的玩家统计数据。@LinkTable 自动 LEFT JOIN + 级联保存。
 */
data class LinkedPlayerHome(
    @Id val username: String,
    @Length(32) val serverName: String,
    @Length(32) var world: String,
    @ColumnType(sql = ColumnTypeSQL.TEXT, sqlite = ColumnTypeSQLite.TEXT) var description: String,
    @LinkTable("statsUsername") val stats: PlayerStats?
)
