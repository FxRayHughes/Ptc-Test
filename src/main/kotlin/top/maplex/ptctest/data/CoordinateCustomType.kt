package top.maplex.ptctest.data

import taboolib.expansion.CustomType
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnTypeSQLite

/**
 * Coordinate 的自定义类型序列化器
 *
 * 实现 [CustomType] 接口，告诉框架如何将 [Coordinate] 对象存入数据库列、
 * 以及如何从数据库列值还原为 [Coordinate] 对象。
 *
 * **注册方式：**
 * 框架通过 ClassVisitor 自动扫描所有实现了 [CustomType] 接口的 object 单例，
 * 无需手动注册。只要本 object 存在于类路径中，启动时即自动生效。
 *
 * **序列化格式：** `"x,y,z"` 纯文本（如 `"100.5,64.0,-200.3"`）
 *
 * **列类型：** TEXT（SQL / SQLite 均为 TEXT），长度 128
 */
object CoordinateCustomType : CustomType {

    /** 本 CustomType 处理的目标类型 */
    override val type: Class<*> = Coordinate::class.java

    /** SQL 列类型 */
    override val typeSQL: ColumnTypeSQL = ColumnTypeSQL.TEXT

    /** SQLite 列类型 */
    override val typeSQLite: ColumnTypeSQLite = ColumnTypeSQLite.TEXT

    /** 列长度（MySQL VARCHAR 长度） */
    override val length: Int = 128

    /**
     * 序列化：Coordinate → 字符串
     *
     * 将坐标对象转为 `"x,y,z"` 格式的字符串存入数据库。
     */
    override fun serialize(value: Any): Any {
        val coord = value as Coordinate
        return "${coord.x},${coord.y},${coord.z}"
    }

    /**
     * 反序列化：字符串 → Coordinate
     *
     * 从数据库读取 `"x,y,z"` 字符串，解析为 [Coordinate] 对象。
     */
    override fun deserialize(value: Any): Any {
        val parts = value.toString().split(",")
        return Coordinate(
            x = parts[0].toDouble(),
            y = parts[1].toDouble(),
            z = parts[2].toDouble()
        )
    }
}
