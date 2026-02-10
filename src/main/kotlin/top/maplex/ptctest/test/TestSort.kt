package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * 排序查询测试
 *
 * 本测试验证 `sort` 和 `sortDescending` 方法，它们在查询时按指定列排序并限制返回数量。
 *
 * ## 测试的 API 方法
 *
 * | 方法                                        | 说明                                    |
 * |--------------------------------------------|-----------------------------------------|
 * | `sort(column, limit) { filter }`           | 按指定列升序排列，取前 limit 条            |
 * | `sortDescending(column, limit) { filter }` | 按指定列降序排列，取前 limit 条            |
 *
 * ## 参数说明
 *
 * - `column`：排序列名（数据库列名，如 "x"）
 * - `limit`：返回的最大记录数
 * - `filter`：可选的过滤条件，限制参与排序的记录范围
 *
 * ## 使用场景
 *
 * - 排行榜：`sortDescending("kills", 10)` 获取击杀数前 10 名
 * - 最近记录：`sortDescending("created_at", 5)` 获取最近 5 条记录
 * - 最小值查询：`sort("price", 1)` 获取价格最低的记录
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- sort（升序）
 * SELECT * FROM `player_home` WHERE `world` = ? ORDER BY `x` ASC LIMIT 3
 *
 * -- sortDescending（降序）
 * SELECT * FROM `player_home` WHERE `world` = ? ORDER BY `x` DESC LIMIT 3
 * ```
 */
object TestSort {

    fun run(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "sort_1" }
        homeMapper.deleteWhere { "username" eq "sort_2" }
        homeMapper.deleteWhere { "username" eq "sort_3" }
        homeMapper.deleteWhere { "username" eq "sort_4" }
        homeMapper.deleteWhere { "username" eq "sort_5" }

        // 插入 5 条记录，x 值故意乱序：10, 30, 20, 50, 40
        // 用于验证排序是否正确
        homeMapper.insert(PlayerHome("sort_1", "s1", "sort_world", 10.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("sort_2", "s2", "sort_world", 30.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("sort_3", "s3", "sort_world", 20.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("sort_4", "s4", "sort_world", 50.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("sort_5", "s5", "sort_world", 40.0, 0.0, 0.0, true))

        // sort("x", 3) —— 按 x 升序取前 3 条
        // 5 条记录的 x 值排序后：10, 20, 30, 40, 50
        // 取前 3 条：10, 20, 30
        val asc = homeMapper.sort("x", 3) { "world" eq "sort_world" }
        assert(asc.size == 3, "sort 应返回 3 条, 实际: ${asc.size}")
        assert(asc[0].x == 10.0, "sort 第一条 x 应为 10.0, 实际: ${asc[0].x}")
        assert(asc[1].x == 20.0, "sort 第二条 x 应为 20.0, 实际: ${asc[1].x}")
        assert(asc[2].x == 30.0, "sort 第三条 x 应为 30.0, 实际: ${asc[2].x}")
        sender.sendMessage("§7  sort 完成: ${asc.map { it.x }}")

        // sortDescending("x", 3) —— 按 x 降序取前 3 条
        // 降序排列后：50, 40, 30, 20, 10
        // 取前 3 条：50, 40, 30
        val desc = homeMapper.sortDescending("x", 3) { "world" eq "sort_world" }
        assert(desc.size == 3, "sortDescending 应返回 3 条, 实际: ${desc.size}")
        assert(desc[0].x == 50.0, "sortDescending 第一条 x 应为 50.0, 实际: ${desc[0].x}")
        assert(desc[1].x == 40.0, "sortDescending 第二条 x 应为 40.0, 实际: ${desc[1].x}")
        assert(desc[2].x == 30.0, "sortDescending 第三条 x 应为 30.0, 实际: ${desc[2].x}")
        sender.sendMessage("§7  sortDescending 完成: ${desc.map { it.x }}")

        homeMapper.deleteWhere { "world" eq "sort_world" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
