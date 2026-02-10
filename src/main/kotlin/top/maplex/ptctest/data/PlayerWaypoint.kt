package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * 玩家路径点数据类 —— 演示 CustomType 自定义类型字段
 *
 * [coordinate] 字段的类型是 [Coordinate]（自定义 data class），
 * 框架无法直接映射此类型到数据库列。通过 [CoordinateCustomType] 注册
 * 自定义序列化逻辑后，框架会自动调用 serialize/deserialize 完成转换。
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE player_waypoint (
 *     name        TEXT PRIMARY KEY,   -- @Id 逻辑主键
 *     owner       TEXT,               -- 所有者
 *     coordinate  TEXT,               -- CustomType: Coordinate → "x,y,z"
 *     tag         TEXT                -- 标签
 * )
 * ```
 *
 * @property name       路径点名称，@Id 逻辑主键。
 * @property owner      所有者玩家名。
 * @property coordinate 坐标位置。CustomType 自动序列化为 `"x,y,z"` 字符串存储。
 * @property tag        标签。
 */
data class PlayerWaypoint(
    @Id val name: String,
    @Length(32) val owner: String,
    var coordinate: Coordinate,
    @Length(64) var tag: String,
    var uuid: UUID
)
