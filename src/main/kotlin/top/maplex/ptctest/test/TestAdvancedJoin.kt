package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import taboolib.expansion.subQuery
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.statsMapper
import top.maplex.ptctest.data.PlayerHome
import top.maplex.ptctest.data.PlayerStats

/**
 * 高级联查测试：同表自连接 + 子查询 JOIN
 *
 * 本测试验证 join DSL 的三个高级用法，覆盖实际开发中常见的复杂查询场景。
 *
 * ## 场景 1：同表自连接（Self-Join）
 *
 * **需求**：找出同一个 world 中的不同玩家对。
 *
 * **难点**：同一张表 JOIN 两次，必须用不同别名区分。
 * 不能用 `innerJoin<PlayerHome>` 泛型重载（因为两次都是同一个类），
 * 必须用 `innerJoin(name: String)` 传入预格式化的表名+别名。
 *
 * ```kotlin
 * // 错误：泛型重载无法区分两次 JOIN 的是同一张表
 * innerJoin<PlayerHome> { ... }
 *
 * // 正确：用字符串指定 "表名 AS 别名"
 * from("`player_home` AS `h1`")
 * innerJoin("`player_home` AS `h2`") { ... }
 * ```
 *
 * **去重技巧**：`h1.username < h2.username` 避免 (A,B)/(B,A) 重复对。
 * 使用 `lt` + `pre()` 实现列与列的比较。
 *
 * ## 场景 2：子查询 JOIN（DSL 版）
 *
 * **需求**：查询每个玩家的总击杀数和总死亡数（聚合统计），并与 home 表关联。
 *
 * **实现**：使用 `subQuery("表名", "别名") { DSL }` 构建子查询。
 * 子查询复用 ActionSelect 的能力（rows, where, groupBy 等），
 * 框架自动生成子查询 SQL 并收集参数。
 *
 * ```kotlin
 * innerJoin(
 *     subQuery("player_stats", "sub") {
 *         rows("username", "SUM(kills) AS total_kills")
 *         groupBy("username")
 *     }
 * ) {
 *     on("h.username" eq pre("sub.username"))
 * }
 * ```
 *
 * ## 场景 3：子查询带参数绑定
 *
 * **需求**：子查询内部有 WHERE 条件（如 `kills > 50`），需要正确绑定参数。
 *
 * **参数绑定顺序**：框架按以下顺序绑定 `?` 占位符的参数：
 * 1. 子查询内部的参数（如 50）
 * 2. ON 条件的参数（通常无，因为用 pre() 引用列）
 * 3. 外层 WHERE 的参数（如 "adv_world_1"）
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- 场景 1：同表自连接
 * SELECT `h1`.`username` AS `user1`, `h2`.`username` AS `user2`, `h1`.`world` AS `world`
 * FROM `player_home` AS `h1`
 * INNER JOIN `player_home` AS `h2`
 *   ON `h1`.`world` = `h2`.`world` AND `h1`.`username` < `h2`.`username`
 * WHERE `h1`.`world` LIKE ?
 *
 * -- 场景 2：子查询 JOIN
 * SELECT `h`.`username` AS `username`, `sub`.`total_kills` AS `total_kills`
 * FROM `player_home` AS `h`
 * INNER JOIN (
 *     SELECT `username`, SUM(kills) AS total_kills, SUM(deaths) AS total_deaths
 *     FROM `player_stats` GROUP BY `username`
 * ) AS `sub` ON `h`.`username` = `sub`.`username`
 * WHERE `h`.`world` = ?
 *
 * -- 场景 3：子查询带参数
 * SELECT `h`.`username` AS `username`, `sub`.`total_kills` AS `total_kills`
 * FROM `player_home` AS `h`
 * INNER JOIN (
 *     SELECT `username`, SUM(kills) AS total_kills
 *     FROM `player_stats` WHERE `kills` > ? GROUP BY `username`
 * ) AS `sub` ON `h`.`username` = `sub`.`username`
 * WHERE `h`.`world` = ?
 * -- 参数绑定顺序: [50, "adv_world_1"]
 * ```
 */
object TestAdvancedJoin {

