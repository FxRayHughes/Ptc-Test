package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * 批量操作测试
 *
 * 本测试验证 DataMapper 的批量 CRUD 方法，这些方法使用 batch PreparedStatement，
 * 在处理多条记录时性能远优于逐条操作。
 *
 * ## 批量 vs 逐条的性能差异
 *
 * 逐条操作：每条记录独立执行一次 SQL，N 条记录 = N 次网络往返。
 * 批量操作：使用 `addBatch()` + `executeBatch()`，N 条记录 = 1 次网络往返。
 *
 * 对于 SQLite（本地文件），差异不大。
 * 对于 MySQL（远程服务器），批量操作可以减少 90%+ 的网络延迟。
 *
 * ## 测试的 API 方法
 *
 * | 方法                          | 说明                                              |
 * |-----------------------------|---------------------------------------------------|
 * | `insertBatch(list)`         | 批量插入多条记录                                    |
 * | `findByIds(idList)`         | 通过多个 @Id 值批量查询（生成 IN 子句）              |
 * | `updateBatch(list)`         | 批量更新多条记录，通过 @Id + @Key 定位每条            |
 * | `deleteByIds(idList)`       | 通过多个 @Id 值批量删除（生成 IN 子句）              |
 *
 * ## 关键概念
 *
 * - **IN 子句**：`findByIds` 和 `deleteByIds` 生成 `WHERE username IN (?, ?, ?)`，
 *   一次查询/删除多条记录。
 *
 * - **updateBatch 的定位逻辑**：
 *   与单条 `update` 相同，通过 @Id（+ @Key）定位每条记录。
 *   内部使用 `addBatch()` 将多条 UPDATE 语句打包执行。
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- insertBatch（每条记录一个 addBatch）
 * INSERT INTO `player_home` (...) VALUES (?, ?, ?, ?, ?, ?, ?)
 *
 * -- findByIds
 * SELECT * FROM `player_home` WHERE `username` IN (?, ?, ?)
 *
 * -- updateBatch（每条记录一个 addBatch）
 * UPDATE `player_home` SET `world`=?, `x`=?, `y`=?, `z`=?, `active`=?
 * WHERE `username`=? AND `server_name`=?
 *
 * -- deleteByIds
 * DELETE FROM `player_home` WHERE `username` IN (?, ?, ?)
 * ```
 */
object TestBatch {

    fun run(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "batch_user_1" }
        homeMapper.deleteWhere { "username" eq "batch_user_2" }
        homeMapper.deleteWhere { "username" eq "batch_user_3" }

        // insertBatch —— 一次性插入多条记录
        // 内部使用 PreparedStatement.addBatch() + executeBatch()
        val homes = listOf(
            PlayerHome("batch_user_1", "lobby", "world", 1.0, 64.0, 1.0, true),
            PlayerHome("batch_user_2", "lobby", "world", 2.0, 64.0, 2.0, true),
            PlayerHome("batch_user_3", "lobby", "world", 3.0, 64.0, 3.0, false)
        )
        homeMapper.insertBatch(homes)
        sender.sendMessage("§7  insertBatch 完成")

        // findByIds —— 通过多个 @Id 值批量查询
        // 生成 SQL: SELECT * FROM `player_home` WHERE `username` IN (?, ?, ?)
        // 返回 List<T>，顺序不保证与输入 id 列表一致
        val found = homeMapper.findByIds(listOf("batch_user_1", "batch_user_2", "batch_user_3"))
        assert(found.size == 3, "findByIds 应返回 3 条, 实际: ${found.size}")
        sender.sendMessage("§7  findByIds 完成, 数量: ${found.size}")

        // updateBatch —— 批量更新
        // 对每条记录：通过 @Id(username) + @Key(serverName) 定位，更新 var 字段
        // 使用 data class copy() 创建修改后的副本
        val updated = found.map { it.copy(world = "world_end") }
        homeMapper.updateBatch(updated)
        val afterUpdate = homeMapper.findByIds(listOf("batch_user_1", "batch_user_2"))
        assert(afterUpdate.all { it.world == "world_end" }, "updateBatch 后 world 应全为 world_end")
        sender.sendMessage("§7  updateBatch 完成")

        // deleteByIds —— 通过多个 @Id 值批量删除
        // 生成 SQL: DELETE FROM `player_home` WHERE `username` IN (?, ?, ?)
        homeMapper.deleteByIds(listOf("batch_user_1", "batch_user_2", "batch_user_3"))
        val afterDelete = homeMapper.findByIds(listOf("batch_user_1", "batch_user_2", "batch_user_3"))
        assert(afterDelete.isEmpty(), "deleteByIds 后应为空, 实际: ${afterDelete.size}")
        sender.sendMessage("§7  deleteByIds 完成")
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
