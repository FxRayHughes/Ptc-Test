package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length

/**
 * 容器类型测试 —— List 字段
 *
 * `tags` 字段为 `List<String>` 类型，框架会自动创建子表 `player_tag_tags` 来存储：
 *
 * ```sql
 * -- 主表
 * CREATE TABLE player_tag (
 *     username TEXT PRIMARY KEY,
 *     label    TEXT
 * )
 *
 * -- 自动生成的子表（保留插入顺序）
 * CREATE TABLE player_tag_tags (
 *     id              INTEGER PRIMARY KEY AUTOINCREMENT,
 *     parent_username TEXT,       -- 外键，引用主表 @Id
 *     value           TEXT,       -- 元素值
 *     sort_order      INTEGER     -- 排序序号，维护 List 顺序
 * )
 * ```
 *
 * List 的特点：
 * - 保留插入顺序（通过 `sort_order` 列）
 * - 允许重复值
 * - 允许 null 值
 */
data class PlayerTag(
    @Id val username: String,
    @Length(32) var label: String,
    val tags: List<String>
)
