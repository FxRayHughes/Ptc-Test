package top.maplex.ptctest.data

import taboolib.expansion.Id

/**
 * 玩家统计数据类 —— 用于联查（JOIN）测试
 *
 * 与 [PlayerHome] 通过 username 字段关联，测试 innerJoin 多表查询。
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE player_stats (
 *     username  TEXT PRIMARY KEY,
 *     kills     INTEGER,
 *     deaths    INTEGER,
 *     playtime  INTEGER            -- Long 映射为 INTEGER
 * )
 * ```
 *
 * @property username 玩家名，@Id 逻辑主键，与 PlayerHome.username 关联。
 * @property kills    击杀数。
 * @property deaths   死亡数。
 * @property playtime 游玩时长（秒）。Long 在 SQLite 中映射为 INTEGER。
 */
data class PlayerStats(
    @Id val username: String,
    var kills: Int,
    var deaths: Int,
    var playtime: Long
)
