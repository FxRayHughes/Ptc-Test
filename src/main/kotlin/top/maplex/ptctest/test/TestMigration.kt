package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.migrationHomeMapper
import top.maplex.ptctest.data.MigrationHome

/**
 * 版本迁移集成测试
 *
 * 验证 `migration {}` 功能：通过 `_ptc_meta` 表跟踪版本号，按版本执行迁移 SQL。
 *
 * ## migration API
 *
 * ```kotlin
 * val migrationHomeMapper by mapper<MigrationHome>(dbFile("test_migration.db")) {
 *     manualTable(
 *         """CREATE TABLE IF NOT EXISTS migration_home (
 *             username VARCHAR(64) PRIMARY KEY,
 *             world VARCHAR(64)
 *         )"""
 *     )
 *     migration {
 *         version(1,
 *             "ALTER TABLE migration_home ADD COLUMN x REAL DEFAULT 0",
 *             "ALTER TABLE migration_home ADD COLUMN y REAL DEFAULT 0"
 *         )
 *         version(2,
 *             "ALTER TABLE migration_home ADD COLUMN z REAL DEFAULT 0"
 *         )
 *     }
 * }
 * ```
 *
 * ## 核心行为
 *
 * - 框架自动创建 `_ptc_meta` 表记录每张表的当前版本号
 * - 首次启动时执行所有版本的迁移 SQL（version 1 → version 2）
 * - 再次启动时跳过已执行的版本，只执行新增版本
 * - 迁移按版本号升序执行
 *
 * ## 测试覆盖
 *
 * 1. 迁移后 CRUD 正常 —— 手动建表 + 迁移添加的列均可读写
 * 2. 所有字段可用 —— x、y、z 由迁移添加，insert/findById 正常
 * 3. 批量操作 —— 迁移后的表支持 findAll
 */
object TestMigration {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        migrationHomeMapper.deleteWhere { "username" like "mig_%" }

        // ==================== 1. 迁移后 insert + findById ====================
        sender.sendMessage("§7  --- 迁移后 insert + findById 测试 ---")

        // 插入包含迁移添加字段的完整数据
        val home = MigrationHome(
            username = "mig_player1",
            world = "world",
            x = 100.0, y = 64.0, z = -200.0
        )
        migrationHomeMapper.insert(home)

        val found = migrationHomeMapper.findById("mig_player1")
        assert(found != null, "findById 应返回非空")
        assert(found!!.username == "mig_player1", "username 应为 mig_player1")
        assert(found.world == "world", "world 应为 world, 实际: ${found.world}")
        // x, y 由 version(1) 迁移添加
        assert(found.x == 100.0, "x 应为 100.0（version 1 迁移列）, 实际: ${found.x}")
        assert(found.y == 64.0, "y 应为 64.0（version 1 迁移列）, 实际: ${found.y}")
        // z 由 version(2) 迁移添加
        assert(found.z == -200.0, "z 应为 -200.0（version 2 迁移列）, 实际: ${found.z}")
        sender.sendMessage("§7  insert+findById 完成: 迁移列 x=${found.x}, y=${found.y}, z=${found.z}")

        // ==================== 2. update 迁移列 ====================
        sender.sendMessage("§7  --- update 迁移列测试 ---")

        migrationHomeMapper.update(MigrationHome(
            username = "mig_player1",
            world = "world_nether",
            x = 999.0, y = 128.0, z = -999.0
        ))

        val afterUpdate = migrationHomeMapper.findById("mig_player1")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.world == "world_nether",
            "update 后 world 应为 world_nether, 实际: ${afterUpdate.world}")
        assert(afterUpdate.x == 999.0, "update 后 x 应为 999.0, 实际: ${afterUpdate.x}")
        assert(afterUpdate.z == -999.0, "update 后 z 应为 -999.0, 实际: ${afterUpdate.z}")
        sender.sendMessage("§7  update 完成: x=${afterUpdate.x}, z=${afterUpdate.z}")

        // ==================== 3. 批量操作 ====================
        sender.sendMessage("§7  --- 批量操作测试 ---")

        migrationHomeMapper.insert(MigrationHome("mig_player2", "nether", 50.0, 32.0, 50.0))
        migrationHomeMapper.insert(MigrationHome("mig_player3", "end", 0.0, 100.0, 0.0))

        val all = migrationHomeMapper.findAll { "username" like "mig_%" }
        assert(all.size == 3, "findAll 应返回 3 条, 实际: ${all.size}")
        sender.sendMessage("§7  批量查询完成: ${all.size} 条记录")

        // 清理
        migrationHomeMapper.deleteWhere { "username" like "mig_%" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
