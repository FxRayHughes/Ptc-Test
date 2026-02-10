package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.guildMapper
import top.maplex.ptctest.guildLeaderMapper
import top.maplex.ptctest.guildHomeMapper
import top.maplex.ptctest.data.Guild
import top.maplex.ptctest.data.GuildLeader
import top.maplex.ptctest.data.GuildHome

/**
 * @LinkTable 嵌套关联测试 —— Guild → GuildLeader → GuildHome 三层
 *
 * 本测试验证 @LinkTable 的无限嵌套能力。当关联对象自身也包含 @LinkTable 字段时，
 * 框架会递归处理，自动生成多层 LEFT JOIN 和级联保存。
 *
 * ## 三层嵌套结构
 *
 * ```
 * Guild (第一层 - 根节点)
 *   ├── id: String (@Id)
 *   ├── name: String
 *   ├── level: Int
 *   └── leader: GuildLeader? (@LinkTable)
 *         ├── id: String (@Id)
 *         ├── name: String
 *         └── home: GuildHome? (@LinkTable)
 *               ├── id: String (@Id)
 *               ├── world: String
 *               ├── x: Double
 *               ├── y: Double
 *               └── z: Double
 * ```
 *
 * ## 自动生成的 SQL（三层 LEFT JOIN）
 *
 * ```sql
 * SELECT `guild`.`id`, `guild`.`name`, `guild`.`level`,
 *        `__t0`.`id` AS `__link__leader_id__id`,
 *        `__t0`.`name` AS `__link__leader_id__name`,
 *        `__t1`.`id` AS `__link__leader_id____link__home_id__id`,
 *        `__t1`.`world` AS `__link__leader_id____link__home_id__world`,
 *        `__t1`.`x` AS `__link__leader_id____link__home_id__x`,
 *        `__t1`.`y` AS `__link__leader_id____link__home_id__y`,
 *        `__t1`.`z` AS `__link__leader_id____link__home_id__z`
 * FROM `guild`
 * LEFT JOIN `guild_leader` AS `__t0` ON `guild`.`leader_id` = `__t0`.`id`
 * LEFT JOIN `guild_home` AS `__t1` ON `__t0`.`home_id` = `__t1`.`id`
 * ```
 *
 * ## 列别名命名规则
 *
 * 框架使用 `__link__<外键列名>__<字段名>` 格式的别名来区分不同层级的列：
 * - 第一层：`__link__leader_id__name` → GuildLeader.name
 * - 第二层：`__link__leader_id____link__home_id__world` → GuildHome.world
 *
 * ## 级联保存顺序
 *
 * `guildMapper.insert(guild)` 时，框架按依赖顺序保存：
 * 1. 先保存 GuildHome（叶子节点，无依赖）
 * 2. 再保存 GuildLeader（依赖 GuildHome 的 id）
 * 3. 最后保存 Guild（依赖 GuildLeader 的 id）
 *
 * ## 测试覆盖的场景
 *
 * | 场景                    | 说明                                          |
 * |------------------------|-----------------------------------------------|
 * | 完整链                  | Guild → Leader → Home 三层都有值               |
 * | 中间层 null             | Guild.leader = null，Home 也自然为 null         |
 * | 叶子层 null             | Guild.leader ≠ null，但 leader.home = null     |
 * | findAll 混合嵌套         | 同时包含完整链、部分链、空链的记录                |
 * | 级联更新                | update 时自动更新嵌套关联对象                    |
 * | sort 排序查询            | 排序查询也支持嵌套 JOIN                         |
 * | findByIds 批量查询       | 批量查询也支持嵌套 JOIN                         |
 */
object TestNestedLink {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        guildMapper.deleteWhere { "id" eq "guild_1" }
        guildMapper.deleteWhere { "id" eq "guild_2" }
        guildMapper.deleteWhere { "id" eq "guild_3" }
        guildLeaderMapper.deleteWhere { "id" eq "leader_1" }
        guildLeaderMapper.deleteWhere { "id" eq "leader_2" }
        guildHomeMapper.deleteWhere { "id" eq "home_1" }

