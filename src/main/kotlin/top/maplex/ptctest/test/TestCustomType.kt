package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.waypointMapper
import top.maplex.ptctest.data.PlayerWaypoint
import top.maplex.ptctest.data.Coordinate
import java.util.UUID

/**
 * CustomType 自定义类型测试
 *
 * 本测试验证 CustomType 机制：当 data class 字段类型不是框架内置支持的类型时，
 * 通过注册自定义的序列化/反序列化逻辑，实现任意类型与数据库列的映射。
 *
 * ## 框架内置支持的类型
 *
 * | Kotlin 类型    | 数据库类型（SQLite） | 数据库类型（MySQL）  |
 * |---------------|--------------------|--------------------|
 * | String        | TEXT               | VARCHAR            |
 * | Int           | INTEGER            | INT                |
 * | Long          | INTEGER            | BIGINT             |
 * | Double        | REAL               | DOUBLE             |
 * | Boolean       | INTEGER (0/1)      | TINYINT (0/1)      |
 * | Enum          | TEXT               | VARCHAR            |
 *
 * 当字段类型不在上述列表中（如 Coordinate），框架无法自动映射，
 * 需要通过 CustomType 告诉框架如何处理。
 *
 * ## CustomType 接口
 *
 * ```kotlin
 * object CoordinateCustomType : CustomType {
 *     override val type: Class<*> = Coordinate::class.java  // 目标类型
 *     override val typeSQL: ColumnTypeSQL = ColumnTypeSQL.TEXT  // MySQL 列类型
 *     override val typeSQLite: ColumnTypeSQLite = ColumnTypeSQLite.TEXT  // SQLite 列类型
 *     override val length: Int = 128  // VARCHAR 长度
 *
 *     override fun serialize(value: Any): Any {
 *         val coord = value as Coordinate
 *         return "${coord.x},${coord.y},${coord.z}"  // 对象 → 字符串
 *     }
 *
 *     override fun deserialize(value: Any): Any {
 *         val parts = value.toString().split(",")
 *         return Coordinate(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
 *     }
 * }
 * ```
 *
 * ## 注册方式
 *
 * CustomType 的注册是自动的：框架通过 ClassVisitor 扫描所有实现了 CustomType 接口的
 * object 单例，启动时自动注册。无需手动调用任何注册方法。
 *
 * ## 序列化/反序列化流程
 *
 * **写入（insert/update）**：
 * ```
 * Coordinate(100.5, 64.0, -200.3)
 *   → serialize() → "100.5,64.0,-200.3"
 *   → INSERT INTO ... VALUES (..., "100.5,64.0,-200.3", ...)
 * ```
 *
 * **读取（findById/findAll）**：
 * ```
 * SELECT ... → ResultSet.getString("coordinate") → "100.5,64.0,-200.3"
 *   → deserialize() → Coordinate(100.5, 64.0, -200.3)
 * ```
 *
 * ## 建表结构
 *
 * ```sql
 * CREATE TABLE player_waypoint (
 *     name        TEXT PRIMARY KEY,
 *     owner       TEXT,
 *     coordinate  TEXT,    -- CustomType: Coordinate → "x,y,z" 字符串
 *     tag         TEXT
 * )
 * ```
 */
object TestCustomType {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        waypointMapper.deleteWhere { "name" eq "wp_spawn" }
        waypointMapper.deleteWhere { "name" eq "wp_mine" }
        waypointMapper.deleteWhere { "name" eq "wp_farm" }

        // 1. insert —— Coordinate 自动序列化为 "x,y,z" 字符串存入数据库
        //    框架检测到 coordinate 字段类型是 Coordinate，
        //    查找已注册的 CoordinateCustomType，调用 serialize() 转换
        val wp1 = PlayerWaypoint("wp_spawn", "player1", Coordinate(100.5, 64.0, -200.3), "出生点", UUID.randomUUID())
        val wp2 = PlayerWaypoint("wp_mine", "player1", Coordinate(-50.0, 12.0, 300.0), "矿洞", UUID.randomUUID())
        val wp3 = PlayerWaypoint("wp_farm", "player2", Coordinate(0.0, 70.0, 0.0), "农场", UUID.randomUUID())
        waypointMapper.insertBatch(listOf(wp1, wp2, wp3))
        sender.sendMessage("§7  insert 3 条路径点完成")

        // 2. findById —— 从数据库读取字符串，自动反序列化为 Coordinate 对象
        //    框架从 ResultSet 获取 "100.5,64.0,-200.3" 字符串，
        //    调用 CoordinateCustomType.deserialize() 转换为 Coordinate 对象
        val found = waypointMapper.findById("wp_spawn")
        assert(found != null, "findById wp_spawn 应返回非空")
        assert(found!!.coordinate.x == 100.5, "coordinate.x 应为 100.5, 实际: ${found.coordinate.x}")
        assert(found.coordinate.y == 64.0, "coordinate.y 应为 64.0, 实际: ${found.coordinate.y}")
        assert(found.coordinate.z == -200.3, "coordinate.z 应为 -200.3, 实际: ${found.coordinate.z}")
        assert(found.tag == "出生点", "tag 应为 出生点, 实际: ${found.tag}")
        sender.sendMessage("§7  findById 完成: name=${found.name}, coord=${found.coordinate}, tag=${found.tag}")

        // 3. update —— 修改 Coordinate 字段，框架自动重新序列化
        //    新的 Coordinate(999.0, 128.0, -999.0) 会被序列化为 "999.0,128.0,-999.0"
        val updated = found.copy(coordinate = Coordinate(999.0, 128.0, -999.0), tag = "新出生点")
        waypointMapper.update(updated)
        sender.sendMessage("§7  update 完成")

        val afterUpdate = waypointMapper.findById("wp_spawn")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.coordinate.x == 999.0, "update 后 x 应为 999.0, 实际: ${afterUpdate.coordinate.x}")
        assert(afterUpdate.coordinate.y == 128.0, "update 后 y 应为 128.0, 实际: ${afterUpdate.coordinate.y}")
        assert(afterUpdate.coordinate.z == -999.0, "update 后 z 应为 -999.0, 实际: ${afterUpdate.coordinate.z}")
        assert(afterUpdate.tag == "新出生点", "update 后 tag 应为 新出生点, 实际: ${afterUpdate.tag}")
        sender.sendMessage("§7  update 验证完成: coord=${afterUpdate.coordinate}")

        // 4. findAll —— 批量查询，所有记录的 Coordinate 均正确反序列化
        //    框架对每条记录的 coordinate 列都调用 deserialize()
        val all = waypointMapper.findAll { "owner" eq "player1" }
        assert(all.size == 2, "player1 的路径点应为 2 条, 实际: ${all.size}")
        val mine = all.find { it.name == "wp_mine" }
        assert(mine != null, "应找到 wp_mine")
        assert(mine!!.coordinate.x == -50.0, "wp_mine x 应为 -50.0, 实际: ${mine.coordinate.x}")
        sender.sendMessage("§7  findAll 完成: ${all.map { "${it.name}→${it.coordinate}" }}")

        // 清理
        waypointMapper.deleteWhere { "name" eq "wp_spawn" }
        waypointMapper.deleteWhere { "name" eq "wp_mine" }
        waypointMapper.deleteWhere { "name" eq "wp_farm" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
