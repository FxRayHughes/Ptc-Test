package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length
import taboolib.expansion.LinkTable

/**
 * 公会会长数据类 —— @LinkTable 嵌套关联的中间节点（第二层）
 *
 * 通过 @LinkTable("homeId") 关联 [GuildHome]，形成 GuildLeader → GuildHome 的关联链。
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE guild_leader (
 *     id      TEXT PRIMARY KEY,
 *     name    TEXT,
 *     home_id TEXT              -- @LinkTable 外键列，指向 guild_home.id
 * )
 * ```
 *
 * @property id   会长 ID，@Id 逻辑主键。
 * @property name 会长名称。
 * @property home 会长的家园。@LinkTable 自动 LEFT JOIN guild_home + 级联保存。
 */
data class GuildLeader(
    @Id @Length(32) val id: String,
    @Length(32) var name: String,
    @LinkTable("homeId") val home: GuildHome?
)
