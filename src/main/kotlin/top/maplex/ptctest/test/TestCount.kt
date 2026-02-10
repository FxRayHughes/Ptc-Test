package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * 计数与存在性检查测试
 *
 * 本测试验证 `count` 和 `exists` 的各种重载形式，
 * 这些方法不返回完整记录，只返回统计信息，性能优于 findAll + size。
 *
 * ## 测试的 API 方法
 *
 * | 方法                      | 说明                                              |
 * |--------------------------|---------------------------------------------------|
 * | `count { filter }`       | 统计满足条件的记录数，返回 Long                      |
 * | `exists(id)`             | 通过 @Id 检查记录是否存在，返回 Boolean              |
 * | `exists { filter }`      | 通过自定义条件检查记录是否存在，返回 Boolean          |
 *
 * ## Filter DSL 语法
 *
 * Filter 是框架提供的条件构建 DSL，用于生成 WHERE 子句：
 *
 * ```kotlin
 * // 单条件
 * { "world" eq "count_world" }
 * // 生成: WHERE `world` = ?
 *
 * // 多条件（默认 AND 连接）
 * { "world" eq "count_world"; "active" eq true }
 * // 生成: WHERE `world` = ? AND `active` = ?
 * ```
 *
 * 支持的操作符：
 * - `eq`：等于（=）
 * - `gt`：大于（>）
 * - `lt`：小于（<）
 * - `like`：模糊匹配（LIKE）
 * - 更多操作符参见 Filter DSL 文档
 *
 * ## count vs exists 的选择
 *
 * - 只需要知道"有没有"→ 用 `exists`（内部可能用 LIMIT 1 优化）
 * - 需要知道"有多少条"→ 用 `count`
 * - 不要用 `findAll().size` 代替 `count`，后者不需要传输完整记录数据
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- count(filter)
 * SELECT COUNT(1) FROM `player_home` WHERE `world` = ?
 *
 * -- count(filter) 多条件
 * SELECT COUNT(1) FROM `player_home` WHERE `world` = ? AND `active` = ?
 *
 * -- exists(id)
 * SELECT COUNT(1) FROM `player_home` WHERE `username` = ?
 *
 * -- exists(filter)
 * SELECT COUNT(1) FROM `player_home` WHERE `world` = ? AND `active` = ?
 * ```
 */
object TestCount {

    fun run(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "count_1" }
        homeMapper.deleteWhere { "username" eq "count_2" }
        homeMapper.deleteWhere { "username" eq "count_3" }

        // 插入 3 条记录，共享 world="count_world" 用于分组过滤
        // count_1 和 count_3 的 active=true，count_2 的 active=false
        homeMapper.insert(PlayerHome("count_1", "lobby", "count_world", 0.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("count_2", "survival", "count_world", 0.0, 0.0, 0.0, false))
        homeMapper.insert(PlayerHome("count_3", "creative", "count_world", 0.0, 0.0, 0.0, true))

        // count(filter) —— 统计满足条件的记录数
        // 生成 SQL: SELECT COUNT(1) FROM `player_home` WHERE `world` = ?
        val total = homeMapper.count { "world" eq "count_world" }
        assert(total == 3L, "count 应为 3, 实际: $total")
        sender.sendMessage("§7  count 完成: $total")

        // count(filter) —— 多条件组合
        // Filter 中多个条件默认用 AND 连接
        // 生成 SQL: SELECT COUNT(1) FROM `player_home` WHERE `world` = ? AND `active` = ?
        val activeCount = homeMapper.count { "world" eq "count_world"; "active" eq true }
        assert(activeCount == 2L, "active count 应为 2, 实际: $activeCount")
        sender.sendMessage("§7  count(filter) 完成: $activeCount")

        // exists(id) —— 通过 @Id 检查存在性
        // 生成 SQL: SELECT COUNT(1) FROM `player_home` WHERE `username` = ?
        val ex1 = homeMapper.exists("count_1")
        assert(ex1, "exists(id) 应返回 true")
        sender.sendMessage("§7  exists(id) 完成: $ex1")

        // exists(filter) —— 通过自定义条件检查存在性
        val ex2 = homeMapper.exists { "world" eq "count_world"; "active" eq false }
        assert(ex2, "exists(filter) 应返回 true")
        // 验证不存在的条件返回 false
        val ex3 = homeMapper.exists { "world" eq "count_world_none" }
        assert(!ex3, "exists(filter) 对不存在的 world 应返回 false")
        sender.sendMessage("§7  exists(filter) 完成")

        homeMapper.deleteWhere { "username" eq "count_1" }
        homeMapper.deleteWhere { "username" eq "count_2" }
        homeMapper.deleteWhere { "username" eq "count_3" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
