package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.noteMapper
import top.maplex.ptctest.data.SimpleNote

/**
 * 无 @Id 数据类的 rowId 操作测试
 *
 * 本测试验证当数据类没有 @Id 注解时，如何通过框架自动生成的
 * 自增 `id` 列（rowId）来定位和操作记录。
 *
 * ## 背景知识
 *
 * 数据类的两种主键模式：
 *
 * 1. **有 @Id**（如 PlayerHome）：
 *    业务代码指定主键值，使用 findById/deleteById 等方法。
 *
 * 2. **无 @Id**（如 SimpleNote）：
 *    框架自动添加 `id INTEGER PRIMARY KEY AUTOINCREMENT` 列。
 *    业务代码通过 `insertAndGetKey` 获取自增 id，
 *    再用 `findByRowId` / `deleteByRowId` 操作。
 *
 * ## 测试的 API 方法
 *
 * | 方法                          | 说明                                          |
 * |-----------------------------|-----------------------------------------------|
 * | `insertAndGetKey(entity)`   | 插入记录并返回自增 rowId（Long）                 |
 * | `findByRowId(rowId)`        | 通过自增 rowId 查询单条记录，返回 T?             |
 * | `deleteByRowId(rowId)`      | 通过自增 rowId 删除单条记录                      |
 *
 * ## findByRowId vs findById 的区别
 *
 * - `findById(id)`：通过 @Id 注解标记的业务字段查询。
 *   只有标注了 @Id 的数据类才能使用。
 *
 * - `findByRowId(rowId)`：通过框架自动生成的自增 id 列查询。
 *   主要用于无 @Id 的数据类，但有 @Id 的数据类也可以使用
 *   （此时 rowId 是框架内部维护的隐式列）。
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- insertAndGetKey
 * INSERT INTO `simple_note` (`title`, `content`, `created_at`) VALUES (?, ?, ?)
 * -- 返回 LAST_INSERT_ROWID()
 *
 * -- findByRowId
 * SELECT * FROM `simple_note` WHERE `id` = ?
 *
 * -- deleteByRowId
 * DELETE FROM `simple_note` WHERE `id` = ?
 * ```
 */
object TestRowId {

    fun run(sender: ProxyCommandSender) {
        noteMapper.deleteWhere { "title" eq "rowid_test" }

        // insertAndGetKey —— 获取框架自动生成的自增 rowId
        // 这是无 @Id 数据类操作记录的唯一入口：
        // 必须先通过 insertAndGetKey 拿到 rowId，才能后续用 findByRowId/deleteByRowId
        val note = SimpleNote("rowid_test", "rowid content", System.currentTimeMillis())
        val rowId = noteMapper.insertAndGetKey(note)
        assert(rowId > 0, "insertAndGetKey 应返回正数, 实际: $rowId")
        sender.sendMessage("§7  insertAndGetKey 返回 rowId: $rowId")

        // findByRowId —— 通过自增 ID 查询
        // 生成 SQL: SELECT * FROM `simple_note` WHERE `id` = ?
        val found = noteMapper.findByRowId(rowId)
        assert(found != null, "findByRowId 应返回非空")
        assert(found!!.title == "rowid_test", "findByRowId title 应为 rowid_test")
        sender.sendMessage("§7  findByRowId 完成: ${found.title}")

        // deleteByRowId —— 通过自增 ID 删除
        // 生成 SQL: DELETE FROM `simple_note` WHERE `id` = ?
        noteMapper.deleteByRowId(rowId)
        val afterDelete = noteMapper.findByRowId(rowId)
        assert(afterDelete == null, "deleteByRowId 后 findByRowId 应返回 null")
        sender.sendMessage("§7  deleteByRowId 完成")
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
