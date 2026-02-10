package top.maplex.ptctest.data

/**
 * 坐标数据类 —— CustomType 演示用的自定义类型
 *
 * 本类不是一个独立的数据库表，而是作为其他 data class 的字段类型使用。
 * 框架无法直接将此类型映射到数据库列，需要通过 [CoordinateCustomType]
 * 注册自定义的序列化/反序列化逻辑。
 *
 * 存储格式：`"x,y,z"` 字符串（如 `"100.5,64.0,-200.3"`）
 *
 * @property x X 坐标
 * @property y Y 坐标
 * @property z Z 坐标
 */
data class Coordinate(
    val x: Double,
    val y: Double,
    val z: Double
) {
    override fun toString(): String = "($x, $y, $z)"
}
