package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.statsMapper
import top.maplex.ptctest.data.PlayerHome
import top.maplex.ptctest.data.PlayerStats

/**
 * 多表联查测试
 *
 * 本测试验证 `DataMapper.join {}` DSL，通过 INNER JOIN 关联多张表进行联合查询。
 *
 * ## 背景知识
 *
 * 当数据分散在多张表中时，需要 JOIN 操作将它们关联起来。
 * 例如：player_home 存储玩家位置，player_stats 存储玩家统计，
 * 通过 username 字段关联，一次查询获取完整信息。
 *
 * ## join DSL 结构
 *
 * ```kotlin
 * homeMapper.join {
 *     // 1. innerJoin<T> {} —— 指定要关联的表（通过泛型推断表名）
 *     innerJoin<PlayerStats> {
 *         // on() —— 指定连接条件
 *         // pre() —— 引用列名（而非占位符值）
 *         on("player_home.username" eq pre("player_stats.username"))
 *     }
 *     // 2. selectAs() —— 指定查询列及别名
 *     selectAs(
 *         "player_home.username" to "username",
 *         "player_stats.kills" to "kills"
 *     )
 *     // 3. where {} —— 过滤条件
 *     where { "player_home.username" eq "join_user" }
 * }.execute()  // 执行查询，返回 List<BundleMap>
 * ```
 *
 * ## 关键概念
 *
 * - **pre() 函数**：在 on() 条件中，`pre("player_stats.username")` 表示引用列名，
 *   而非将字符串作为参数值。不加 pre() 会生成 `= ?`（占位符），加了 pre() 生成 `= player_stats.username`（列引用）。
 *
 * - **selectAs()**：解决多表同名列冲突。两张表都有 username 列，
 *   通过 `"player_home.username" to "username"` 指定使用哪张表的列，并设置别名。
 *
 * - **BundleMap**：JOIN 查询的返回类型。不是数据类，而是一个 Map-like 容器。
 *   通过 `row.get<Any>("username")` 按别名获取值。
 *
 * - **execute()**：join DSL 构建完成后，调用 `execute()` 执行查询。
 *   返回 `List<BundleMap>`。
 *
 * ## 前提条件
 *
 * 两个 mapper（homeMapper 和 statsMapper）必须使用同一个数据库文件（test.db），
 * 这样两张表在同一个数据库中，才能直接 JOIN。
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * SELECT `player_home`.`username` AS `username`,
 *        `player_home`.`world` AS `world`,
 *        `player_stats`.`kills` AS `kills`,
 *        `player_stats`.`deaths` AS `deaths`
 * FROM `player_home`
 * INNER JOIN `player_stats`
 *   ON `player_home`.`username` = `player_stats`.`username`
 * WHERE `player_home`.`username` = ?
 * ```
 */
object TestJoin {

    fun run(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "join_user" }
        statsMapper.deleteWhere { "username" eq "join_user" }

        // 分别向两张表插入关联数据
        // 两条记录通过 username="join_user" 关联
        homeMapper.insert(PlayerHome("join_user", "lobby", "world", 1.0, 64.0, 1.0, true))
        statsMapper.insert(PlayerStats("join_user", 100, 50, 36000L))
        sender.sendMessage("§7  插入 home + stats 完成")

        // join DSL 构建联查
        val results = homeMapper.join {
            // innerJoin<PlayerStats> —— 通过泛型推断关联 player_stats 表
            innerJoin<PlayerStats> {
                // on() 指定连接条件
                // pre() 表示右侧是列引用，不是参数值
                // 生成: ON `player_home`.`username` = `player_stats`.`username`
                on("player_home.username" eq pre("player_stats.username"))
            }
            // selectAs —— 指定查询列及别名，避免同名列冲突
            // "表名.列名" to "别名"
            selectAs(
                "player_home.username" to "username",
                "player_home.world" to "world",
                "player_stats.kills" to "kills",
                "player_stats.deaths" to "deaths"
            )
            // where —— 过滤条件，使用 "表名.列名" 格式避免歧义
            where { "player_home.username" eq "join_user" }
        }.execute()  // execute() 执行查询，返回 List<BundleMap>

        assert(results.isNotEmpty(), "join 应返回非空结果")
        val row = results[0]
        // BundleMap.get<T>(key) —— 通过别名获取值
        // 需要显式指定类型参数，通常用 Any 然后 toString()
        assert(row.get<Any>("username").toString() == "join_user", "join username 应为 join_user")
        assert(row.get<Any>("kills").toString().toInt() == 100, "join kills 应为 100")
        sender.sendMessage("§7  join 完成, 结果: username=${row.get<Any>("username")}, kills=${row.get<Any>("kills")}, deaths=${row.get<Any>("deaths")}")

        homeMapper.deleteWhere { "username" eq "join_user" }
        statsMapper.deleteWhere { "username" eq "join_user" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
