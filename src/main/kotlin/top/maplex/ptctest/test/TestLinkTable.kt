package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.linkedHomeMapper
import top.maplex.ptctest.statsMapper
import top.maplex.ptctest.data.LinkedPlayerHome
import top.maplex.ptctest.data.PlayerStats

/**
 * @LinkTable 自动关联表测试
 *
 * 本测试综合验证 @LinkTable 注解的核心功能，包括级联保存、自动 LEFT JOIN、
 * 级联更新，以及与手动 join {} 的共存。
 *
 * ## @LinkTable 注解说明
 *
 * ```kotlin
 * data class LinkedPlayerHome(
 *     @Id val username: String,
 *     ...
 *     @LinkTable("statsUsername") val stats: PlayerStats?
 * )
 * ```
 *
 * `@LinkTable("statsUsername")` 的含义：
 * - 在 linked_player_home 表中创建外键列 `stats_username`
 * - 外键列的类型与 PlayerStats 的 @Id 字段（username: String）一致
 * - 查询时自动 LEFT JOIN player_stats 表
 * - 写入时级联保存 PlayerStats 对象
 *
 * ## 建表结构
 *
 * ```sql
 * CREATE TABLE linked_player_home (
 *     username        TEXT PRIMARY KEY,
 *     server_name     TEXT,
 *     world           TEXT,
 *     description     TEXT,
 *     stats_username  TEXT    -- @LinkTable 自动创建的外键列
 * )
 * ```
 *
 * ## 自动 LEFT JOIN 行为
 *
 * 当调用 `findById` / `findAll` 等类型化查询时，框架自动生成：
 * ```sql
 * SELECT `linked_player_home`.*,
 *        `__t0`.`username` AS `__link__stats_username__username`,
 *        `__t0`.`kills` AS `__link__stats_username__kills`,
 *        ...
 * FROM `linked_player_home`
 * LEFT JOIN `player_stats` AS `__t0`
 *   ON `linked_player_home`.`stats_username` = `__t0`.`username`
 * WHERE ...
 * ```
 *
 * LEFT JOIN 的特点：即使关联对象不存在（stats_username 为 NULL 或无匹配记录），
 * 主表记录仍然返回，关联字段为 null。
 *
 * ## 级联保存行为
 *
 * `insert(linkedHome)` 时：
 * 1. 检查 `stats` 字段是否非 null
 * 2. 如果非 null，先将 PlayerStats 对象插入/更新到 player_stats 表
 * 3. 将 stats.username 的值写入 linked_player_home.stats_username 外键列
 * 4. 插入 linked_player_home 主表记录
 *
 * ## 级联更新行为
 *
 * `update(linkedHome)` 时：
 * 1. 检查 `stats` 字段是否非 null
 * 2. 如果非 null，检查 player_stats 表中是否已存在该记录
 *    - 已存在 → UPDATE
 *    - 不存在 → INSERT
 * 3. 更新 linked_player_home.stats_username 外键列
 * 4. 更新 linked_player_home 主表记录
 *
 * ## 手动 join {} 与 @LinkTable 的关系
 *
 * @LinkTable 的自动 JOIN 只作用于类型化查询（findById/findAll 等），
 * 返回的是完整的数据类对象。
 *
 * 手动 join {} 是完全独立的代码路径，返回 BundleMap，
 * 两者互不干扰，可以在同一个 mapper 上同时使用。
 */
object TestLinkTable {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        linkedHomeMapper.deleteWhere { "username" eq "link_user_1" }
        linkedHomeMapper.deleteWhere { "username" eq "link_user_2" }
        linkedHomeMapper.deleteWhere { "username" eq "link_user_3" }
        statsMapper.deleteWhere { "username" eq "link_stats_1" }
        statsMapper.deleteWhere { "username" eq "link_stats_2" }
        statsMapper.deleteWhere { "username" eq "link_stats_3" }

        // 构造关联对象（不需要手动插入到 player_stats 表！）
        // 级联保存会自动处理
        val stats1 = PlayerStats("link_stats_1", 100, 10, 3600L)
        val stats2 = PlayerStats("link_stats_2", 200, 20, 7200L)

