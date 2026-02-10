package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.noteMapper
import top.maplex.ptctest.data.SimpleNote

/**
 * @ColumnType 注解测试
 *
 * 本测试验证 `@ColumnType` 注解能正确控制数据库列类型，
 * 使其能存储超过框架默认 VARCHAR 长度限制的长文本。
 *
 * ## 背景知识
 *
 * 框架对 String 类型字段的默认映射：
 * - MySQL: `VARCHAR(64)`（可通过 @Length 调整长度，但仍有上限）
 * - SQLite: `TEXT`（SQLite 的 TEXT 无长度限制，但框架仍会按 VARCHAR 处理）
 *
 * 当需要存储超过 VARCHAR 长度的文本时，使用 `@ColumnType` 显式指定列类型：
 * ```kotlin
 * @ColumnType(sql = ColumnTypeSQL.TEXT, sqlite = ColumnTypeSQLite.TEXT)
 * var content: String
 * ```
 *
 * ## 测试的 API 方法
 *
 * | 方法                        | 说明                                              |
 * |----------------------------|---------------------------------------------------|
 * | `insert(entity)`           | 插入包含 @ColumnType(TEXT) 字段的记录               |
 * | `findOne { filter }`       | 通过自定义条件查询单条记录（无 @Id 数据类专用）       |
 * | `deleteWhere { filter }`   | 通过自定义条件删除记录                               |
 *
 * ## 关键概念
 *
 * - **@ColumnType 注解**：显式指定 SQL 和 SQLite 的列类型，覆盖框架的自动类型推断。
 *   需要分别指定 `sql`（MySQL）和 `sqlite`（SQLite）两个参数。
 *
 * - **findOne vs findById**：
 *   `findById` 通过 @Id 字段查询，`findOne` 通过自定义 filter 条件查询。
 *   对于没有 @Id 的数据类（如 SimpleNote），只能用 `findOne` 或 `findAll`。
 *
 * - **无 @Id 数据类**：
 *   SimpleNote 没有 @Id 注解，框架自动添加 `id INTEGER PRIMARY KEY AUTOINCREMENT` 列。
 *   业务代码无法直接访问这个自增 id，需要通过 `insertAndGetKey` 获取。
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- 建表（content 列为 TEXT 而非默认的 VARCHAR）
 * CREATE TABLE `simple_note` (
 *     `id` INTEGER PRIMARY KEY AUTOINCREMENT,
 *     `title` TEXT,
 *     `content` TEXT,        -- @ColumnType 指定
 *     `created_at` INTEGER
 * )
 *
 * -- insert
 * INSERT INTO `simple_note` (`title`, `content`, `created_at`) VALUES (?, ?, ?)
 *
 * -- findOne
 * SELECT * FROM `simple_note` WHERE `title` = ? LIMIT 1
 * ```
 */
object TestColumn {

    fun run(sender: ProxyCommandSender) {
        noteMapper.deleteWhere { "title" eq "test_column_title" }

        // 构造 500 字符的长文本，超过 VARCHAR 默认长度 64
        // 如果列类型是 VARCHAR(64)，这段文本会被截断或报错
        // 使用 @ColumnType(TEXT) 后，可以完整存储任意长度字符串
        val longContent = "A".repeat(500)
        val note = SimpleNote("test_column_title", longContent, System.currentTimeMillis())
        noteMapper.insert(note)
        sender.sendMessage("§7  insert SimpleNote 完成")

        // findOne —— 无 @Id 数据类通过 filter 条件查询单条
        // filter DSL: { "列名" eq 值 } 生成 WHERE `title` = ?
        // findOne 自动添加 LIMIT 1，只返回第一条匹配记录
        val found = noteMapper.findOne { "title" eq "test_column_title" }
        assert(found != null, "findOne 应返回非空")
        // 验证长文本完整性：内容未被截断
        assert(found!!.content == longContent, "content 长度应为 500, 实际: ${found.content.length}")
        sender.sendMessage("§7  查询验证完成, content 长度: ${found.content.length}")

        noteMapper.deleteWhere { "title" eq "test_column_title" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
