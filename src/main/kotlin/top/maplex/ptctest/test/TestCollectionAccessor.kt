package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.tagMapper
import top.maplex.ptctest.permMapper
import top.maplex.ptctest.propMapper
import top.maplex.ptctest.data.PlayerTag
import top.maplex.ptctest.data.PlayerPermission
import top.maplex.ptctest.data.PlayerProperty

/**
 * 容器类型 Accessor（代理集合）测试
 *
 * 本测试验证 DataMapper 新增的 `mapOf` / `listOf` / `setOf` API，
 * 这些方法返回实现了标准 `MutableMap` / `MutableList` / `MutableSet` 接口的数据库代理对象。
 *
 * ## 核心理念
 *
 * 用户拿到代理对象后，可以像操作普通 Kotlin 集合一样使用，
 * 所有操作自动转化为对子表的 SQL 操作，无需手写 SQL。
 *
 * ## API 概览
 *
 * ```kotlin
 * // 通过 @Id 定位父记录，获取 Map 代理
 * val props: MutableMap<String, String?> = propMapper.mapOf("player1", "properties")
 * props["lang"]              // → SELECT `map_value` FROM ... WHERE `map_key` = ?
 * props["lang"] = "en_US"    // → DELETE + INSERT
 * props.remove("lang")       // → DELETE WHERE `map_key` = ?
 * props.size                 // → SELECT COUNT(*)
 *
 * // 通过 @Id 定位父记录，获取 List 代理
 * val tags: MutableList<String?> = tagMapper.listOf("player1", "tags")
 * tags[0]                    // → SELECT LIMIT 1 OFFSET 0
 * tags.add("newTag")         // → INSERT (append)
 * tags.add(0, "first")       // → UPDATE sort_order + INSERT
 * tags.removeAt(1)           // → DELETE at index + UPDATE sort_order
 *
 * // 通过 @Id 定位父记录，获取 Set 代理
 * val perms: MutableSet<String?> = permMapper.setOf("player1", "permissions")
 * perms.add("admin")         // → INSERT IF NOT EXISTS
 * perms.contains("admin")    // → SELECT EXISTS
 * perms.remove("admin")      // → DELETE
 * ```
 *
 * ## 两种定位方式
 *
 * - **by ID**：`mapOf(id, fieldName)` —— 直接传入父记录的 @Id 值
 * - **by Filter**：`mapOf(fieldName) { "username" eq "xxx" }` —— 通过条件过滤父表记录
 *
 * ## 数据库持久化
 *
 * 代理对象的每次操作都**立即执行 SQL**，不需要额外调用 save/flush。
 * 通过代理写入的数据可以被 `findById` 等常规查询读到。
 *
 * ## 测试覆盖
 *
 * 1. MapAccessor —— put/get/containsKey/remove/size/clear/entries
 * 2. ListAccessor —— get/add/add(index)/set/removeAt/size/contains
 * 3. SetAccessor —— add/contains/remove/size/clear/iterator
 * 4. Filter 定位 —— 通过条件查找父记录再获取代理
 * 5. 持久化验证 —— 代理写入的数据通过 findById 读取
 */
object TestCollectionAccessor {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        propMapper.deleteWhere { "username" like "acc_%" }
        tagMapper.deleteWhere { "username" like "acc_%" }
        permMapper.deleteWhere { "username" like "acc_%" }

        // ==================== 1. Map Accessor ====================
        sender.sendMessage("§7  --- MapAccessor 测试 ---")

        // 准备数据：先插入一条带初始 Map 的记录
        propMapper.insert(PlayerProperty("acc_map1", 5, mapOf("lang" to "zh_CN", "theme" to "dark")))

        // 1.1 获取 Map 代理（by ID）
        //     返回 MutableMap<String, String?>，所有操作实时映射到 SQL
        val map = propMapper.mapOf("acc_map1", "properties")

        // 1.2 get —— SELECT `map_value` FROM ... WHERE `map_key` = ?
        assert(map["lang"] == "zh_CN", "Map.get: lang 应为 zh_CN, 实际: ${map["lang"]}")
        assert(map["theme"] == "dark", "Map.get: theme 应为 dark, 实际: ${map["theme"]}")
        sender.sendMessage("§7  Map.get 完成: lang=${map["lang"]}, theme=${map["theme"]}")

        // 1.3 size —— SELECT COUNT(*) FROM ...
        assert(map.size == 2, "Map.size: 应为 2, 实际: ${map.size}")
        sender.sendMessage("§7  Map.size 完成: ${map.size}")

        // 1.4 containsKey —— SELECT 1 FROM ... WHERE `map_key` = ? LIMIT 1
        assert(map.containsKey("lang"), "Map.containsKey: lang 应存在")
        assert(!map.containsKey("nonexistent"), "Map.containsKey: nonexistent 不应存在")
        sender.sendMessage("§7  Map.containsKey 完成")