        // 1. insert —— 级联保存 + @ColumnType 长文本
        //    框架自动：先将 stats1 插入 player_stats 表，再插入主表
        //    description 使用 @ColumnType(TEXT)，可存储超过 VARCHAR 默认长度的长文本
        val longDesc = "A".repeat(500)
        val home1 = LinkedPlayerHome("link_user_1", "lobby", "world", longDesc, stats1)
        linkedHomeMapper.insert(home1)
        sender.sendMessage("§7  insert (级联保存 stats1 + 长文本) 完成")

        // 验证 stats1 被自动插入到 player_stats 表
        val autoSaved = statsMapper.findById("link_stats_1")
        assert(autoSaved != null, "stats1 应被级联插入到 player_stats")
        assert(autoSaved!!.kills == 100, "级联插入的 kills 应为 100, 实际: ${autoSaved.kills}")
        sender.sendMessage("§7  验证级联插入完成: stats1 已自动保存")

        // 2. insert —— 关联对象为 null，不触发级联
        //    stats_username 外键列为 NULL
        val home2 = LinkedPlayerHome("link_user_2", "survival", "world_nether", "短描述", null)
        linkedHomeMapper.insert(home2)
        sender.sendMessage("§7  insert (无关联) 完成")

        // 3. insert —— 级联保存 stats2
        val home3 = LinkedPlayerHome("link_user_3", "creative", "world_end", "创造模式", stats2)
        linkedHomeMapper.insert(home3)
        sender.sendMessage("§7  insert (级联保存 stats2) 完成")

        // 4. findById —— 自动 LEFT JOIN + @ColumnType 长文本验证
        //    框架自动生成 LEFT JOIN SQL，将 player_stats 的数据填充到 stats 字段
        val found1 = linkedHomeMapper.findById("link_user_1")
        assert(found1 != null, "findById link_user_1 应返回非空")
        assert(found1!!.stats != null, "link_user_1 的 stats 应非空")
        assert(found1.stats!!.username == "link_stats_1", "stats.username 应为 link_stats_1")
        assert(found1.stats!!.kills == 100, "stats.kills 应为 100")
        // @ColumnType(TEXT) 验证：500 字符长文本完整存取
        assert(found1.description == longDesc, "description 长度应为 500, 实际: ${found1.description.length}")
        sender.sendMessage("§7  findById 完成: stats=${found1.stats}, desc长度=${found1.description.length}")

        // 5. findById —— LEFT JOIN 关联对象为 null
        //    LEFT JOIN 的特点：即使无匹配记录，主表记录仍返回，关联字段为 null
        val found2 = linkedHomeMapper.findById("link_user_2")
        assert(found2 != null, "findById link_user_2 应返回非空")
        assert(found2!!.stats == null, "link_user_2 的 stats 应为 null")
        sender.sendMessage("§7  findById (无关联/LEFT JOIN) 完成: stats=${found2.stats}")

        // 6. findAll —— 查询所有记录，均自动 JOIN
        val allLinked = linkedHomeMapper.findAll { "world" like "world%" }
        assert(allLinked.size >= 3, "findAll 应返回至少 3 条, 实际: ${allLinked.size}")
        val withStats = allLinked.count { it.stats != null }
        val withoutStats = allLinked.count { it.stats == null }
        assert(withStats >= 2, "有关联的记录应至少 2 条, 实际: $withStats")
        assert(withoutStats >= 1, "无关联的记录应至少 1 条, 实际: $withoutStats")
        sender.sendMessage("§7  findAll 完成: 总数=${allLinked.size}, 有关联=$withStats, 无关联=$withoutStats")

        // 7. findByIds —— 批量查询也支持自动 JOIN
        val byIds = linkedHomeMapper.findByIds(listOf("link_user_1", "link_user_3"))
        assert(byIds.size == 2, "findByIds 应返回 2 条, 实际: ${byIds.size}")
        assert(byIds.all { it.stats != null }, "findByIds 结果应全部有关联对象")
        sender.sendMessage("§7  findByIds 完成: ${byIds.map { "${it.username}->${it.stats?.username}" }}")

