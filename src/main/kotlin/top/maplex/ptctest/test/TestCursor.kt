package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * 游标查询测试
 *
 * 本测试验证 `selectCursor`、`sortCursor`、`sortDescendingCursor` 方法，
 * 它们返回 [taboolib.expansion.Cursor] 对象，逐行从数据库读取数据，
 * 避免大量数据导致内存溢出。
 *
 * ## 核心特性
 *
 * - **逐行读取**：数据库不会一次性返回所有数据，每次迭代才从数据库取一条
 * - **必须在事务中使用**：游标查询需要保持数据库连接不断开
 * - **支持提前终止**：在 for 循环中可以随时 break，配合 `use {}` 自动释放资源
 *
 * ## 测试的 API 方法
 *
 * | 方法                                              | 说明                          |
 * |--------------------------------------------------|-------------------------------|
 * | `selectCursor { filter }`                        | 游标查询，无排序               |
 * | `sortCursor(column) { filter }`                  | 游标查询，正序排序             |
 * | `sortDescendingCursor(column) { filter }`        | 游标查询，倒序排序             |
 * | `cursor.forEach { ... }`                         | 便捷方法，自动关闭游标         |
 * | `cursor.use { for (item in it) { ... } }`        | 手动迭代，use 保证资源释放     |
 *
 * ## 使用场景
 *
 * - 数据查询并写入缓存（百万级数据逐条处理）
 * - Excel 导出（逐行写入，不占用大量内存）
 * - 数据迁移（逐条读取旧表写入新表）
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- selectCursor
 * SELECT * FROM `player_home` WHERE `world` = ?
 *
 * -- sortCursor
 * SELECT * FROM `player_home` WHERE `world` = ? ORDER BY `x` ASC
 *
 * -- sortDescendingCursor
 * SELECT * FROM `player_home` WHERE `world` = ? ORDER BY `x` DESC
 * ```
 */
object TestCursor {

    fun run(sender: ProxyCommandSender) {
        // 清理测试数据
        homeMapper.deleteWhere { "world" eq "cursor_world" }

        // 插入 5 条记录，x 值分别为 10, 30, 20, 50, 40
        homeMapper.insert(PlayerHome("cursor_1", "s1", "cursor_world", 10.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("cursor_2", "s2", "cursor_world", 30.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("cursor_3", "s3", "cursor_world", 20.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("cursor_4", "s4", "cursor_world", 50.0, 0.0, 0.0, false))
        homeMapper.insert(PlayerHome("cursor_5", "s5", "cursor_world", 40.0, 0.0, 0.0, true))

        // === selectCursor + forEach 便捷方法 ===
        // 在事务中使用游标逐条读取所有匹配记录
        homeMapper.transaction {
            val items = mutableListOf<String>()
            selectCursor { "world" eq "cursor_world" }.forEach { home ->
                items += home.username
            }
            assert(items.size == 5, "selectCursor 应读取 5 条, 实际: ${items.size}")
            sender.sendMessage("§7  selectCursor forEach 完成: ${items.size} 条")
        }

        // === sortCursor 正序游标 ===
        // 按 x 升序逐条读取：10, 20, 30, 40, 50
        homeMapper.transaction {
            val xValues = mutableListOf<Double>()
            sortCursor("x") { "world" eq "cursor_world" }.forEach { home ->
                xValues += home.x
            }
            assert(xValues.size == 5, "sortCursor 应读取 5 条, 实际: ${xValues.size}")
            assert(xValues[0] == 10.0, "sortCursor 第 1 条 x 应为 10.0, 实际: ${xValues[0]}")
            assert(xValues[4] == 50.0, "sortCursor 最后一条 x 应为 50.0, 实际: ${xValues[4]}")
            sender.sendMessage("§7  sortCursor 完成: $xValues")
        }

        // === sortDescendingCursor 倒序游标 ===
        // 按 x 降序逐条读取：50, 40, 30, 20, 10
        homeMapper.transaction {
            val xValues = mutableListOf<Double>()
            sortDescendingCursor("x") { "world" eq "cursor_world" }.forEach { home ->
                xValues += home.x
            }
            assert(xValues.size == 5, "sortDescendingCursor 应读取 5 条, 实际: ${xValues.size}")
            assert(xValues[0] == 50.0, "sortDescendingCursor 第 1 条 x 应为 50.0, 实际: ${xValues[0]}")
            assert(xValues[4] == 10.0, "sortDescendingCursor 最后一条 x 应为 10.0, 实际: ${xValues[4]}")
            sender.sendMessage("§7  sortDescendingCursor 完成: $xValues")
        }

        // === 提前终止（use + break）===
        // 只读取前 2 条就停止，验证游标可以提前关闭
        homeMapper.transaction {
            val items = mutableListOf<Double>()
            sortCursor("x") { "world" eq "cursor_world" }.use { cursor ->
                for (home in cursor) {
                    items += home.x
                    if (items.size >= 2) break
                }
            }
            assert(items.size == 2, "提前终止应读取 2 条, 实际: ${items.size}")
            assert(items[0] == 10.0, "提前终止第 1 条 x 应为 10.0, 实际: ${items[0]}")
            assert(items[1] == 20.0, "提前终止第 2 条 x 应为 20.0, 实际: ${items[1]}")
            sender.sendMessage("§7  提前终止完成: $items")
        }

        // === 事务外调用应抛出异常 ===
        var errorCaught = false
        try {
            homeMapper.selectCursor { "world" eq "cursor_world" }
        } catch (e: IllegalStateException) {
            errorCaught = true
        }
        assert(errorCaught, "事务外调用 selectCursor 应抛出异常")
        sender.sendMessage("§7  事务外异常检测完成")

        // 清理测试数据
        homeMapper.deleteWhere { "world" eq "cursor_world" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
