package top.maplex.ptctest

import taboolib.expansion.dbFile
import taboolib.expansion.mapper
import top.maplex.ptctest.data.LinkedPlayerHome
import top.maplex.ptctest.data.Guild
import top.maplex.ptctest.data.GuildLeader
import top.maplex.ptctest.data.GuildHome
import top.maplex.ptctest.data.PlayerHome
import top.maplex.ptctest.data.PlayerStats
import top.maplex.ptctest.data.PlayerWaypoint
import top.maplex.ptctest.data.SimpleNote
import top.maplex.ptctest.data.AccountData

/**
 * 数据库 Mapper 声明
 *
 * 使用 `by mapper<T>(source)` 委托创建 DataMapper 实例。
 * - 首次访问时懒加载：自动建表 + 创建连接池
 * - source 参数：dbFile() 返回 SQLite 文件路径，dbSection() 返回 MySQL 配置
 * - 同一个 dbFile 的多个 mapper 共享同一个数据库文件，但各自拥有独立的连接池
 */

/** 玩家家园 Mapper —— 无缓存（默认），用于大部分 CRUD 测试 */
val homeMapper by mapper<PlayerHome>(dbFile("test.db"))

/** 玩家统计 Mapper —— 与 homeMapper 共用 test.db，用于 JOIN 联查测试 */
val statsMapper by mapper<PlayerStats>(dbFile("test.db"))

/** 简单笔记 Mapper —— 无 @Id 数据类，用于 rowId / autoKey / columnType 测试 */
val noteMapper by mapper<SimpleNote>(dbFile("test.db"))

/** 关联表 Mapper —— @LinkTable 自动 LEFT JOIN 测试，与 statsMapper 共用 test.db */
val linkedHomeMapper by mapper<LinkedPlayerHome>(dbFile("test.db"))

/** 路径点 Mapper —— CustomType 自定义类型测试，Coordinate 字段自动序列化为 "x,y,z" 字符串 */
val waypointMapper by mapper<PlayerWaypoint>(dbFile("test.db"))

/** 公会家园 Mapper —— @LinkTable 嵌套关联叶子节点（第三层） */
val guildHomeMapper by mapper<GuildHome>(dbFile("test.db"))

/** 公会会长 Mapper —— @LinkTable 嵌套关联中间节点（第二层），关联 GuildHome */
val guildLeaderMapper by mapper<GuildLeader>(dbFile("test.db"))

/** 公会 Mapper —— @LinkTable 嵌套关联根节点（第一层），关联 GuildLeader → GuildHome */
val guildMapper by mapper<Guild>(dbFile("test.db"))

/** 账户 Mapper —— IndexedEnum 枚举索引测试，AccountType 以数值存储 */
val accountMapper by mapper<AccountData>(dbFile("test.db"))

/**
 * 带缓存的玩家家园 Mapper —— 使用独立的 test_cached.db
 *
 * L2 双层缓存配置：
 * - **Bean Cache**：按实体 ID 存储，maximumSize=100，写入后 60 秒过期
 * - **Query Cache**：按查询哈希存储，maximumSize=50，写入后 60 秒过期
 *
 * 缓存行为：
 * - 读操作（findById 等）命中缓存时直接返回，不查库
 * - 插入操作仅清空 Query Cache，Bean Cache 不受影响
 * - 单条更新/删除失效该 ID 的 Bean Cache + 清空 Query Cache
 * - 批量/不确定范围操作全部清空
 *
 * 使用独立数据库文件，避免与无缓存 mapper 的数据互相干扰。
 */
val cachedHomeMapper by mapper<PlayerHome>(dbFile("test_cached.db")) {
    cache {
        beanCache { maximumSize = 100; expireAfterWrite = 60 }
        queryCache { maximumSize = 50; expireAfterWrite = 60 }
    }
}
