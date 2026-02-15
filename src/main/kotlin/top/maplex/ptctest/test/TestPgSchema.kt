package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.pgSchemaHomeMapper
import top.maplex.ptctest.data.PgSchemaHome

/**
 * PostgreSQL Schema 集成测试
 *
 * 验证 `@TableName(schema = "game")` 功能：
 * 框架在建表前自动执行 `CREATE SCHEMA IF NOT EXISTS "game"`，
 * 表名解析为 `game.pg_schema_home`。
 *
 * ## @TableName Schema 支持
 *
 * ```kotlin
 * @TableName("pg_schema_home", schema = "game")
 * data class PgSchemaHome(
 *     @Id val username: String,
 *     @Key @Length(32) val serverName: String,
 *     @Length(32) var world: String,
 *     var x: Double,
 *     var y: Double,
 *     var z: Double
 * )
 * ```
 *
 * ## 核心行为
 *
 * - `resolveTableName()` 返回 `game.pg_schema_home`
 * - `asFormattedColumnName()` 自动格式化为 `"game"."pg_schema_home"`
 * - `ContainerPostgreSQL.init()` 在建表前执行 `CREATE SCHEMA IF NOT EXISTS "game"`
 * - CRUD 操作使用带 Schema 前缀的表名
 *
 * ## 前提条件
 *
 * - PostgreSQL 服务可用
 * - config.yml 中 postgresql 节点配置正确
 *
 * ## 测试覆盖
 *
 * 1. tableName 验证 —— 表名包含 schema 前缀
 * 2. insert + findById —— Schema 下的 CRUD 正常
 * 3. update + delete —— Schema 下的更新和删除
 * 4. 批量操作 —— Schema 下的 findAll
 */
object TestPgSchema {

    fun run(sender: ProxyCommandSender) {
        // 验证表名
        val tableName = pgSchemaHomeMapper.tableName
        assert(tableName == "game.pg_schema_home",
            "tableName 应为 game.pg_schema_home, 实际: $tableName")
        sender.sendMessage("§7  tableName 验证完成: $tableName")

        // 前置清理
        pgSchemaHomeMapper.deleteWhere { "username" like "pgs_%" }

        // ==================== 1. insert + findById ====================
        sender.sendMessage("§7  --- insert + findById 测试 ---")

        val home = PgSchemaHome(
            username = "pgs_player1",
            serverName = "lobby",
            world = "world",
            x = 100.0, y = 64.0, z = -200.0
        )
        pgSchemaHomeMapper.insert(home)

        val found = pgSchemaHomeMapper.findById("pgs_player1")
        assert(found != null, "findById 应返回非空")
        assert(found!!.username == "pgs_player1", "username 应为 pgs_player1")
        assert(found.world == "world", "world 应为 world, 实际: ${found.world}")
        assert(found.x == 100.0, "x 应为 100.0, 实际: ${found.x}")
        sender.sendMessage("§7  insert+findById 完成")

        // ==================== 2. update ====================
        sender.sendMessage("§7  --- update 测试 ---")

        pgSchemaHomeMapper.update(found.copy(world = "world_nether", x = 999.0))
        val afterUpdate = pgSchemaHomeMapper.findById("pgs_player1")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.world == "world_nether",
            "update 后 world 应为 world_nether, 实际: ${afterUpdate.world}")
        assert(afterUpdate.x == 999.0, "update 后 x 应为 999.0, 实际: ${afterUpdate.x}")
        sender.sendMessage("§7  update 完成")

        // ==================== 3. 批量操作 ====================
        sender.sendMessage("§7  --- 批量操作测试 ---")

        pgSchemaHomeMapper.insert(PgSchemaHome("pgs_player2", "lobby", "nether", 50.0, 32.0, 50.0))
        pgSchemaHomeMapper.insert(PgSchemaHome("pgs_player3", "lobby", "end", 0.0, 100.0, 0.0))

        val all = pgSchemaHomeMapper.findAll { "username" like "pgs_%" }
        assert(all.size == 3, "findAll 应返回 3 条, 实际: ${all.size}")
        sender.sendMessage("§7  批量查询完成: ${all.size} 条记录")

        // ==================== 4. delete ====================
        sender.sendMessage("§7  --- delete 测试 ---")

        pgSchemaHomeMapper.deleteById("pgs_player1")
        val afterDelete = pgSchemaHomeMapper.findById("pgs_player1")
        assert(afterDelete == null, "deleteById 后应返回 null")
        sender.sendMessage("§7  delete 完成")

        // 清理
        pgSchemaHomeMapper.deleteWhere { "username" like "pgs_%" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
