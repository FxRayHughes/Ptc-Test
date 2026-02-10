package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length
import taboolib.expansion.LinkTable

/**
 * 公会数据类 —— @LinkTable 嵌套关联的根节点（第一层）
 *
 * 通过 @LinkTable("leaderId") 关联 [GuildLeader]，而 GuildLeader 又通过
 * @LinkTable("homeId") 关联 [GuildHome]，形成三层嵌套关联链：
 *
 * **Guild → GuildLeader → GuildHome**
 *
 * 查询 Guild 时，框架自动生成：
 * ```sql
 * SELECT `guild`.`id`, `guild`.`name`, `guild`.`level`,
 *        `__t0`.`id` AS `__link__leader_id__id`,
 *        `__t0`.`name` AS `__link__leader_id__name`,
 *        `__t1`.`id` AS `__link__leader_id____link__home_id__id`,
 *        `__t1`.`world` AS `__link__leader_id____link__home_id__world`, ...
 * FROM `guild`
 * LEFT JOIN `guild_leader` AS `__t0` ON `guild`.`leader_id` = `__t0`.`id`
 * LEFT JOIN `guild_home` AS `__t1` ON `__t0`.`home_id` = `__t1`.`id`
 * ```
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE guild (
 *     id        TEXT PRIMARY KEY,
 *     name      TEXT,
 *     level     INTEGER,
 *     leader_id TEXT              -- @LinkTable 外键列，指向 guild_leader.id
 * )
 * ```
 *
 * @property id     公会 ID，@Id 逻辑主键。
 * @property name   公会名称。
 * @property level  公会等级。
 * @property leader 公会会长。@LinkTable 自动 LEFT JOIN guild_leader + 级联保存。
 */
data class Guild(
    @Id @Length(32) val id: String,
    @Length(64) val name: String,
    var level: Int,
    @LinkTable("leaderId") val leader: GuildLeader?
)