        // 构造三层嵌套对象
        // GuildHome（叶子） → GuildLeader（中间） → Guild（根）
        val home1 = GuildHome("home_1", "world", 100.0, 64.0, -200.0)
        val leader1 = GuildLeader("leader_1", "Alex", home1)
        val guild1 = Guild("guild_1", "龙之谷", 10, leader1)

        // 1. insert —— 级联保存三层对象
        //    框架自动按依赖顺序保存：GuildHome → GuildLeader → Guild
        guildMapper.insert(guild1)
        sender.sendMessage("§7  insert (三层级联保存) 完成")

        // 验证 GuildHome 被自动插入
        val autoHome = guildHomeMapper.findById("home_1")
        assert(autoHome != null, "home_1 应被级联插入到 guild_home")
        assert(autoHome!!.world == "world", "级联插入的 world 应为 world, 实际: ${autoHome.world}")
        assert(autoHome.x == 100.0, "级联插入的 x 应为 100.0, 实际: ${autoHome.x}")
        sender.sendMessage("§7  验证级联插入 GuildHome 完成")

        // 验证 GuildLeader 被自动插入
        val autoLeader = guildLeaderMapper.findById("leader_1")
        assert(autoLeader != null, "leader_1 应被级联插入到 guild_leader")
        assert(autoLeader!!.name == "Alex", "级联插入的 name 应为 Alex, 实际: ${autoLeader.name}")
        sender.sendMessage("§7  验证级联插入 GuildLeader 完成")

        // 2. findById —— 三层 LEFT JOIN 查询
        //    框架自动生成两个 LEFT JOIN，将三张表的数据组装为完整的嵌套对象
        val found1 = guildMapper.findById("guild_1")
        assert(found1 != null, "findById guild_1 应返回非空")
        assert(found1!!.name == "龙之谷", "name 应为 龙之谷")
        assert(found1.level == 10, "level 应为 10")
        assert(found1.leader != null, "leader 应非空")
        assert(found1.leader!!.id == "leader_1", "leader.id 应为 leader_1")
        assert(found1.leader!!.name == "Alex", "leader.name 应为 Alex")
        assert(found1.leader!!.home != null, "leader.home 应非空")
        assert(found1.leader!!.home!!.id == "home_1", "leader.home.id 应为 home_1")
        assert(found1.leader!!.home!!.world == "world", "leader.home.world 应为 world")
        assert(found1.leader!!.home!!.x == 100.0, "leader.home.x 应为 100.0")
        sender.sendMessage("§7  findById 三层 JOIN 完成: guild=${found1.name}, leader=${found1.leader!!.name}, home=${found1.leader!!.home!!.world}")

        // 3. insert —— 中间层为 null（Guild 无 leader）
        //    leader_id 外键列为 NULL，不触发任何级联
        val guild2 = Guild("guild_2", "孤狼帮", 5, null)
        guildMapper.insert(guild2)
        val found2 = guildMapper.findById("guild_2")
        assert(found2 != null, "findById guild_2 应返回非空")
        assert(found2!!.leader == null, "guild_2 的 leader 应为 null")
        sender.sendMessage("§7  中间层 null 测试完成: leader=${found2.leader}")

        // 4. insert —— 叶子层为 null（Leader 无 home）
        //    GuildLeader.home_id 外键列为 NULL
        //    Guild.leader_id 正常指向 leader_2
        val leader2 = GuildLeader("leader_2", "Steve", null)
        val guild3 = Guild("guild_3", "流浪者", 3, leader2)
        guildMapper.insert(guild3)
        val found3 = guildMapper.findById("guild_3")
        assert(found3 != null, "findById guild_3 应返回非空")
        assert(found3!!.leader != null, "guild_3 的 leader 应非空")
        assert(found3.leader!!.name == "Steve", "leader.name 应为 Steve")
        assert(found3.leader!!.home == null, "leader.home 应为 null")
        sender.sendMessage("§7  叶子层 null 测试完成: leader=${found3.leader!!.name}, home=${found3.leader!!.home}")

