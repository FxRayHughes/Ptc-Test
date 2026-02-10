package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length

/**
 * 容器类型测试 —— 混合容器字段
 *
 * 一个数据类可以同时包含 List、Set、Map 三种容器类型字段。
 * 框架为每个容器字段各创建一张子表：
 *
 * ```sql
 * -- 主表
 * CREATE TABLE player_profile (
 *     username     TEXT PRIMARY KEY,
 *     display_name TEXT
 * )
 *
 * -- List 子表（保留顺序）
 * CREATE TABLE player_profile_friends (
 *     id              INTEGER PRIMARY KEY AUTOINCREMENT,
 *     parent_username TEXT,
 *     value           TEXT,
 *     sort_order      INTEGER
 * )
 *
 * -- Set 子表
 * CREATE TABLE player_profile_badges (
 *     id              INTEGER PRIMARY KEY AUTOINCREMENT,
 *     parent_username TEXT,
 *     value           TEXT
 * )
 *
 * -- Map 子表
 * CREATE TABLE player_profile_settings (
 *     id              INTEGER PRIMARY KEY AUTOINCREMENT,
 *     parent_username TEXT,
 *     map_key         TEXT,
 *     map_value       TEXT
 * )
 * ```
 */
data class PlayerProfile(
    @Id val username: String,
    @Length(32) var displayName: String,
    val friends: List<String>,
    val badges: Set<String>,
    val settings: Map<String, String>
)
