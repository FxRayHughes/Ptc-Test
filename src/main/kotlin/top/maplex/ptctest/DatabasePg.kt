package top.maplex.ptctest

import taboolib.expansion.db
import taboolib.expansion.mapper
import top.maplex.ptctest.data.PgHome
import top.maplex.ptctest.data.PgSchemaHome
import top.maplex.ptctest.data.PlayerHome
import top.maplex.ptctest.data.PlayerStats

/**
 * PostgreSQL 数据库 Mapper 声明
 *
 * 使用 config.yml 中的 postgresql 节点配置连接。
 * 需要确保 PostgreSQL 服务可用且 config.yml 中配置正确。
 *
 * 配置示例（config.yml）：
 * ```yaml
 * postgresql:
 *   enable: true
 *   type: postgresql
 *   host: localhost
 *   port: 5432
 *   user: postgres
 *   password: postgres
 *   database: ptc_test
 *   schema: public
 * ```
 */

/** PostgreSQL 玩家家园 Mapper —— 使用 @TableName("pg_player_home") 自定义表名 */
val pgHomeMapper by mapper<PgHome>(db(node = "postgresql"))

/** PostgreSQL 原始 PlayerHome Mapper —— 表名自动生成为 player_home */
val pgPlayerHomeMapper by mapper<PlayerHome>(db(node = "postgresql"))

/** PostgreSQL 玩家统计 Mapper —— 用于 JOIN 联查测试 */
val pgStatsMapper by mapper<PlayerStats>(db(node = "postgresql"))

/** PostgreSQL Schema 测试 Mapper —— @TableName(schema = "game") 自动创建 Schema */
val pgSchemaHomeMapper by mapper<PgSchemaHome>(db(node = "postgresql"))
