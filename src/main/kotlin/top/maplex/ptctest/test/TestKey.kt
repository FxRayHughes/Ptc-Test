package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * @Key 复合定位测试
 *
 * 本测试验证 `@Key` 注解与 `@Id` 组合使用时的复合定位能力。
 * `findByKey` / `existsByKey` / `deleteByKey` 通过 @Id + @Key 字段组合作为 WHERE 条件。
 *
 * ## 背景知识
 *
 * `@Id` 和 `@Key` 的区别：
 * - `@Id`：逻辑主键，用于 findById/deleteById 等基础 CRUD 操作
 * - `@Key`：索引字段，与 @Id 组合用于 findByKey/deleteByKey 等复合定位操作
 *
 * 在 PlayerHome 中：
 * - `@Id username`：玩家名
 * - `@Key serverName`：服务器名
 *
 * 复合定位的意义：同一个玩家在不同服务器可以有不同的家（MySQL 模式下）。
 *
 * ## SQLite vs MySQL 的差异
 *
 * - **SQLite**：@Id 是 PRIMARY KEY（唯一约束），同一 username 只能有一条记录。
 *   因此本测试用不同的 username 插入多条记录来模拟。
 * - **MySQL**：@Id 是普通 KEY（非唯一），同一 username + 不同 serverName 可以有多条记录。
 *   这才是 @Key 复合定位的真正用武之地。
 *
 * ## 测试的 API 方法
 *
 * | 方法                    | 说明                                                    |
 * |------------------------|---------------------------------------------------------|
 * | `findByKey(probe)`     | 通过探针对象的 @Id + @Key 字段值查询，返回 List<T>        |
 * | `existsByKey(probe)`   | 通过探针对象的 @Id + @Key 字段值检查存在性                 |
 * | `deleteByKey(probe)`   | 通过探针对象的 @Id + @Key 字段值精确删除                   |
 *
 * ## 关键概念：探针对象（Probe Object）
 *
 * `findByKey` / `existsByKey` / `deleteByKey` 接收一个数据类实例作为"探针"。
 * 框架从探针中提取 @Id 和 @Key 字段的值作为 WHERE 条件，其他字段的值被忽略。
 *
 * ```kotlin
 * // 只有 username 和 serverName 参与查询，其他字段值无关紧要
 * val probe = PlayerHome("key_user_2", "survival", "", 0.0, 0.0, 0.0, false)
 * val found = homeMapper.findByKey(probe)
 * // 生成 SQL: SELECT * FROM `player_home` WHERE `username` = ? AND `server_name` = ?
 * ```
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- findByKey
 * SELECT * FROM `player_home` WHERE `username` = ? AND `server_name` = ?
 *
 * -- existsByKey
 * SELECT COUNT(1) FROM `player_home` WHERE `username` = ? AND `server_name` = ?
 *
 * -- deleteByKey
 * DELETE FROM `player_home` WHERE `username` = ? AND `server_name` = ?
 * ```
 */
object TestKey {

    fun run(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "key_user_1" }
        homeMapper.deleteWhere { "username" eq "key_user_2" }
        homeMapper.deleteWhere { "username" eq "key_user_3" }

        // 插入 3 条记录，各自不同的 username + serverName
        val home1 = PlayerHome("key_user_1", "lobby", "world", 0.0, 64.0, 0.0, true)
        val home2 = PlayerHome("key_user_2", "survival", "world", 100.0, 64.0, 100.0, true)
        val home3 = PlayerHome("key_user_3", "creative", "world_nether", 50.0, 64.0, 50.0, false)
        homeMapper.insert(home1)
        homeMapper.insert(home2)
        homeMapper.insert(home3)
        sender.sendMessage("§7  插入 3 条记录完成")

        // findByKey —— 构造探针对象
        // 框架提取 @Id(username="key_user_2") + @Key(serverName="survival") 作为 WHERE 条件
        // 其他字段（world, x, y, z, active）的值不参与查询，可以随意填写
        val probe = PlayerHome("key_user_2", "survival", "", 0.0, 0.0, 0.0, false)
        val found = homeMapper.findByKey(probe)
        assert(found.size == 1, "findByKey 应返回 1 条, 实际: ${found.size}")
        assert(found[0].x == 100.0, "findByKey 结果 x 应为 100.0")
        sender.sendMessage("§7  findByKey 完成, 数量: ${found.size}")

        // existsByKey —— 检查 @Id + @Key 组合是否存在
        val exists = homeMapper.existsByKey(probe)
        assert(exists, "existsByKey 应返回 true")
        sender.sendMessage("§7  existsByKey 完成: $exists")

        // deleteByKey —— 通过 @Id + @Key 精确删除
        // 只删除 username="key_user_2" AND serverName="survival" 的记录
        homeMapper.deleteByKey(probe)
        val afterDelete = homeMapper.existsByKey(probe)
        assert(!afterDelete, "deleteByKey 后 existsByKey 应返回 false")
        sender.sendMessage("§7  deleteByKey 完成")

        // 验证 deleteByKey 的精确性：只删除了目标记录，其他记录不受影响
        val r1 = homeMapper.exists("key_user_1")
        val r3 = homeMapper.exists("key_user_3")
        assert(r1, "key_user_1 应仍存在")
        assert(r3, "key_user_3 应仍存在")
        sender.sendMessage("§7  其他记录未受影响")

        homeMapper.deleteWhere { "username" eq "key_user_1" }
        homeMapper.deleteWhere { "username" eq "key_user_3" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
