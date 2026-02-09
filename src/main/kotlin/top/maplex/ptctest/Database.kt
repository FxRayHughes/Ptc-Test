package top.maplex.ptctest

import taboolib.expansion.dbFile
import taboolib.expansion.mapper
import top.maplex.ptctest.data.PlayerHome
import top.maplex.ptctest.data.PlayerStats
import top.maplex.ptctest.data.SimpleNote

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

/**
 * 带缓存的玩家家园 Mapper —— 使用独立的 test_cached.db
 *
 * 缓存配置：
 * - maximumSize = 100：缓存最多 100 条记录
 * - expireAfterWrite = 60：写入后 60 秒过期
 *
 * 缓存行为：
 * - 读操作（findById 等）命中缓存时直接返回，不查库
 * - 写操作（insert/update/delete）自动失效相关缓存条目
 *
 * 使用独立数据库文件，避免与无缓存 mapper 的数据互相干扰。
 */
val cachedHomeMapper by mapper<PlayerHome>(dbFile("test_cached.db")) {
    cache {
        maximumSize = 100
        expireAfterWrite = 60
    }
}