    fun run(sender: ProxyCommandSender) {
        // 清理测试数据
        listOf("adv_alice", "adv_bob", "adv_carol").forEach {
            homeMapper.deleteWhere { "username" eq it }
            statsMapper.deleteWhere { "username" eq it }
        }

        // 准备数据：3 个玩家，alice 和 bob 在 adv_world_1，carol 在 adv_world_2
        homeMapper.insert(PlayerHome("adv_alice", "s1", "adv_world_1", 10.0, 64.0, 10.0, true))
        homeMapper.insert(PlayerHome("adv_bob", "s1", "adv_world_1", 20.0, 64.0, 20.0, true))
        homeMapper.insert(PlayerHome("adv_carol", "s1", "adv_world_2", 30.0, 64.0, 30.0, true))
        statsMapper.insert(PlayerStats("adv_alice", 100, 10, 3600L))
        statsMapper.insert(PlayerStats("adv_bob", 200, 20, 7200L))
        statsMapper.insert(PlayerStats("adv_carol", 50, 5, 1800L))
        sender.sendMessage("§7  准备数据完成 (3 homes + 3 stats)")

        // === 场景 1：同表自连接 ===
        // from() 和 innerJoin() 都用 "`表名` AS `别名`" 格式
        // lt + pre() 实现列与列的 < 比较，避免 (A,B)/(B,A) 重复对
        // where/selectAs 中统一用别名 h1/h2 引用
        val selfJoinResults = homeMapper.join {
            from("`player_home` AS `h1`")
            innerJoin("`player_home` AS `h2`") {
                on("h1.world" eq pre("h2.world"))
                on("h1.username" lt pre("h2.username"))
            }
            selectAs(
                "h1.username" to "user1",
                "h2.username" to "user2",
                "h1.world" to "world"
            )
            where { "h1.world" like "adv_world%" }
        }.execute()

        // alice < bob 且同在 adv_world_1，应找到 1 对
        assert(selfJoinResults.size == 1, "自连接应返回 1 对, 实际: ${selfJoinResults.size}")
        val pair = selfJoinResults[0]
        sender.sendMessage("§7  自连接结果: ${pair.get<Any>("user1")} + ${pair.get<Any>("user2")} in ${pair.get<Any>("world")}")

        // === 场景 2：子查询 JOIN（DSL 版） ===
        // subQuery("表名", "别名") { DSL } 复用 ActionSelect 构建子查询
        // 子查询的 SQL 和参数由框架自动生成和收集
        // 外层通过别名 "sub" 引用子查询的列
        val subqueryJoin = homeMapper.join {
            from("`player_home` AS `h`")
            innerJoin(
                subQuery("player_stats", "sub") {
                    rows("username", "SUM(kills) AS total_kills", "SUM(deaths) AS total_deaths")
                    groupBy("username")
                }
            ) {
                on("h.username" eq pre("sub.username"))
            }
            selectAs(
                "h.username" to "username",
                "h.world" to "world",
                "sub.total_kills" to "total_kills",
                "sub.total_deaths" to "total_deaths"
            )
            where { "h.world" eq "adv_world_1" }
        }.execute()

        // adv_world_1 有 alice 和 bob，各自有 stats 记录
        assert(subqueryJoin.size == 2, "子查询 JOIN 应返回 2 条, 实际: ${subqueryJoin.size}")
        for (row in subqueryJoin) {
            val name = row.get<Any>("username")
            val kills = row.get<Any>("total_kills")
            val deaths = row.get<Any>("total_deaths")
            sender.sendMessage("§7  子查询结果: $name kills=$kills deaths=$deaths")
        }

        // === 场景 3：子查询带参数绑定 ===
        // 子查询内部 where { "kills" gt 50 } 生成 ? 占位符
        // SubQuery 自动收集参数，框架按正确顺序绑定：
        //   子查询参数(50) → ON参数(无) → WHERE参数("adv_world_1")
        val paramResults = homeMapper.join {
            from("`player_home` AS `h`")
            innerJoin(
                subQuery("player_stats", "sub") {
                    rows("username", "SUM(kills) AS total_kills")
                    where { "kills" gt 50 }
                    groupBy("username")
                }
            ) {
                on("h.username" eq pre("sub.username"))
            }
            selectAs(
                "h.username" to "username",
                "sub.total_kills" to "total_kills"
            )
            where { "h.world" eq "adv_world_1" }
        }.execute()

        // alice(kills=100) 和 bob(kills=200) 满足 kills>50 且在 adv_world_1
        // carol(kills=50) 不满足 kills>50
        assert(paramResults.size == 2, "子查询参数绑定应返回 2 条, 实际: ${paramResults.size}")
        for (row in paramResults) {
            sender.sendMessage("§7  参数绑定结果: ${row.get<Any>("username")} total_kills=${row.get<Any>("total_kills")}")
        }

        // 清理
        listOf("adv_alice", "adv_bob", "adv_carol").forEach {
            homeMapper.deleteWhere { "username" eq it }
            statsMapper.deleteWhere { "username" eq it }
        }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
