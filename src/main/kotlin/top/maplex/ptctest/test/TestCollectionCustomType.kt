package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.inventoryMapper
import top.maplex.ptctest.data.PlayerInventory
import top.maplex.ptctest.data.ItemData

/**
 * 集合 CustomType（扁平化集合）集成测试
 *
 * 本测试验证"集合 CustomType"机制：当 List/Set/Map 字段有匹配的集合级别 CustomType 时，
 * 框架不创建子表，而是将整个集合序列化为单列字符串存储。
 *
 * ## 集合 CustomType vs 普通子表
 *
 * | 特性           | 集合 CustomType（扁平化）     | 普通子表模式              |
 * |---------------|----------------------------|--------------------------|
 * | 存储方式       | 主表单列（TEXT）              | 独立子表                  |
 * | 序列化         | 整个集合 → 一个字符串         | 每个元素 → 一行            |
 * | 查询性能       | 单表查询，无 JOIN             | 需要额外查询子表           |
 * | 元素级查询     | 不支持（需反序列化后过滤）     | 支持 SQL WHERE 条件        |
 * | 适用场景       | 小集合、不需要元素级查询       | 大集合、需要元素级查询      |
 *
 * ## 集合 CustomType 定义
 *
 * ```kotlin
 * object ItemDataListType : CustomType {
 *     override val type: Class<*> = List::class.java       // 集合类型
 *     override val elementType: Class<*> = ItemData::class.java  // 元素类型（关键！）
 *     override fun serialize(value: Any): Any { ... }      // List<ItemData> → 字符串
 *     override fun deserialize(value: Any): Any { ... }    // 字符串 → List<ItemData>
 * }
 * ```
 *
 * `elementType` 不为 null 是集合 CustomType 的标志。框架通过
 * `(collectionType, elementType)` 二元组匹配，确定是否走扁平化路径。
 *
 * ## 建表结构
 *
 * ```sql
 * CREATE TABLE player_inventory (
 *     username TEXT PRIMARY KEY,
 *     label    TEXT,
 *     items    TEXT    -- 扁平化: "diamond_sword:1;golden_apple:64;ender_pearl:16"
 * )
 * -- 注意：没有 player_inventory_items 子表！
 * ```
 *
 * ## 测试覆盖
 *
 * 1. insert + findById —— 扁平化集合正确序列化和反序列化
 * 2. 空集合 —— 空 List 正常存取
 * 3. update —— 更新扁平化集合字段
 * 4. 批量查询 —— 多条记录各自拥有独立的扁平化集合
 * 5. 单元素集合 —— 只有一个元素的 List
 */
object TestCollectionCustomType {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        inventoryMapper.deleteWhere { "username" like "cct_%" }

        // ==================== 1. insert + findById ====================
        sender.sendMessage("§7  --- 扁平化集合 insert + findById ---")

        val inv1 = PlayerInventory(
            username = "cct_player1",
            label = "背包",
            items = listOf(
                ItemData("diamond_sword", 1),
                ItemData("golden_apple", 64),
                ItemData("ender_pearl", 16)
            )
        )
        inventoryMapper.insert(inv1)

        val found = inventoryMapper.findById("cct_player1")
        assert(found != null, "findById 应返回非空")
        assert(found!!.items.size == 3, "items 应有 3 个元素, 实际: ${found.items.size}")
        assert(found.items[0] == ItemData("diamond_sword", 1),
            "items[0] 应为 diamond_sword:1, 实际: ${found.items[0]}")
        assert(found.items[1] == ItemData("golden_apple", 64),
            "items[1] 应为 golden_apple:64, 实际: ${found.items[1]}")
        assert(found.items[2] == ItemData("ender_pearl", 16),
            "items[2] 应为 ender_pearl:16, 实际: ${found.items[2]}")
        sender.sendMessage("§7  insert+findById 完成: items=${found.items}")

        // ==================== 2. 空集合 ====================
        sender.sendMessage("§7  --- 空集合测试 ---")

        val empty = PlayerInventory("cct_empty", "空背包", emptyList())
        inventoryMapper.insert(empty)
        val foundEmpty = inventoryMapper.findById("cct_empty")
        assert(foundEmpty != null, "空集合 findById 应返回非空")
        assert(foundEmpty!!.items.isEmpty(), "空集合 items 应为空, 实际: ${foundEmpty.items}")
        sender.sendMessage("§7  空集合测试完成: items=${foundEmpty.items}")

        // ==================== 3. update ====================
        sender.sendMessage("§7  --- update 测试 ---")

        // 更新扁平化集合字段
        inventoryMapper.update(PlayerInventory(
            username = "cct_player1",
            label = "更新后的背包",
            items = listOf(
                ItemData("netherite_sword", 1),
                ItemData("totem_of_undying", 3)
            )
        ))

        val afterUpdate = inventoryMapper.findById("cct_player1")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.label == "更新后的背包",
            "update 后 label 应为 '更新后的背包', 实际: '${afterUpdate.label}'")
        assert(afterUpdate.items.size == 2,
            "update 后 items 应有 2 个元素, 实际: ${afterUpdate.items.size}")
        assert(afterUpdate.items[0] == ItemData("netherite_sword", 1),
            "update 后 items[0] 应为 netherite_sword:1, 实际: ${afterUpdate.items[0]}")
        sender.sendMessage("§7  update 完成: label=${afterUpdate.label}, items=${afterUpdate.items}")

        // ==================== 4. 批量查询 ====================
        sender.sendMessage("§7  --- 批量查询测试 ---")

        inventoryMapper.insert(PlayerInventory(
            "cct_player2", "玩家2背包",
            listOf(ItemData("bow", 1), ItemData("arrow", 64))
        ))

        val all = inventoryMapper.findAll { "username" like "cct_player%" }
        assert(all.size == 2, "findAll 应返回 2 条, 实际: ${all.size}")
        val p1 = all.first { it.username == "cct_player1" }
        val p2 = all.first { it.username == "cct_player2" }
        assert(p1.items.size == 2, "player1 items 应有 2 个, 实际: ${p1.items.size}")
        assert(p2.items.size == 2, "player2 items 应有 2 个, 实际: ${p2.items.size}")
        assert(p2.items[0] == ItemData("bow", 1), "player2 items[0] 应为 bow:1")
        sender.sendMessage("§7  批量查询完成: player1=${p1.items}, player2=${p2.items}")

        // ==================== 5. 单元素集合 ====================
        sender.sendMessage("§7  --- 单元素集合测试 ---")

        val single = PlayerInventory("cct_single", "单物品", listOf(ItemData("stick", 1)))
        inventoryMapper.insert(single)
        val foundSingle = inventoryMapper.findById("cct_single")
        assert(foundSingle != null, "单元素 findById 应返回非空")
        assert(foundSingle!!.items.size == 1, "单元素 items 应有 1 个, 实际: ${foundSingle.items.size}")
        assert(foundSingle.items[0] == ItemData("stick", 1), "单元素 items[0] 应为 stick:1")
        sender.sendMessage("§7  单元素集合完成: items=${foundSingle.items}")

        // 清理
        inventoryMapper.deleteWhere { "username" like "cct_%" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
