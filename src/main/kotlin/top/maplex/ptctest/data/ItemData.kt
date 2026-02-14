package top.maplex.ptctest.data

/**
 * 物品数据 —— 集合 CustomType 测试用的元素类型
 *
 * 作为 List/Set/Map 的元素类型，配合集合 CustomType 实现扁平化存储。
 * 不是独立的数据库表，而是被序列化为字符串存入父表的单列中。
 *
 * 序列化格式：`"name:count"`（如 `"diamond_sword:1"`）
 *
 * @property name  物品名称
 * @property count 物品数量
 */
data class ItemData(val name: String, val count: Int)
