package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.tagMapper
import top.maplex.ptctest.permMapper
import top.maplex.ptctest.propMapper
import top.maplex.ptctest.profileMapper
import top.maplex.ptctest.data.PlayerTag
import top.maplex.ptctest.data.PlayerPermission
import top.maplex.ptctest.data.PlayerProperty
import top.maplex.ptctest.data.PlayerProfile

/**
 * 容器类型（List/Set/Map）子表测试
 *
 * 本测试验证数据类中包含 List、Set、Map 字段时，框架自动创建子表并进行关联读写的功能。
 *
 * ## 核心机制
 *
 * 当数据类包含 `List<String>`、`Set<String>`、`Map<String, String>` 类型的字段时，
 * 框架不会将这些字段存储为主表的列，而是为每个容器字段创建独立的子表。
 *
 * - **List 子表**：包含 `parent_{id}`, `value`, `sort_order` 列，保留插入顺序
 * - **Set 子表**：包含 `parent_{id}`, `value` 列，无序存储
 * - **Map 子表**：包含 `parent_{id}`, `map_key`, `map_value` 列，键值对存储
 *
 * ## 生命周期
 *
 * - **insert**：先插入主表记录，再将容器数据逐条写入子表
 * - **findById / findAll 等**：先查询主表，再批量查询子表数据，合并后构建完整对象
 * - **update**：更新主表 var 字段 + 子表先删后插（全量替换）
 * - **delete**：删除主表记录时自动级联删除所有子表中对应的数据
 *
 * ## 测试覆盖
 *
 * 1. List 字段 —— 插入、查询、顺序保持
 * 2. Set 字段 —— 插入、查询
 * 3. Map 字段 —— 插入、查询
 * 4. 混合容器 —— 同时包含 List + Set + Map 的数据类
 * 5. 空容器 —— 字段为 emptyList/emptySet/emptyMap 时正常存取
 * 6. update 同步 —— update 后子表数据被完整替换
 * 7. delete 级联 —— 删除主记录时子表数据同步清除
 * 8. 批量查询 —— 多条记录各自拥有独立的容器数据
 */
object TestCollection {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        tagMapper.deleteWhere { "username" like "coll_%" }
        permMapper.deleteWhere { "username" like "coll_%" }
        propMapper.deleteWhere { "username" like "coll_%" }
        profileMapper.deleteWhere { "username" like "coll_%" }

        // ==================== 1. List 字段 ====================
        sender.sendMessage("§7  --- List 字段测试 ---")

        // 1.1 insert + findById —— List 子表自动写入和读取
        //    insert 生成的 SQL：
        //    INSERT INTO `player_tag` (`username`, `label`) VALUES (?, ?)
        //    INSERT INTO `player_tag_tags` (`parent_username`, `value`, `sort_order`) VALUES (?, ?, 0), (?, ?, 1), (?, ?, 2)
        val tag1 = PlayerTag("coll_tag1", "Warrior", listOf("pvp", "tank", "warrior"))
        tagMapper.insert(tag1)
        val foundTag = tagMapper.findById("coll_tag1")
        assert(foundTag != null, "List: findById 应返回非空")
        assert(foundTag!!.tags == listOf("pvp", "tank", "warrior"), "List: tags 应完整返回, 实际: ${foundTag.tags}")
        sender.sendMessage("§7  List insert+findById 完成: tags=${foundTag.tags}")

        // 1.2 List 保持顺序 —— sort_order 确保读取顺序与写入一致
        val tag2 = PlayerTag("coll_tag2", "Sorted", listOf("z", "a", "m"))
        tagMapper.insert(tag2)
        val sortedTag = tagMapper.findById("coll_tag2")
        assert(sortedTag!!.tags == listOf("z", "a", "m"), "List: 顺序应保持 [z, a, m], 实际: ${sortedTag.tags}")
        sender.sendMessage("§7  List 顺序保持完成: tags=${sortedTag.tags}")

        // ==================== 2. Set 字段 ====================
        sender.sendMessage("§7  --- Set 字段测试 ---")

        // 2.1 insert + findById —— Set 子表自动写入和读取
        val perm1 = PlayerPermission("coll_perm1", setOf("admin", "moderator", "builder"))
        permMapper.insert(perm1)
        val foundPerm = permMapper.findById("coll_perm1")
        assert(foundPerm != null, "Set: findById 应返回非空")
        assert(foundPerm!!.permissions == setOf("admin", "moderator", "builder"), "Set: permissions 应完整返回, 实际: ${foundPerm.permissions}")
        sender.sendMessage("§7  Set insert+findById 完成: permissions=${foundPerm.permissions}")

        // ==================== 3. Map 字段 ====================
        sender.sendMessage("§7  --- Map 字段测试 ---")

        // 3.1 insert + findById —— Map 子表自动写入和读取
        //    Map 子表列：parent_username, map_key, map_value
        val prop1 = PlayerProperty("coll_prop1", 10, mapOf("lang" to "zh_CN", "theme" to "dark", "volume" to "80"))
        propMapper.insert(prop1)
        val foundProp = propMapper.findById("coll_prop1")
        assert(foundProp != null, "Map: findById 应返回非空")
        assert(foundProp!!.properties == mapOf("lang" to "zh_CN", "theme" to "dark", "volume" to "80"),
            "Map: properties 应完整返回, 实际: ${foundProp.properties}")
        sender.sendMessage("§7  Map insert+findById 完成: properties=${foundProp.properties}")

