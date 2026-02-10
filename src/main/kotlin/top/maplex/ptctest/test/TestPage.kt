package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * 分页查询测试
 *
 * 本测试验证 `findPage`、`sortPage`、`sortDescendingPage` 方法，
 * 它们返回包含总数信息的 [taboolib.expansion.Page] 对象，支持分页浏览数据。
 *
 * ## 测试的 API 方法
 *
 * | 方法                                                    | 说明                                         |
 * |--------------------------------------------------------|----------------------------------------------|
 * | `findPage(page, size) { filter }`                      | 分页获取数据，返回 Page 对象                    |
 * | `sortPage(column, page, size) { filter }`              | 分页正序排序，返回 Page 对象                    |
 * | `sortDescendingPage(column, page, size) { filter }`    | 分页倒序排序，返回 Page 对象                    |
 *
 * ## Page 对象属性
 *
 * | 属性          | 说明                          |
 * |--------------|-------------------------------|
 * | `content`    | 当前页数据列表                  |
 * | `page`       | 当前页码（从 1 开始）            |
 * | `size`       | 每页大小                       |
 * | `total`      | 总记录数                       |
 * | `totalPages` | 总页数                         |
 * | `hasNext`    | 是否有下一页                    |
 * | `hasPrevious`| 是否有上一页                    |
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- findPage（第 2 页，每页 3 条）
 * SELECT * FROM `player_home` WHERE `world` = ? LIMIT 3 OFFSET 3
 *
 * -- sortPage（按 x 升序，第 1 页，每页 3 条）
 * SELECT * FROM `player_home` WHERE `world` = ? ORDER BY `x` ASC LIMIT 3 OFFSET 0
 *
 * -- count（获取总数）
 * SELECT COUNT(*) FROM `player_home` WHERE `world` = ?
 * ```
 */
object TestPage {

    fun run(sender: ProxyCommandSender) {
        // 清理测试数据
        for (i in 1..7) {
            homeMapper.deleteWhere { "username" eq "page_$i" }
        }

        // 插入 7 条记录，x 值分别为 10, 30, 20, 70, 50, 40, 60
        homeMapper.insert(PlayerHome("page_1", "s1", "page_world", 10.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("page_2", "s2", "page_world", 30.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("page_3", "s3", "page_world", 20.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("page_4", "s4", "page_world", 70.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("page_5", "s5", "page_world", 50.0, 0.0, 0.0, false))
        homeMapper.insert(PlayerHome("page_6", "s6", "page_world", 40.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("page_7", "s7", "page_world", 60.0, 0.0, 0.0, false))

        // === findPage 基础分页 ===
        // 第 1 页，每页 3 条
        val page1 = homeMapper.findPage(1, 3) { "world" eq "page_world" }
        assert(page1.content.size == 3, "findPage 第 1 页应返回 3 条, 实际: ${page1.content.size}")
        assert(page1.page == 1, "page 应为 1, 实际: ${page1.page}")
        assert(page1.size == 3, "size 应为 3, 实际: ${page1.size}")
        assert(page1.total == 7L, "total 应为 7, 实际: ${page1.total}")
        assert(page1.totalPages == 3, "totalPages 应为 3, 实际: ${page1.totalPages}")
        assert(page1.hasNext, "第 1 页应有下一页")
        assert(!page1.hasPrevious, "第 1 页不应有上一页")
        sender.sendMessage("§7  findPage 第 1 页完成: ${page1.content.size} 条, 总计 ${page1.total}")

        // 第 2 页
        val page2 = homeMapper.findPage(2, 3) { "world" eq "page_world" }
        assert(page2.content.size == 3, "findPage 第 2 页应返回 3 条, 实际: ${page2.content.size}")
        assert(page2.hasNext, "第 2 页应有下一页")
        assert(page2.hasPrevious, "第 2 页应有上一页")
        sender.sendMessage("§7  findPage 第 2 页完成: ${page2.content.size} 条")

        // 第 3 页（最后一页，只有 1 条）
        val page3 = homeMapper.findPage(3, 3) { "world" eq "page_world" }
        assert(page3.content.size == 1, "findPage 第 3 页应返回 1 条, 实际: ${page3.content.size}")
        assert(!page3.hasNext, "第 3 页不应有下一页")
        assert(page3.hasPrevious, "第 3 页应有上一页")
        sender.sendMessage("§7  findPage 第 3 页完成: ${page3.content.size} 条")

        // === sortPage 分页正序排序 ===
        // 按 x 升序排列后：10, 20, 30, 40, 50, 60, 70
        // 第 1 页（每页 3 条）：10, 20, 30
        val sortAsc1 = homeMapper.sortPage("x", 1, 3) { "world" eq "page_world" }
        assert(sortAsc1.content.size == 3, "sortPage 第 1 页应返回 3 条, 实际: ${sortAsc1.content.size}")
        assert(sortAsc1.content[0].x == 10.0, "sortPage 第 1 页第 1 条 x 应为 10.0, 实际: ${sortAsc1.content[0].x}")
        assert(sortAsc1.content[1].x == 20.0, "sortPage 第 1 页第 2 条 x 应为 20.0, 实际: ${sortAsc1.content[1].x}")
        assert(sortAsc1.content[2].x == 30.0, "sortPage 第 1 页第 3 条 x 应为 30.0, 实际: ${sortAsc1.content[2].x}")
        assert(sortAsc1.total == 7L, "sortPage total 应为 7, 实际: ${sortAsc1.total}")
        sender.sendMessage("§7  sortPage 第 1 页完成: ${sortAsc1.content.map { it.x }}")

        // 第 2 页：40, 50, 60
        val sortAsc2 = homeMapper.sortPage("x", 2, 3) { "world" eq "page_world" }
        assert(sortAsc2.content.size == 3, "sortPage 第 2 页应返回 3 条, 实际: ${sortAsc2.content.size}")
        assert(sortAsc2.content[0].x == 40.0, "sortPage 第 2 页第 1 条 x 应为 40.0, 实际: ${sortAsc2.content[0].x}")
        assert(sortAsc2.content[1].x == 50.0, "sortPage 第 2 页第 2 条 x 应为 50.0, 实际: ${sortAsc2.content[1].x}")
        assert(sortAsc2.content[2].x == 60.0, "sortPage 第 2 页第 3 条 x 应为 60.0, 实际: ${sortAsc2.content[2].x}")
        sender.sendMessage("§7  sortPage 第 2 页完成: ${sortAsc2.content.map { it.x }}")

        // 第 3 页：70
        val sortAsc3 = homeMapper.sortPage("x", 3, 3) { "world" eq "page_world" }
        assert(sortAsc3.content.size == 1, "sortPage 第 3 页应返回 1 条, 实际: ${sortAsc3.content.size}")
        assert(sortAsc3.content[0].x == 70.0, "sortPage 第 3 页第 1 条 x 应为 70.0, 实际: ${sortAsc3.content[0].x}")
        sender.sendMessage("§7  sortPage 第 3 页完成: ${sortAsc3.content.map { it.x }}")

        // === sortDescendingPage 分页倒序排序 ===
        // 按 x 降序排列后：70, 60, 50, 40, 30, 20, 10
        // 第 1 页（每页 3 条）：70, 60, 50
        val sortDesc1 = homeMapper.sortDescendingPage("x", 1, 3) { "world" eq "page_world" }
        assert(sortDesc1.content.size == 3, "sortDescendingPage 第 1 页应返回 3 条, 实际: ${sortDesc1.content.size}")
        assert(sortDesc1.content[0].x == 70.0, "sortDescendingPage 第 1 条 x 应为 70.0, 实际: ${sortDesc1.content[0].x}")
        assert(sortDesc1.content[1].x == 60.0, "sortDescendingPage 第 2 条 x 应为 60.0, 实际: ${sortDesc1.content[1].x}")
        assert(sortDesc1.content[2].x == 50.0, "sortDescendingPage 第 3 条 x 应为 50.0, 实际: ${sortDesc1.content[2].x}")
        sender.sendMessage("§7  sortDescendingPage 第 1 页完成: ${sortDesc1.content.map { it.x }}")

        // 第 3 页（最后一页）：10
        val sortDesc3 = homeMapper.sortDescendingPage("x", 3, 3) { "world" eq "page_world" }
        assert(sortDesc3.content.size == 1, "sortDescendingPage 第 3 页应返回 1 条, 实际: ${sortDesc3.content.size}")
        assert(sortDesc3.content[0].x == 10.0, "sortDescendingPage 最后一条 x 应为 10.0, 实际: ${sortDesc3.content[0].x}")
        assert(!sortDesc3.hasNext, "最后一页不应有下一页")
        sender.sendMessage("§7  sortDescendingPage 第 3 页完成: ${sortDesc3.content.map { it.x }}")

        // === 带 filter 的分页 ===
        // 只查 active=true 的记录（5 条：10, 30, 20, 70, 40）
        val activePage = homeMapper.sortPage("x", 1, 3) { "world" eq "page_world"; "active" eq true }
        assert(activePage.total == 5L, "active=true 总数应为 5, 实际: ${activePage.total}")
        assert(activePage.totalPages == 2, "active=true 总页数应为 2, 实际: ${activePage.totalPages}")
        assert(activePage.content.size == 3, "active=true 第 1 页应返回 3 条, 实际: ${activePage.content.size}")
        // 升序排列后：10, 20, 30
        assert(activePage.content[0].x == 10.0, "active 第 1 条 x 应为 10.0, 实际: ${activePage.content[0].x}")
        assert(activePage.content[1].x == 20.0, "active 第 2 条 x 应为 20.0, 实际: ${activePage.content[1].x}")
        assert(activePage.content[2].x == 30.0, "active 第 3 条 x 应为 30.0, 实际: ${activePage.content[2].x}")
        sender.sendMessage("§7  带 filter 分页完成: ${activePage.content.map { it.x }}, 总计 ${activePage.total}")

        // === 超出范围的页码 ===
        val emptyPage = homeMapper.findPage(100, 3) { "world" eq "page_world" }
        assert(emptyPage.content.isEmpty(), "超出范围的页码应返回空列表, 实际: ${emptyPage.content.size}")
        assert(emptyPage.total == 7L, "超出范围时 total 仍应为 7, 实际: ${emptyPage.total}")
        assert(!emptyPage.hasNext, "超出范围时不应有下一页")
        sender.sendMessage("§7  超出范围页码完成: 空列表, total=${emptyPage.total}")

        // 清理测试数据
        homeMapper.deleteWhere { "world" eq "page_world" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
