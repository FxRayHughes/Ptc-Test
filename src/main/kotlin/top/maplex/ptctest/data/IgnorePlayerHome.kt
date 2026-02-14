package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Ignore
import taboolib.expansion.Length

/**
 * @Ignore 注解测试 —— 忽略字段不参与数据库读写
 *
 * `@Ignore` 标记的字段不会在数据库中创建列，insert/update/select 均跳过。
 * 从数据库读取时，@Ignore 字段使用 Kotlin 声明的默认值。
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE ignore_player_home (
 *     username TEXT PRIMARY KEY,   -- @Id
 *     world    TEXT,               -- var 可更新
 *     x        REAL,
 *     y        REAL,
 *     z        REAL
 *     -- cachedDisplayName 不建列（@Ignore）
 *     -- tempScore 不建列（@Ignore）
 *     -- debugInfo 不建列（@Ignore）
 * )
 * ```
 *
 * @property username         玩家名，@Id 逻辑主键
 * @property world            世界名，var 可更新
 * @property x                X 坐标
 * @property y                Y 坐标
 * @property z                Z 坐标
 * @property cachedDisplayName 缓存的显示名，@Ignore 不入库，默认 "Unknown"
 * @property tempScore        临时分数，@Ignore 不入库，默认 100
 * @property debugInfo        调试信息，@Ignore 不入库，可空默认 null
 */
class IgnorePlayerHome{

    @Id
    var username: String = ""

    @Length(32)
    var world: String = ""

    var x: Double = 0.0
    var y: Double = 0.0
    var z: Double = 0.0

    @Ignore
    var cachedDisplayName: String = "Unknown"
    @Ignore
    var tempScore: Int = 100
    @Ignore
    var debugInfo: String? = null

}