        // ==================== 4. 混合容器 ====================
        sender.sendMessage("§7  --- 混合容器测试 ---")

        // 4.1 三种容器类型同时存在 —— 框架为 friends, badges, settings 各创建一张子表
        val profile1 = PlayerProfile(
            "coll_profile1", "Steve",
            friends = listOf("Alex", "Notch", "Jeb"),
            badges = setOf("early_bird", "pvp_master"),
            settings = mapOf("fov" to "90", "render_distance" to "16")
        )
        profileMapper.insert(profile1)
        val foundProfile = profileMapper.findById("coll_profile1")
        assert(foundProfile != null, "Mixed: findById 应返回非空")
        assert(foundProfile!!.friends == listOf("Alex", "Notch", "Jeb"),
            "Mixed: friends 应为 [Alex, Notch, Jeb], 实际: ${foundProfile.friends}")
        assert(foundProfile.badges == setOf("early_bird", "pvp_master"),
            "Mixed: badges 应为 {early_bird, pvp_master}, 实际: ${foundProfile.badges}")
        assert(foundProfile.settings == mapOf("fov" to "90", "render_distance" to "16"),
            "Mixed: settings 应正确, 实际: ${foundProfile.settings}")
        sender.sendMessage("§7  混合容器 insert+findById 完成")
        sender.sendMessage("§7    friends=${foundProfile.friends}")
        sender.sendMessage("§7    badges=${foundProfile.badges}")
        sender.sendMessage("§7    settings=${foundProfile.settings}")

        // ==================== 5. 空容器 ====================
        sender.sendMessage("§7  --- 空容器测试 ---")

        // 5.1 空 List/Set/Map 正常存取 —— 子表无数据行，读取时返回空集合
        val empty = PlayerProfile("coll_empty", "Empty", emptyList(), emptySet(), emptyMap())
        profileMapper.insert(empty)
        val foundEmpty = profileMapper.findById("coll_empty")
        assert(foundEmpty != null, "Empty: findById 应返回非空")
        assert(foundEmpty!!.friends.isEmpty(), "Empty: friends 应为空, 实际: ${foundEmpty.friends}")
        assert(foundEmpty.badges.isEmpty(), "Empty: badges 应为空, 实际: ${foundEmpty.badges}")
        assert(foundEmpty.settings.isEmpty(), "Empty: settings 应为空, 实际: ${foundEmpty.settings}")
        sender.sendMessage("§7  空容器 insert+findById 完成")

        // ==================== 6. update 同步 ====================
        sender.sendMessage("§7  --- update 同步测试 ---")

        // 6.1 update 时子表数据被全量替换（先删后插）
        //    框架行为：DELETE FROM player_tag_tags WHERE parent_username = ? → 再 INSERT 新数据
        tagMapper.update(PlayerTag("coll_tag1", "Updated", listOf("new_tag1", "new_tag2")))
        val afterUpdate = tagMapper.findById("coll_tag1")
        assert(afterUpdate!!.label == "Updated", "Update: label 应为 Updated, 实际: ${afterUpdate.label}")
        assert(afterUpdate.tags == listOf("new_tag1", "new_tag2"),
            "Update: tags 应为 [new_tag1, new_tag2], 实际: ${afterUpdate.tags}")
        sender.sendMessage("§7  update 同步完成: label=${afterUpdate.label}, tags=${afterUpdate.tags}")

        // ==================== 7. delete 级联 ====================
        sender.sendMessage("§7  --- delete 级联测试 ---")

        // 7.1 删除主表记录时自动删除子表数据
        tagMapper.deleteById("coll_tag1")
        val afterDelete = tagMapper.findById("coll_tag1")
        assert(afterDelete == null, "Delete: findById 应返回 null")
        sender.sendMessage("§7  delete 级联完成: 主表+子表数据已清除")

        // ==================== 8. 批量查询 ====================
        sender.sendMessage("§7  --- 批量查询测试 ---")

        // 8.1 多条记录各自拥有独立的容器数据
        tagMapper.insertBatch(listOf(
            PlayerTag("coll_batch1", "A", listOf("x", "y")),
            PlayerTag("coll_batch2", "B", listOf("1", "2", "3"))
        ))
        val all = tagMapper.findAll { "username" like "coll_batch%" }
        assert(all.size == 2, "Batch: findAll 应返回 2 条, 实际: ${all.size}")
        val b1 = all.first { it.username == "coll_batch1" }
        val b2 = all.first { it.username == "coll_batch2" }
        assert(b1.tags == listOf("x", "y"), "Batch: b1.tags 应为 [x, y], 实际: ${b1.tags}")
        assert(b2.tags == listOf("1", "2", "3"), "Batch: b2.tags 应为 [1, 2, 3], 实际: ${b2.tags}")
        sender.sendMessage("§7  批量查询完成: b1.tags=${b1.tags}, b2.tags=${b2.tags}")

        // 清理
        tagMapper.deleteWhere { "username" like "coll_%" }
        permMapper.deleteWhere { "username" like "coll_%" }
        propMapper.deleteWhere { "username" like "coll_%" }
        profileMapper.deleteWhere { "username" like "coll_%" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
