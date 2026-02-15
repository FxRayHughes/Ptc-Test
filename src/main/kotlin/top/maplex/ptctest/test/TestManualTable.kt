package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.manualHomeMapper
import top.maplex.ptctest.data.ManualHome

/**
 * 手动建表集成测试
 *
 * 验证 `manualTable(...)` 功能：跳过框架自动建表，执行用户提供的 SQL 创建表结构。
 *
 * ## manualTable API
 *
 * ```kotlin
 * val manualHomeMapper by mapper<ManualHome>(dbFile("test_manual.db")) {
 *     manualTable(
 *         """CREATE TABLE IF NOT EXISTS manual_home (
 *             username VARCHAR(64) PRIMARY KEY,
 *             world VARCHAR(64),
 *             x REAL DEFAULT 0,
 *             y REAL DEFAULT 0,
 *             z REAL DEFAULT 0
 *         )"""
 *     )
 * }
 * ```
 *
 * ## 核心行为
 *
 * - 框架不执行自动 `CREATE TABLE`，而是执行 `manualTable(...)` 中的 SQL
 * - 数据类字段与手动建表 SQL 的列结构需保持一致
 * - CRUD 操作与自动建表模式完全相同
 *
 * ## 测试覆盖
 *
 * 1. insert + findById —— 手动建表后 CRUD 正常工作
 * 2. update —— 更新 var 字段
 * 3. delete —— 删除记录
 */
object TestManualTable {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        manualHomeMapper.deleteWhere { "username" like "mt_%" }

        // ==================== 1. insert + findById ====================
        sender.sendMessage("§7  --- insert + findById 测试 ---")

        val home = ManualHome(
            username = "mt_player1",
            world = "world",
            x = 100.0, y = 64.0, z = -200.0
        )
        manualHomeMapper.insert(home)

        val found = manualHomeMapper.findById("mt_player1")
        assert(found != null, "findById 应返回非空")
        assert(found!!.username == "mt_player1", "username 应为 mt_player1")
        assert(found.world == "world", "world 应为 world, 实际: ${found.world}")
        assert(found.x == 100.0, "x 应为 100.0, 实际: ${found.x}")
        assert(found.y == 64.0, "y 应为 64.0, 实际: ${found.y}")
        assert(found.z == -200.0, "z 应为 -200.0, 实际: ${found.z}")
        sender.sendMessage("§7  insert+findById 完成")

        // ==================== 2. update ====================
        sender.sendMessage("§7  --- update 测试 ---")

        manualHomeMapper.update(ManualHome(
            username = "mt_player1",
            world = "world_nether",
            x = 999.0, y = 128.0, z = -999.0
        ))

        val afterUpdate = manualHomeMapper.findById("mt_player1")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.world == "world_nether",
            "update 后 world 应为 world_nether, 实际: ${afterUpdate.world}")
        assert(afterUpdate.x == 999.0, "update 后 x 应为 999.0, 实际: ${afterUpdate.x}")
        sender.sendMessage("§7  update 完成")

        // ==================== 3. delete ====================
        sender.sendMessage("§7  --- delete 测试 ---")

        manualHomeMapper.deleteById("mt_player1")
        val afterDelete = manualHomeMapper.findById("mt_player1")
        assert(afterDelete == null, "deleteById 后应返回 null")
        sender.sendMessage("§7  delete 完成")

        // 清理
        manualHomeMapper.deleteWhere { "username" like "mt_%" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
