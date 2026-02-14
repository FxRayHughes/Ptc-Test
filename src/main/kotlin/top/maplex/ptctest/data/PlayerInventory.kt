package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length

/**
 * 扁平化集合测试 —— List<ItemData> 作为单列存储
 *
 * `items` 字段为 `List<ItemData>` 类型，由于注册了 [ItemDataListType]
 * （集合 CustomType，type=List, elementType=ItemData），框架判定为"扁平化集合"，
 * 不创建子表，而是将整个 List 序列化为单列字符串。
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE player_inventory (
 *     username TEXT PRIMARY KEY,
 *     label    TEXT,
 *     items    TEXT    -- 扁平化: "diamond_sword:1;golden_apple:64"
 * )
 * ```
 *
 * 对比普通子表模式（无集合 CustomType 时）：
 * ```sql
 * -- 主表
 * CREATE TABLE xxx (username TEXT PRIMARY KEY, label TEXT)
 * -- 子表
 * CREATE TABLE xxx_items (id INTEGER, parent_username TEXT, value TEXT, sort_order INTEGER)
 * ```
 *
 * @property username 玩家名，@Id 逻辑主键
 * @property label    标签
 * @property items    物品列表，扁平化为单列存储
 */
data class PlayerInventory(
    @Id val username: String,
    @Length(32) var label: String,
    var items: List<ItemData>
)
