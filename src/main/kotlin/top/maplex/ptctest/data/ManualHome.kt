package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length

/**
 * 手动建表测试数据类
 *
 * 此数据类配合 `manualTable(...)` 使用，框架不会自动建表，
 * 而是执行用户提供的 SQL 语句创建表结构。
 *
 * 字段结构与手动建表 SQL 保持一致：
 * ```sql
 * CREATE TABLE IF NOT EXISTS manual_home (
 *     username VARCHAR(64) PRIMARY KEY,
 *     world VARCHAR(64),
 *     x REAL DEFAULT 0,
 *     y REAL DEFAULT 0,
 *     z REAL DEFAULT 0
 * )
 * ```
 *
 * @property username 玩家名，逻辑主键
 * @property world    世界名
 * @property x        X 坐标
 * @property y        Y 坐标
 * @property z        Z 坐标
 */
data class ManualHome(
    @Id val username: String,
    @Length(64) var world: String,
    var x: Double,
    var y: Double,
    var z: Double
)
