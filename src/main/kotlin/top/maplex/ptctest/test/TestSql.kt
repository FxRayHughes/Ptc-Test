package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * 自定义 SQL 测试
 *
 * 本测试验证 DataMapper 提供的底层 SQL 操作方法，当框架的高层 API
 * 无法满足需求时，可以通过这些方法编写自定义 SQL。
 *
 * ## 高层 API vs 底层 API
 *
 * 高层 API（findById, insert, update 等）：
 * - 自动生成 SQL，类型安全，不易出错
 * - 适用于 90% 的常见场景
 *
 * 底层 API（query, rawQuery, rawUpdate, rawDelete）：
 * - 手动构建 SQL，灵活度高
 * - 适用于复杂查询、聚合函数、子查询等高级场景
 *
 * ## 测试的 API 方法
 *
 * | 方法                          | 说明                                                  |
 * |-----------------------------|-------------------------------------------------------|
 * | `query { DSL }`             | 自定义 SELECT，结果自动映射为 List<T>                    |
 * | `queryOne { DSL }`          | 自定义 SELECT 查询单条，结果自动映射为 T?                 |
 * | `rawQuery({ DSL }) { rs }` | 自定义 SELECT + 手动处理 ResultSet                      |
 * | `rawUpdate { DSL }`         | 自定义 UPDATE，返回受影响行数                            |
 * | `rawDelete { DSL }`         | 自定义 DELETE，返回受影响行数                            |
 *
 * ## ActionSelect DSL 语法
 *
 * `query` / `queryOne` / `rawQuery` 的 lambda 参数是 ActionSelect DSL：
 *
 * ```kotlin
 * homeMapper.query {
 *     rows("username", "world")     // 指定查询列（默认 *）
 *     where { "world" eq "xxx" }    // WHERE 条件
 *     limit(10)                     // LIMIT
 *     orderBy("x", true)           // ORDER BY x ASC
 * }
 * ```
 *
 * ## rawQuery 的两个 lambda
 *
 * `rawQuery` 接收两个 lambda：
 * 1. 第一个 lambda：构建 ActionSelect（与 query 相同）
 * 2. 第二个 lambda：处理 ResultSet，手动提取数据
 *
 * ```kotlin
 * val worlds = homeMapper.rawQuery({
 *     rows("world")
 *     where { "world" eq "xxx" }
 * }) { rs ->
 *     buildList { while (rs.next()) add(rs.getString("world")) }
 * }
 * ```
 *
 * ## rawUpdate DSL 语法
 *
 * ```kotlin
 * homeMapper.rawUpdate {
 *     set("active", true)                    // SET active = true
 *     where { "username" eq "sql_user_2" }   // WHERE username = 'sql_user_2'
 * }
 * // 生成: UPDATE `player_home` SET `active` = ? WHERE `username` = ?
 * ```
 *
 * ## rawDelete DSL 语法
 *
 * ```kotlin
 * homeMapper.rawDelete {
 *     where { "username" eq "sql_user_2" }
 * }
 * // 生成: DELETE FROM `player_home` WHERE `username` = ?
 * ```
 */
object TestSql {

    fun run(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "sql_user_1" }
        homeMapper.deleteWhere { "username" eq "sql_user_2" }

        homeMapper.insert(PlayerHome("sql_user_1", "lobby", "sql_world", 1.0, 2.0, 3.0, true))
        homeMapper.insert(PlayerHome("sql_user_2", "survival", "sql_world", 4.0, 5.0, 6.0, false))

        // query —— 自定义 SELECT，结果自动映射为 PlayerHome 列表
        // ActionSelect DSL：where {} 设置条件
        // 框架自动将 ResultSet 的每一行映射为 PlayerHome 对象
        val results = homeMapper.query {
            where { "world" eq "sql_world" }
        }
        assert(results.size == 2, "query 应返回 2 条, 实际: ${results.size}")
        sender.sendMessage("§7  query 完成, 数量: ${results.size}")

        // queryOne —— 自定义 SELECT 查询单条
        // 与 query 的区别：自动添加 LIMIT 1，返回 T? 而非 List<T>
        val one = homeMapper.queryOne {
            where { "username" eq "sql_user_1" }
        }
        assert(one != null, "queryOne 应返回非空")
        assert(one!!.serverName == "lobby", "queryOne serverName 应为 lobby")
        sender.sendMessage("§7  queryOne 完成: ${one.serverName}")

        // rawQuery —— 自定义 SELECT + 手动处理 ResultSet
        // 第一个 lambda：构建 ActionSelect（指定查询列和条件）
        // 第二个 lambda：接收 ResultSet，手动遍历提取数据
        // 适用于只需要部分列、或需要特殊处理的场景
        val worlds = homeMapper.rawQuery({
            rows("world")                          // 只查询 world 列
            where { "world" eq "sql_world" }
        }) { rs ->
            buildList {
                while (rs.next()) add(rs.getString("world"))
            }
        }
        assert(worlds.size == 2, "rawQuery 应返回 2 条, 实际: ${worlds.size}")
        sender.sendMessage("§7  rawQuery 完成: $worlds")

        // rawUpdate —— 自定义 UPDATE
        // set("列名", 值) 设置新值，where {} 设置条件
        // 返回受影响的行数（Int）
        val affected = homeMapper.rawUpdate {
            set("active", true)                    // SET active = true
            where { "username" eq "sql_user_2" }   // WHERE username = 'sql_user_2'
        }
        assert(affected == 1, "rawUpdate 应影响 1 行, 实际: $affected")
        // 验证更新结果
        val verified = homeMapper.findById("sql_user_2")
        assert(verified != null && verified.active, "rawUpdate 后 active 应为 true")
        sender.sendMessage("§7  rawUpdate 完成, 影响行数: $affected")

        // rawDelete —— 自定义 DELETE
        // where {} 设置条件，返回受影响的行数（Int）
        val deleted = homeMapper.rawDelete {
            where { "username" eq "sql_user_2" }
        }
        assert(deleted == 1, "rawDelete 应影响 1 行, 实际: $deleted")
        sender.sendMessage("§7  rawDelete 完成, 影响行数: $deleted")

        homeMapper.deleteWhere { "username" eq "sql_user_1" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