        // 1.5 put (新增) —— DELETE + INSERT
        map["volume"] = "80"
        assert(map["volume"] == "80", "Map.put: volume 应为 80, 实际: ${map["volume"]}")
        assert(map.size == 3, "Map.put 后 size 应为 3, 实际: ${map.size}")
        sender.sendMessage("§7  Map.put (新增) 完成: volume=${map["volume"]}, size=${map.size}")

        // 1.6 put (覆盖) —— DELETE 旧值 + INSERT 新值
        map["lang"] = "en_US"
        assert(map["lang"] == "en_US", "Map.put: lang 应为 en_US, 实际: ${map["lang"]}")
        assert(map.size == 3, "Map.put 覆盖后 size 应保持 3, 实际: ${map.size}")
        sender.sendMessage("§7  Map.put (覆盖) 完成: lang=${map["lang"]}")

        // 1.7 remove —— DELETE FROM ... WHERE `map_key` = ?
        val removed = map.remove("theme")
        assert(removed == "dark", "Map.remove: 返回值应为 dark, 实际: $removed")
        assert(!map.containsKey("theme"), "Map.remove 后 theme 不应存在")
        assert(map.size == 2, "Map.remove 后 size 应为 2, 实际: ${map.size}")
        sender.sendMessage("§7  Map.remove 完成: removed=$removed, size=${map.size}")

        // 1.8 entries —— SELECT ALL → EntrySet
        val entries = map.entries.associate { it.key to it.value }
        assert(entries == mapOf("lang" to "en_US", "volume" to "80"),
            "Map.entries: 应为 {lang=en_US, volume=80}, 实际: $entries")
        sender.sendMessage("§7  Map.entries 完成: $entries")

        // 1.9 clear —— DELETE FROM ... WHERE parent_xxx = ?
        map.clear()
        assert(map.size == 0, "Map.clear 后 size 应为 0, 实际: ${map.size}")
        sender.sendMessage("§7  Map.clear 完成: size=${map.size}")

        // ==================== 2. List Accessor ====================
        sender.sendMessage("§7  --- ListAccessor 测试 ---")

        // 准备数据
        tagMapper.insert(PlayerTag("acc_list1", "Tester", listOf("alpha", "beta", "gamma")))

        // 2.1 获取 List 代理（by ID）
        val list = tagMapper.listOf("acc_list1", "tags")

        // 2.2 get —— SELECT `value` FROM ... ORDER BY `sort_order` LIMIT 1 OFFSET ?
        assert(list[0] == "alpha", "List.get(0): 应为 alpha, 实际: ${list[0]}")
        assert(list[1] == "beta", "List.get(1): 应为 beta, 实际: ${list[1]}")
        assert(list[2] == "gamma", "List.get(2): 应为 gamma, 实际: ${list[2]}")
        sender.sendMessage("§7  List.get 完成: [${list[0]}, ${list[1]}, ${list[2]}]")

        // 2.3 size —— SELECT COUNT(*)
        assert(list.size == 3, "List.size: 应为 3, 实际: ${list.size}")
        sender.sendMessage("§7  List.size 完成: ${list.size}")

        // 2.4 contains —— SELECT 1 FROM ... WHERE `value` = ? LIMIT 1
        assert(list.contains("beta"), "List.contains: beta 应存在")
        assert(!list.contains("delta"), "List.contains: delta 不应存在")
        sender.sendMessage("§7  List.contains 完成")

        // 2.5 add (尾部追加) —— INSERT INTO ... VALUES (?, ?, <size>)
        list.add("delta")
        assert(list.size == 4, "List.add 后 size 应为 4, 实际: ${list.size}")
        assert(list[3] == "delta", "List.add: 最后一个元素应为 delta, 实际: ${list[3]}")
        sender.sendMessage("§7  List.add (尾部) 完成: size=${list.size}, last=${list[3]}")

        // 2.6 add(index) (中间插入) —— UPDATE sort_order 位移 + INSERT
        //     在索引 1 处插入 "inserted"，原 beta 及后续元素后移
        list.add(1, "inserted")
        assert(list.size == 5, "List.add(1) 后 size 应为 5, 实际: ${list.size}")
        assert(list[0] == "alpha", "List[0] 应为 alpha, 实际: ${list[0]}")
        assert(list[1] == "inserted", "List[1] 应为 inserted, 实际: ${list[1]}")
        assert(list[2] == "beta", "List[2] 应为 beta, 实际: ${list[2]}")
        sender.sendMessage("§7  List.add(1) 完成: [${list[0]}, ${list[1]}, ${list[2]}, ...]")

        // 2.7 set —— UPDATE `value` WHERE sort_order = ?
        val oldVal = list.set(0, "ALPHA")
        assert(oldVal == "alpha", "List.set: 返回值应为 alpha, 实际: $oldVal")
        assert(list[0] == "ALPHA", "List.set: [0] 应为 ALPHA, 实际: ${list[0]}")
        sender.sendMessage("§7  List.set 完成: old=$oldVal, new=${list[0]}")

