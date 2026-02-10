package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.noteMapper
import top.maplex.ptctest.data.SimpleNote

/**
 * 自增主键获取测试
 *
 * 本测试验证 `insertAndGetKey` 和 `insertBatchAndGetKeys` 两个方法，
 * 它们在插入记录的同时返回数据库生成的自增主键值。
 *
 * ## 适用场景
 *
 * 当数据类没有 @Id 注解时，框架自动添加 `id INTEGER PRIMARY KEY AUTOINCREMENT` 列。
 * 业务代码无法在构造对象时指定这个 id，需要在插入后通过返回值获取。
 *
 * 典型用途：
 * - 插入后需要立即用 id 做后续操作（如关联其他表）
 * - 需要用 `findByRowId` / `deleteByRowId` 操作刚插入的记录
 *
 * ## 测试的 API 方法
 *
 * | 方法                              | 说明                                              |
 * |----------------------------------|---------------------------------------------------|
 * | `insertAndGetKey(entity)`        | 插入单条记录并返回自增主键值（Long）                  |
 * | `insertBatchAndGetKeys(list)`    | 批量插入并返回自增主键列表（List<Long>）              |
 * | `findOne { filter }`             | 通过条件查询单条，验证批量插入的数据                   |
 * | `deleteByRowId(rowId)`           | 通过自增 rowId 删除记录                              |
 *
 * ## 关键概念
 *
 * - **自增主键 vs @Id**：
 *   有 @Id 的数据类，主键由业务代码指定（如 username）。
 *   无 @Id 的数据类，主键由数据库自动生成（自增 id）。
 *   `insertAndGetKey` 返回的就是这个自动生成的 id。
 *
 * - **SQLite 批量插入的限制**：
 *   SQLite 的 `getGeneratedKeys()` 在批量插入时只返回最后一条的 ID。
 *   因此 `insertBatchAndGetKeys` 返回的列表长度可能为 1 而非 dataList.size。
 *   这是 SQLite JDBC 驱动的已知行为，MySQL 不受此限制。
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- insertAndGetKey（与普通 insert 相同，但使用 RETURN_GENERATED_KEYS 标志）
 * INSERT INTO `simple_note` (`title`, `content`, `created_at`) VALUES (?, ?, ?)
 *
 * -- insertBatchAndGetKeys（批量版本）
 * INSERT INTO `simple_note` (`title`, `content`, `created_at`) VALUES (?, ?, ?)
 * -- 使用 PreparedStatement.addBatch() + executeBatch()
 *
 * -- deleteByRowId
 * DELETE FROM `simple_note` WHERE `id` = ?
 * ```
 */
object TestAutoKey {

    fun run(sender: ProxyCommandSender) {
        noteMapper.deleteWhere { "title" eq "autokey_1" }
        noteMapper.deleteWhere { "title" eq "autokey_2" }
        noteMapper.deleteWhere { "title" eq "autokey_3" }

        // insertAndGetKey —— 插入并返回自增主键值
        // 内部使用 Statement.RETURN_GENERATED_KEYS 标志
        // 执行 INSERT 后通过 getGeneratedKeys() 获取数据库生成的 id
        val note = SimpleNote("autokey_1", "content_1", System.currentTimeMillis())
        val key = noteMapper.insertAndGetKey(note)
        assert(key > 0, "insertAndGetKey 应返回正数, 实际: $key")
        sender.sendMessage("§7  insertAndGetKey 返回: $key")

        // insertBatchAndGetKeys —— 批量插入并返回自增主键列表
        // 内部使用 PreparedStatement.addBatch() + executeBatch()
        // 然后通过 getGeneratedKeys() 获取所有生成的 id
        val notes = listOf(
            SimpleNote("autokey_2", "content_2", System.currentTimeMillis()),
            SimpleNote("autokey_3", "content_3", System.currentTimeMillis())
        )
        val keys = noteMapper.insertBatchAndGetKeys(notes)
        // ⚠ SQLite 限制：批量插入可能只返回最后一个 key
        // 这是 SQLite JDBC 驱动的行为，不是框架的 bug
        // MySQL 驱动会正确返回所有 key
        assert(keys.isNotEmpty(), "insertBatchAndGetKeys 应返回非空列表, 实际: ${keys.size}")
        assert(keys.all { it > 0 }, "所有 key 应为正数")
        sender.sendMessage("§7  insertBatchAndGetKeys 返回: $keys (数量: ${keys.size})")

        // 额外验证：通过 findOne 确认批量插入的数据确实存在
        val found2 = noteMapper.findOne { "title" eq "autokey_2" }
        val found3 = noteMapper.findOne { "title" eq "autokey_3" }
        assert(found2 != null, "autokey_2 应存在")
        assert(found3 != null, "autokey_3 应存在")
        sender.sendMessage("§7  批量插入数据验证完成")

        // 清理：用 deleteByRowId 和 deleteWhere 两种方式
        // deleteByRowId 通过自增 id 精确删除
        // deleteWhere 通过自定义条件删除
        noteMapper.deleteByRowId(key)
        noteMapper.deleteWhere { "title" eq "autokey_2" }
        noteMapper.deleteWhere { "title" eq "autokey_3" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
