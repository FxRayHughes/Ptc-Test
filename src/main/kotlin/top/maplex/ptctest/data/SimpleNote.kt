package top.maplex.ptctest.data

import taboolib.expansion.ColumnType
import taboolib.expansion.Length
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnTypeSQLite

/**
 * 简单笔记数据类 —— 无 @Id + @ColumnType 测试模型
 *
 * 本类不包含 @Id 注解，框架会自动添加一个名为 `id` 的自增主键列。
 * 适用于不需要业务主键、仅通过自增 rowId 定位记录的场景。
 *
 * 演示 @ColumnType 注解：显式指定 SQL 和 SQLite 的列类型，
 * 覆盖框架的自动类型推断（默认 String → VARCHAR）。
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE simple_note (
 *     id         INTEGER PRIMARY KEY AUTOINCREMENT,  -- 框架自动添加的自增主键
 *     title      TEXT,                               -- @Length(64)
 *     content    TEXT,                               -- @ColumnType 显式指定为 TEXT
 *     created_at INTEGER                             -- Long → INTEGER
 * )
 * ```
 *
 * @property title     笔记标题。@Length(64) 限制 VARCHAR 长度（MySQL 下生效）。
 * @property content   笔记内容。@ColumnType 显式指定为 TEXT 类型，可存储超过 VARCHAR 默认长度的长文本。
 * @property createdAt 创建时间戳（毫秒）。
 */
data class SimpleNote(
    @Length(64) val title: String,
    @ColumnType(sql = ColumnTypeSQL.TEXT, sqlite = ColumnTypeSQLite.TEXT) var content: String,
    var createdAt: Long
)