        // 2.8 removeAt —— DELETE + UPDATE sort_order 位移
        //     移除索引 1 处的 "inserted"
        val removedVal = list.removeAt(1)
        assert(removedVal == "inserted", "List.removeAt: 返回值应为 inserted, 实际: $removedVal")
        assert(list.size == 4, "List.removeAt 后 size 应为 4, 实际: ${list.size}")
        assert(list[0] == "ALPHA", "List[0] 应为 ALPHA, 实际: ${list[0]}")
        assert(list[1] == "beta", "List[1] 应为 beta, 实际: ${list[1]}")
        sender.sendMessage("§7  List.removeAt 完成: removed=$removedVal, size=${list.size}")

        // ==================== 3. Set Accessor ====================
        sender.sendMessage("§7  --- SetAccessor 测试 ---")

        // 准备数据
        permMapper.insert(PlayerPermission("acc_set1", setOf("build", "chat")))

        // 3.1 获取 Set 代理（by ID）
        val set = permMapper.setOf("acc_set1", "permissions")

        // 3.2 size / contains
        assert(set.size == 2, "Set.size: 应为 2, 实际: ${set.size}")
        assert(set.contains("build"), "Set.contains: build 应存在")
        assert(set.contains("chat"), "Set.contains: chat 应存在")
        assert(!set.contains("fly"), "Set.contains: fly 不应存在")
        sender.sendMessage("§7  Set.size+contains 完成: size=${set.size}")

        // 3.3 add (新元素) —— INSERT INTO ... VALUES (?, ?)
        assert(set.add("fly"), "Set.add: 新元素应返回 true")
        assert(set.size == 3, "Set.add 后 size 应为 3, 实际: ${set.size}")
        assert(set.contains("fly"), "Set.add 后 fly 应存在")
        sender.sendMessage("§7  Set.add (新元素) 完成: size=${set.size}")

        // 3.4 add (重复元素) —— 不插入，返回 false
        assert(!set.add("build"), "Set.add: 重复元素应返回 false")
        assert(set.size == 3, "Set.add 重复后 size 应保持 3, 实际: ${set.size}")
        sender.sendMessage("§7  Set.add (重复) 完成: size=${set.size}")

        // 3.5 remove —— DELETE FROM ... WHERE `value` = ?
        assert(set.remove("chat"), "Set.remove: chat 应返回 true")
        assert(set.size == 2, "Set.remove 后 size 应为 2, 实际: ${set.size}")
        assert(!set.contains("chat"), "Set.remove 后 chat 不应存在")
        sender.sendMessage("§7  Set.remove 完成: size=${set.size}")

        // 3.6 remove 不存在的元素
        assert(!set.remove("nonexistent"), "Set.remove: 不存在的元素应返回 false")
        sender.sendMessage("§7  Set.remove (不存在) 完成")

        // 3.7 iterator —— SELECT ALL → 快照遍历
        val values = set.toSet()
        assert(values == setOf("build", "fly"), "Set.iterator: 应为 {build, fly}, 实际: $values")
        sender.sendMessage("§7  Set.iterator 完成: $values")

        // 3.8 clear —— DELETE FROM ... WHERE parent_xxx = ?
        set.clear()
        assert(set.size == 0, "Set.clear 后 size 应为 0, 实际: ${set.size}")
        sender.sendMessage("§7  Set.clear 完成: size=${set.size}")

        // ==================== 4. Filter 定位 ====================
        sender.sendMessage("§7  --- Filter 定位测试 ---")

        // 4.1 通过 Filter 条件查找父记录，再获取代理
        //     等价于先执行 SELECT username FROM player_property WHERE level = 5 LIMIT 1，
        //     取到 acc_map1 后再绑定代理
        propMapper.insert(PlayerProperty("acc_filter1", 99, mapOf("test" to "value")))
        val filterMap = propMapper.mapOf("properties") { "level" eq 99 }
        assert(filterMap["test"] == "value", "Filter: 应读到 test=value, 实际: ${filterMap["test"]}")
        sender.sendMessage("§7  Filter 定位完成: test=${filterMap["test"]}")

        // ==================== 5. 持久化验证 ====================
        sender.sendMessage("§7  --- 持久化验证 ---")

        // 5.1 通过 Accessor 写入的数据，通过 findById 读取，验证数据库持久化
        propMapper.insert(PlayerProperty("acc_persist1", 1, emptyMap()))
        val persistMap = propMapper.mapOf("acc_persist1", "properties")
        persistMap["saved_key"] = "saved_val"
        persistMap["another_key"] = "another_val"

        // 通过常规 API 读取
        val persisted = propMapper.findById("acc_persist1")
        assert(persisted != null, "Persist: findById 应返回非空")
        assert(persisted!!.properties == mapOf("saved_key" to "saved_val", "another_key" to "another_val"),
            "Persist: properties 应为 {saved_key=saved_val, another_key=another_val}, 实际: ${persisted.properties}")
        sender.sendMessage("§7  持久化验证完成: ${persisted.properties}")

        // 清理
        propMapper.deleteWhere { "username" like "acc_%" }
        tagMapper.deleteWhere { "username" like "acc_%" }
        permMapper.deleteWhere { "username" like "acc_%" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