        // 8. update —— 级联更新：切换关联对象到 stats2，同时更新 stats2 的字段
        //    框架自动：
        //    1. 检查 stats2 在 player_stats 表中是否存在 → 存在则 UPDATE
        //    2. 更新 linked_player_home.stats_username 外键列
        val modifiedStats2 = PlayerStats("link_stats_2", 999, 50, 9999L)
        val updated = found1.copy(stats = modifiedStats2)
        linkedHomeMapper.update(updated)
        sender.sendMessage("§7  update (级联更新 stats2) 完成")

        // 验证级联更新：stats2 的 kills 应被更新为 999
        val statsAfterUpdate = statsMapper.findById("link_stats_2")
        assert(statsAfterUpdate != null, "stats2 应存在")
        assert(statsAfterUpdate!!.kills == 999, "级联更新后 kills 应为 999, 实际: ${statsAfterUpdate.kills}")
        sender.sendMessage("§7  验证级联更新完成: stats2.kills=${statsAfterUpdate.kills}")

        // 验证主表 FK 也已切换
        val afterUpdate = linkedHomeMapper.findById("link_user_1")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.stats != null, "update 后 stats 应非空")
        assert(afterUpdate.stats!!.username == "link_stats_2", "update 后 stats.username 应为 link_stats_2")
        assert(afterUpdate.stats!!.kills == 999, "update 后 stats.kills 应为 999")
        sender.sendMessage("§7  update 关联对象完成: stats=${afterUpdate.stats}")

        // 9. 级联保存全新的关联对象（验证不存在时自动插入）
        val stats3 = PlayerStats("link_stats_3", 50, 5, 1800L)
        val updated2 = afterUpdate.copy(stats = stats3)
        linkedHomeMapper.update(updated2)
        val stats3Found = statsMapper.findById("link_stats_3")
        assert(stats3Found != null, "stats3 应被级联插入")
        assert(stats3Found!!.kills == 50, "stats3.kills 应为 50")
        sender.sendMessage("§7  级联插入新关联对象完成: stats3=${stats3Found}")

        // 10. 手动 join {} —— 在有 @LinkTable 的 mapper 上使用手动 JOIN
        //     @LinkTable 的自动 JOIN 只作用于类型化查询（findById/findAll 等）
        //     手动 join {} 是完全独立的代码路径，返回 BundleMap，两者互不干扰
        val joinResults = linkedHomeMapper.join {
            innerJoin<PlayerStats> {
                on("linked_player_home.stats_username" eq pre("player_stats.username"))
            }
            selectAs(
                "linked_player_home.username" to "home_user",
                "linked_player_home.world" to "home_world",
                "linked_player_home.description" to "home_desc",
                "player_stats.kills" to "stat_kills",
                "player_stats.deaths" to "stat_deaths"
            )
            where { "linked_player_home.username" eq "link_user_3" }
        }.execute()

        assert(joinResults.isNotEmpty(), "手动 join 应返回非空结果")
        val row = joinResults[0]
        assert(row.get<Any>("home_user").toString() == "link_user_3", "手动 join home_user 应为 link_user_3")
        assert(row.get<Any>("home_world").toString() == "world_end", "手动 join home_world 应为 world_end")
        assert(row.get<Any>("home_desc").toString() == "创造模式", "手动 join home_desc 应为 创造模式")
        val joinKills = row.get<Any>("stat_kills").toString().toInt()
        sender.sendMessage("§7  手动 join 完成: user=${row.get<Any>("home_user")}, world=${row.get<Any>("home_world")}, kills=$joinKills")

        // 清理
        linkedHomeMapper.deleteWhere { "username" eq "link_user_1" }
        linkedHomeMapper.deleteWhere { "username" eq "link_user_2" }
        linkedHomeMapper.deleteWhere { "username" eq "link_user_3" }
        statsMapper.deleteWhere { "username" eq "link_stats_1" }
        statsMapper.deleteWhere { "username" eq "link_stats_2" }
        statsMapper.deleteWhere { "username" eq "link_stats_3" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
