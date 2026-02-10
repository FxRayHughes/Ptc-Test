package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * 基础 CRUD 测试
 *
 * 本测试验证 DataMapper 最核心的六个操作，覆盖单条记录的完整生命周期：
 *
 * ┌─────────┐    ┌──────────┐    ┌─────────┐    ┌────────┐    ┌────────┐    ┌────────────┐
 * │ insert  │ →  │ findById │ →  │ findAll │ →  │ update │ →  │ exists │ →  │ deleteById │
 * └─────────┘    └──────────┘    └─────────┘    └────────┘    └────────┘    └────────────┘
 *
 * ## 测试的 API 方法
 *
 * | 方法                    | 说明                                           |
 * |------------------------|------------------------------------------------|
 * | `insert(entity)`       | 插入单条记录，所有字段（val+var）均参与 INSERT    |
 * | `findById(id)`         | 通过 @Id 字段值查询单条记录，返回 T?             |
 * | `findAll(id)`          | 通过 @Id 字段值查询所有匹配记录，返回 List<T>    |
 * | `update(entity)`       | 通过 @Id 定位记录，仅更新 var 字段               |
 * | `exists(id)`           | 通过 @Id 检查记录是否存在，返回 Boolean          |
 * | `deleteById(id)`       | 通过 @Id 删除单条记录                            |
 *
 * ## 关键概念
 *
 * - **@Id 字段**：`PlayerHome.username` 标注了 `@Id`，是逻辑主键。
 *   SQLite 下为 PRIMARY KEY（唯一约束），MySQL 下为 KEY（普通索引）。
 *
 * - **val vs var 与 update 行为**：
 *   `insert` 时所有字段都会写入数据库。
 *   `update` 时只有 `var` 字段参与 SET 子句，`val` 字段（如 username、serverName）不会被更新。
 *   这是因为 `val` 字段通常是标识符，不应在更新时被修改。
 *
 * - **data class copy()**：
 *   Kotlin data class 的 `copy()` 方法创建一个浅拷贝，可以覆盖指定字段。
 *   这里用 `found.copy(world = "world_nether", x = 10.0)` 创建更新后的对象。
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- insert
 * INSERT INTO `player_home` (`username`, `server_name`, `world`, `x`, `y`, `z`, `active`)
 * VALUES (?, ?, ?, ?, ?, ?, ?)
 *
 * -- findById
 * SELECT * FROM `player_home` WHERE `username` = ?
 *
 * -- findAll(id)
 * SELECT * FROM `player_home` WHERE `username` = ?
 *
 * -- update（只更新 var 字段）
 * UPDATE `player_home` SET `world` = ?, `x` = ?, `y` = ?, `z` = ?, `active` = ?
 * WHERE `username` = ?
 *
 * -- exists
 * SELECT COUNT(1) FROM `player_home` WHERE `username` = ?
 *
 * -- deleteById
 * DELETE FROM `player_home` WHERE `username` = ?
 * ```
 */
object TestBasic {

    fun run(sender: ProxyCommandSender) {
        // 前置清理：确保测试数据不存在残留
        homeMapper.deleteWhere { "username" eq "test_basic" }

        // 1. insert —— 插入单条记录
        //    所有构造参数（val + var）都会参与 INSERT 语句
        //    生成 SQL: INSERT INTO `player_home` (`username`, `server_name`, `world`, `x`, `y`, `z`, `active`) VALUES (?, ?, ?, ?, ?, ?, ?)
        val home = PlayerHome("test_basic", "lobby", "world", 1.0, 2.0, 3.0, true)
        homeMapper.insert(home)
        sender.sendMessage("§7  insert 完成")

        // 2. findById —— 通过 @Id 值查询单条记录
        //    @Id 字段是 username，框架生成: SELECT * FROM `player_home` WHERE `username` = ?
        //    返回 T?，未找到时返回 null
        val found = homeMapper.findById("test_basic")
        assert(found != null, "findById 应返回非空")
        assert(found!!.world == "world", "findById world 应为 world, 实际: ${found.world}")
        assert(found.x == 1.0, "findById x 应为 1.0")
        sender.sendMessage("§7  findById 完成")

        // 3. findAll(id) —— 通过 @Id 值查询所有匹配记录
        //    与 findById 的区别：返回 List<T> 而非 T?
        //    SQLite 下 @Id 是 PRIMARY KEY（唯一），所以最多返回 1 条
        //    MySQL 下 @Id 是普通 KEY（非唯一），可能返回多条
        val allById = homeMapper.findAll("test_basic")
        assert(allById.isNotEmpty(), "findAll(id) 应返回非空列表")
        sender.sendMessage("§7  findAll(id) 完成, 数量: ${allById.size}")

        // 4. update —— 通过 @Id 定位并更新 var 字段
        //    关键：只有 var 字段（world, x, y, z, active）参与 SET 子句
        //    val 字段（username, serverName）不会被更新，它们只用于 WHERE 定位
        //    生成 SQL: UPDATE `player_home` SET `world`=?, `x`=?, `y`=?, `z`=?, `active`=? WHERE `username`=?
        val updated = found.copy(world = "world_nether", x = 10.0)
        homeMapper.update(updated)
        val afterUpdate = homeMapper.findById("test_basic")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.world == "world_nether", "update 后 world 应为 world_nether, 实际: ${afterUpdate.world}")
        assert(afterUpdate.x == 10.0, "update 后 x 应为 10.0")
        sender.sendMessage("§7  update 完成")

        // 5. exists —— 通过 @Id 检查记录是否存在
        //    生成 SQL: SELECT COUNT(1) FROM `player_home` WHERE `username` = ?
        //    返回 Boolean
        val ex = homeMapper.exists("test_basic")
        assert(ex, "exists 应返回 true")
        sender.sendMessage("§7  exists 完成")

        // 6. deleteById —— 通过 @Id 删除记录
        //    生成 SQL: DELETE FROM `player_home` WHERE `username` = ?
        homeMapper.deleteById("test_basic")
        val afterDelete = homeMapper.findById("test_basic")
        assert(afterDelete == null, "deleteById 后 findById 应返回 null")
        sender.sendMessage("§7  deleteById 完成")
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