        // 5. findAll —— 混合嵌套查询
        //    同时包含：完整链(guild_1)、空链(guild_2)、部分链(guild_3)
        val all = guildMapper.findAll()
        assert(all.size >= 3, "findAll 应返回至少 3 条, 实际: ${all.size}")
        val fullChain = all.count { it.leader?.home != null }
        val partialChain = all.count { it.leader != null && it.leader!!.home == null }
        val noChain = all.count { it.leader == null }
        assert(fullChain >= 1, "完整链(leader+home)应至少 1 条, 实际: $fullChain")
        assert(partialChain >= 1, "部分链(leader无home)应至少 1 条, 实际: $partialChain")
        assert(noChain >= 1, "空链(无leader)应至少 1 条, 实际: $noChain")
        sender.sendMessage("§7  findAll 完成: 总数=${all.size}, 完整链=$fullChain, 部分链=$partialChain, 空链=$noChain")

        // 6. update —— 级联更新嵌套对象
        //    修改三层的数据：Guild.level, GuildLeader.name, GuildHome.world/x/y/z
        //    框架自动递归更新所有层级
        val updatedHome = home1.copy(world = "world_nether", x = 0.0, y = 32.0, z = 0.0)
        val updatedLeader = leader1.copy(name = "Alex_v2", home = updatedHome)
        val updatedGuild = found1.copy(level = 20, leader = updatedLeader)
        guildMapper.update(updatedGuild)
        sender.sendMessage("§7  update (级联更新三层) 完成")

        // 验证级联更新
        val afterUpdate = guildMapper.findById("guild_1")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.level == 20, "update 后 level 应为 20, 实际: ${afterUpdate.level}")
        assert(afterUpdate.leader!!.name == "Alex_v2", "update 后 leader.name 应为 Alex_v2, 实际: ${afterUpdate.leader!!.name}")
        assert(afterUpdate.leader!!.home!!.world == "world_nether", "update 后 home.world 应为 world_nether, 实际: ${afterUpdate.leader!!.home!!.world}")
        assert(afterUpdate.leader!!.home!!.x == 0.0, "update 后 home.x 应为 0.0, 实际: ${afterUpdate.leader!!.home!!.x}")
        sender.sendMessage("§7  级联更新验证完成: level=${afterUpdate.level}, leader=${afterUpdate.leader!!.name}, home.world=${afterUpdate.leader!!.home!!.world}")

        // 7. sort —— 排序查询也支持嵌套 JOIN
        //    框架在排序查询中也会自动生成多层 LEFT JOIN
        val sorted = guildMapper.sortDescending("level", 3)
        assert(sorted.size == 3, "sortDescending 应返回 3 条, 实际: ${sorted.size}")
        assert(sorted[0].level >= sorted[1].level, "排序应降序: ${sorted[0].level} >= ${sorted[1].level}")
        assert(sorted[1].level >= sorted[2].level, "排序应降序: ${sorted[1].level} >= ${sorted[2].level}")
        // 第一条应是 guild_1（level=20），且嵌套对象完整
        assert(sorted[0].leader?.home != null, "排序第一条应有完整嵌套链")
        sender.sendMessage("§7  sortDescending 完成: ${sorted.map { "${it.id}(lv${it.level})" }}")

        // 8. findByIds —— 批量查询也支持嵌套 JOIN
        val byIds = guildMapper.findByIds(listOf("guild_1", "guild_3"))
        assert(byIds.size == 2, "findByIds 应返回 2 条, 实际: ${byIds.size}")
        val g1 = byIds.find { it.id == "guild_1" }
        val g3 = byIds.find { it.id == "guild_3" }
        assert(g1?.leader?.home != null, "guild_1 应有完整嵌套链")
        assert(g3?.leader != null && g3.leader!!.home == null, "guild_3 应有 leader 但无 home")
        sender.sendMessage("§7  findByIds 完成: ${byIds.map { "${it.id}→${it.leader?.name}→${it.leader?.home?.world}" }}")

        // 清理
        guildMapper.deleteWhere { "id" eq "guild_1" }
        guildMapper.deleteWhere { "id" eq "guild_2" }
        guildMapper.deleteWhere { "id" eq "guild_3" }
        guildLeaderMapper.deleteWhere { "id" eq "leader_1" }
        guildLeaderMapper.deleteWhere { "id" eq "leader_2" }
        guildHomeMapper.deleteWhere { "id" eq "home_1" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
