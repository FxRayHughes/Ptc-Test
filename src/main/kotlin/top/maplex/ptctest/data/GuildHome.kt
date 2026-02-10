package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length
import taboolib.expansion.LinkTable

/**
 * 公会家园数据类 —— @LinkTable 嵌套关联的叶子节点（第三层）
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE guild_home (
 *     id    TEXT PRIMARY KEY,
 *     world TEXT,
 *     x     REAL,
 *     y     REAL,
 *     z     REAL
 * )
 * ```
 *
 * @property id    家园 ID，@Id 逻辑主键。
 * @property world 世界名。
 * @property x     X 坐标。
 * @property y     Y 坐标。
 * @property z     Z 坐标。
 */
data class GuildHome(
    @Id @Length(32) val id: String,
    @Length(32) var world: String,
    var x: Double,
    var y: Double,
    var z: Double
)
