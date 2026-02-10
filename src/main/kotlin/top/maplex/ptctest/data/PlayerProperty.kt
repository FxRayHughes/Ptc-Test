package top.maplex.ptctest.data

import taboolib.expansion.Id

/**
 * 容器类型测试 —— Map 字段
 *
 * `properties` 字段为 `Map<String, String>` 类型，框架会自动创建子表 `player_property_properties` 来存储：
 *
 * ```sql
 * -- 主表
 * CREATE TABLE player_property (
 *     username TEXT PRIMARY KEY,
 *     level    INTEGER
 * )
 *
 * -- 自动生成的子表
 * CREATE TABLE player_property_properties (
 *     id              INTEGER PRIMARY KEY AUTOINCREMENT,
 *     parent_username TEXT,       -- 外键，引用主表 @Id
 *     map_key         TEXT,       -- Map 的 Key
 *     map_value       TEXT        -- Map 的 Value
 * )
 * ```
 *
 * Map 的特点：
 * - Key-Value 键值对存储
 * - Key 不可重复
 * - Value 允许 null
 */
data class PlayerProperty(
    @Id val username: String,
    var level: Int,
    val properties: Map<String, String>
)
