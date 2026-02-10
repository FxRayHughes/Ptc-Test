package top.maplex.ptctest.data

import taboolib.expansion.Id

/**
 * 容器类型测试 —— Set 字段
 *
 * `permissions` 字段为 `Set<String>` 类型，框架会自动创建子表 `player_permission_permissions` 来存储：
 *
 * ```sql
 * -- 主表
 * CREATE TABLE player_permission (
 *     username TEXT PRIMARY KEY
 * )
 *
 * -- 自动生成的子表
 * CREATE TABLE player_permission_permissions (
 *     id              INTEGER PRIMARY KEY AUTOINCREMENT,
 *     parent_username TEXT,       -- 外键，引用主表 @Id
 *     value           TEXT        -- 元素值
 * )
 * ```
 *
 * Set 的特点：
 * - 不保证顺序
 * - 不允许重复值（语义层面，由框架保证）
 */
data class PlayerPermission(
    @Id val username: String,
    val permissions: Set<String>
)
