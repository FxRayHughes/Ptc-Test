package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.pgHomeMapper
import top.maplex.ptctest.pgPlayerHomeMapper
import top.maplex.ptctest.pgStatsMapper
import top.maplex.ptctest.data.PgHome
import top.maplex.ptctest.data.PlayerHome
import top.maplex.ptctest.data.PlayerStats

/**
 * PostgreSQL 集成测试
 *
 * 验证 PostgreSQL 数据库的基础 CRUD 操作、@TableName 自定义表名、JOIN 联查。
 *
 * ## 测试内容
 *
 * 1. **@TableName 自定义表名**：PgHome 使用 @TableName("pg_player_home")，
 *    验证表名不是默认的 "pg_home" 而是 "pg_player_home"
 * 2. **基础 CRUD**：insert / findById / findAll / update / exists / deleteById
 * 3. **JOIN 联查**：PostgreSQL 下的多表 INNER JOIN
 * 4. **批量操作**：insertBatch / upsertBatch
 *
 * ## 前提条件
 *
 * - PostgreSQL 服务可用
 * - config.yml 中 postgresql 节点配置正确
 * - 已安装 database-postgresql 模块（提供 JDBC 驱动）
 */
object TestPostgreSQL {

    fun run(sender: ProxyCommandSender) {
        testTableName(sender)
        testBasicCrud(sender)
        testBatch(sender)
        testJoin(sender)
    }

    /**
     * 测试 @TableName 自定义表名
     */
    private fun testTableName(sender: ProxyCommandSender) {
        // PgHome 使用 @TableName("pg_player_home")
        // 验证 tableName 属性返回自定义表名
        val tableName = pgHomeMapper.tableName
        assert(tableName == "pg_player_home",
            "@TableName 表名应为 pg_player_home, 实际: $tableName")
        sender.sendMessage("§7  @TableName 验证完成: $tableName")
    }

    /**
     * 基础 CRUD 测试（PostgreSQL）
     */
    private fun testBasicCrud(sender: ProxyCommandSender) {
        pgHomeMapper.deleteWhere { "username" eq "pg_test" }

        // 1. insert
        val home = PgHome("pg_test", "lobby", "world", 1.0, 2.0, 3.0, true)
        pgHomeMapper.insert(home)
        sender.sendMessage("§7  insert 完成")

        // 2. findById
        val found = pgHomeMapper.findById("pg_test")
        assert(found != null, "findById 应返回非空")
        assert(found!!.world == "world", "world 应为 world, 实际: ${found.world}")
        assert(found.x == 1.0, "x 应为 1.0, 实际: ${found.x}")
        assert(found.active, "active 应为 true")
        sender.sendMessage("§7  findById 完成")

        // 3. findAll
        val allById = pgHomeMapper.findAll("pg_test")
        assert(allById.isNotEmpty(), "findAll 应返回非空列表")
        sender.sendMessage("§7  findAll 完成, 数量: ${allById.size}")

        // 4. update
        val updated = found.copy(world = "world_nether", x = 10.0)
        pgHomeMapper.update(updated)
        val afterUpdate = pgHomeMapper.findById("pg_test")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.world == "world_nether",
            "update 后 world 应为 world_nether, 实际: ${afterUpdate.world}")
        assert(afterUpdate.x == 10.0, "update 后 x 应为 10.0")
        sender.sendMessage("§7  update 完成")

        // 5. exists
        val ex = pgHomeMapper.exists("pg_test")
        assert(ex, "exists 应返回 true")
        sender.sendMessage("§7  exists 完成")

        // 6. deleteById
        pgHomeMapper.deleteById("pg_test")
        val afterDelete = pgHomeMapper.findById("pg_test")
        assert(afterDelete == null, "deleteById 后应返回 null")
        sender.sendMessage("§7  deleteById 完成")
    }

    /**
     * 批量操作测试（PostgreSQL）
     */
    private fun testBatch(sender: ProxyCommandSender) {
        // 清理
        pgHomeMapper.deleteWhere { "username" eq "pg_batch_1" }
        pgHomeMapper.deleteWhere { "username" eq "pg_batch_2" }
        pgHomeMapper.deleteWhere { "username" eq "pg_batch_3" }

        // insertBatch
        val homes = listOf(
            PgHome("pg_batch_1", "lobby", "world", 1.0, 2.0, 3.0, true),
            PgHome("pg_batch_2", "lobby", "world", 4.0, 5.0, 6.0, false),
            PgHome("pg_batch_3", "lobby", "world_nether", 7.0, 8.0, 9.0, true),
        )
        pgHomeMapper.insertBatch(homes)
        sender.sendMessage("§7  insertBatch 完成")

        // 验证
        val count = pgHomeMapper.count()
        assert(count >= 3, "insertBatch 后 count 应 >= 3, 实际: $count")
        sender.sendMessage("§7  count 验证完成: $count")

        // upsertBatch
        val upsertList = listOf(
            PgHome("pg_batch_1", "lobby", "world_end", 100.0, 200.0, 300.0, false),
            PgHome("pg_batch_2", "lobby", "world_end", 400.0, 500.0, 600.0, true),
        )
        pgHomeMapper.upsertBatch(upsertList)
        val afterUpsert = pgHomeMapper.findById("pg_batch_1")
        assert(afterUpsert != null, "upsert 后 findById 应返回非空")
        assert(afterUpsert!!.world == "world_end",
            "upsert 后 world 应为 world_end, 实际: ${afterUpsert.world}")
        sender.sendMessage("§7  upsertBatch 完成")
    }

    /**
     * JOIN 联查测试（PostgreSQL）
     */
    private fun testJoin(sender: ProxyCommandSender) {
        pgPlayerHomeMapper.deleteWhere { "username" eq "pg_join_user" }
        pgStatsMapper.deleteWhere { "username" eq "pg_join_user" }

        pgPlayerHomeMapper.insert(
            PlayerHome("pg_join_user", "lobby", "world", 1.0, 64.0, 1.0, true)
        )
        pgStatsMapper.insert(
            PlayerStats("pg_join_user", 100, 50, 36000L)
        )
        sender.sendMessage("§7  join 数据插入完成")

        val results = pgPlayerHomeMapper.join {
            innerJoin<PlayerStats> {
                on("player_home.username" eq pre("player_stats.username"))
            }
            selectAs(
                "player_home.username" to "username",
                "player_home.world" to "world",
                "player_stats.kills" to "kills",
                "player_stats.deaths" to "deaths"
            )
            where { "player_home.username" eq "pg_join_user" }
        }.execute()

        assert(results.isNotEmpty(), "join 应返回非空结果")
        val row = results[0]
        assert(row.get<Any>("username").toString() == "pg_join_user",
            "join username 应为 pg_join_user")
        assert(row.get<Any>("kills").toString().toInt() == 100,
            "join kills 应为 100")
        sender.sendMessage("§7  join 完成, username=${row.get<Any>("username")}, kills=${row.get<Any>("kills")}")
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
