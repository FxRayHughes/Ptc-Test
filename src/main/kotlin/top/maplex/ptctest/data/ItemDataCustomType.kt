package top.maplex.ptctest.data

import taboolib.expansion.CustomType
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnTypeSQLite

/**
 * 集合 CustomType：将 List<ItemData> 序列化为单列存储
 *
 * 当数据类字段为 `List<ItemData>` 时，框架检测到本 CustomType 的
 * `type = List::class.java` 且 `elementType = ItemData::class.java`，
 * 判定为"扁平化集合"，不创建子表，而是将整个 List 序列化为一个字符串存入单列。
 *
 * 序列化格式：`"name:count;name:count;..."`
 * 例如：`"diamond_sword:1;golden_apple:64;ender_pearl:16"`
 *
 * 与普通 CustomType 的区别：
 * - 普通 CustomType：`elementType` 为 null，处理单个对象
 * - 集合 CustomType：`elementType` 不为 null，处理整个集合
 */
object ItemDataListType : CustomType {

    /** 集合类型：List */
    override val type: Class<*> = List::class.java

    /** 元素类型：ItemData —— 标识这是一个集合 CustomType */
    override val elementType: Class<*> = ItemData::class.java

    override val typeSQL: ColumnTypeSQL = ColumnTypeSQL.TEXT
    override val typeSQLite: ColumnTypeSQLite = ColumnTypeSQLite.TEXT
    override val length: Int = 1024

    override fun serialize(value: Any): Any {
        @Suppress("UNCHECKED_CAST")
        val list = value as List<ItemData>
        return list.joinToString(";") { "${it.name}:${it.count}" }
    }

    override fun deserialize(value: Any): Any {
        val str = value.toString()
        if (str.isEmpty()) return emptyList<ItemData>()
        return str.split(";").map {
            val parts = it.split(":")
            ItemData(parts[0], parts[1].toInt())
        }
    }
}

/**
 * 普通 CustomType：处理单个 ItemData 对象的序列化
 *
 * 当 List<ItemData> 没有匹配的集合 CustomType 时，框架会走子表模式。
 * 子表中每个元素需要序列化为字符串存储，此时使用本 CustomType。
 *
 * 本测试项目中同时注册了 ItemDataListType（集合级别）和 ItemDataType（元素级别），
 * 框架会根据上下文自动选择：
 * - 扁平化集合字段 → 使用 ItemDataListType
 * - 子表元素序列化 → 使用 ItemDataType
 */
object ItemDataType : CustomType {

    override val type: Class<*> = ItemData::class.java

    override val typeSQL: ColumnTypeSQL = ColumnTypeSQL.TEXT
    override val typeSQLite: ColumnTypeSQLite = ColumnTypeSQLite.TEXT
    override val length: Int = 256

    override fun serialize(value: Any): Any {
        val item = value as ItemData
        return "${item.name}:${item.count}"
    }

    override fun deserialize(value: Any): Any {
        val parts = value.toString().split(":")
        return ItemData(parts[0], parts[1].toInt())
    }
}
