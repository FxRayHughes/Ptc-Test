package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.ignoreHomeMapper
import top.maplex.ptctest.data.IgnorePlayerHome

/**
 * @Ignore 注解集成测试
 *
 * 本测试验证 `@Ignore` 注解的完整行为：被标记的字段不参与数据库的建表、
 * 插入、更新和查询操作，从数据库读取时使用 Kotlin 声明的默认值。
 *
 * ## @Ignore 注解
 *
 * ```kotlin
 * data class IgnorePlayerHome(
 *     @Id val username: String,
 *     var world: String,
 *     var x: Double,
 *     var y: Double,
 *     var z: Double,
 *     @Ignore val cachedDisplayName: String = "Unknown",  // 不入库，默认 "Unknown"
 *     @Ignore val tempScore: Int = 100,                   // 不入库，默认 100
 *     @Ignore val debugInfo: String? = null               // 不入库，默认 null
 * )
 * ```
 *
 * ## 核心行为
 *
 * - **建表**：@Ignore 字段不创建数据库列
 * - **insert**：@Ignore 字段的值不写入数据库
 * - **select**：从数据库读取时，@Ignore 字段使用 Kotlin 默认值
 * - **update**：@Ignore 字段不参与 SET 子句
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- 建表：只有 5 列，@Ignore 的 3 个字段不建列
 * CREATE TABLE ignore_player_home (
 *     username TEXT PRIMARY KEY,
 *     world    TEXT,
 *     x        REAL,
 *     y        REAL,
 *     z        REAL
 * )
 *
 * -- insert：只插入 5 个值
 * INSERT INTO ignore_player_home (username, world, x, y, z) VALUES (?, ?, ?, ?, ?)
 *
 * -- select：只查 5 列，@Ignore 字段由框架填充默认值
 * SELECT * FROM ignore_player_home WHERE username = ?
 * ```
 *
 * ## 测试覆盖
 *
 * 1. insert + findById —— @Ignore 字段不入库，读取时使用默认值
 * 2. 写入时 @Ignore 字段值被丢弃 —— 即使传入非默认值也不存储
 * 3. update 不影响 @Ignore 字段 —— 更新 var 字段后 @Ignore 仍为默认值
 * 4. 批量查询 —— 多条记录的 @Ignore 字段均为默认值
 */
object TestIgnore {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        ignoreHomeMapper.deleteWhere { "username" like "ign_%" }

        // ==================== 1. insert + findById ====================
        sender.sendMessage("§7  --- insert + findById 测试 ---")

        // 插入时传入 @Ignore 字段的自定义值
        // 这些值不会写入数据库
        val home1 = IgnorePlayerHome(
            username = "ign_player1",
            world = "world",
            x = 100.0, y = 64.0, z = -200.0,
            cachedDisplayName = "CustomName",  // 不入库
            tempScore = 999,                    // 不入库
            debugInfo = "some debug info"       // 不入库
        )
        ignoreHomeMapper.insert(home1)

        // 从数据库读取 → @Ignore 字段应为 Kotlin 默认值，而非插入时的值
        val found = ignoreHomeMapper.findById("ign_player1")
        assert(found != null, "findById 应返回非空")
        assert(found!!.username == "ign_player1", "username 应为 ign_player1")
        assert(found.world == "world", "world 应为 world, 实际: ${found.world}")
        assert(found.x == 100.0, "x 应为 100.0, 实际: ${found.x}")
        assert(found.y == 64.0, "y 应为 64.0, 实际: ${found.y}")
        assert(found.z == -200.0, "z 应为 -200.0, 实际: ${found.z}")

        // @Ignore 字段应为默认值
        assert(found.cachedDisplayName == "Unknown",
            "@Ignore cachedDisplayName 应为默认值 'Unknown', 实际: '${found.cachedDisplayName}'")
        assert(found.tempScore == 100,
            "@Ignore tempScore 应为默认值 100, 实际: ${found.tempScore}")
        assert(found.debugInfo == null,
            "@Ignore debugInfo 应为默认值 null, 实际: ${found.debugInfo}")
        sender.sendMessage("§7  insert+findById 完成: @Ignore 字段均为默认值")
        sender.sendMessage("§7    cachedDisplayName=${found.cachedDisplayName}, tempScore=${found.tempScore}, debugInfo=${found.debugInfo}")

        // ==================== 2. @Ignore 字段值被丢弃 ====================
        sender.sendMessage("§7  --- @Ignore 值丢弃验证 ---")

        // 再插入一条，@Ignore 字段传入不同的值
        val home2 = IgnorePlayerHome(
            username = "ign_player2",
            world = "nether",
            x = 50.0, y = 32.0, z = 50.0,
            cachedDisplayName = "AnotherName",
            tempScore = 12345
        )
        ignoreHomeMapper.insert(home2)

        val found2 = ignoreHomeMapper.findById("ign_player2")
        assert(found2 != null, "findById ign_player2 应返回非空")
        // 无论插入时传什么值，读取时 @Ignore 字段都是默认值
        assert(found2!!.cachedDisplayName == "Unknown",
            "第二条记录 cachedDisplayName 也应为 'Unknown', 实际: '${found2.cachedDisplayName}'")
        assert(found2.tempScore == 100,
            "第二条记录 tempScore 也应为 100, 实际: ${found2.tempScore}")
        sender.sendMessage("§7  @Ignore 值丢弃验证完成")

        // ==================== 3. update 不影响 @Ignore 字段 ====================
        sender.sendMessage("§7  --- update 测试 ---")

        // 更新 var 字段（world, x, y, z）
        ignoreHomeMapper.update(IgnorePlayerHome(
            username = "ign_player1",
            world = "world_nether",
            x = 999.0, y = 128.0, z = -999.0,
            cachedDisplayName = "UpdatedName",  // 不会写入数据库
            tempScore = 777                      // 不会写入数据库
        ))

        val afterUpdate = ignoreHomeMapper.findById("ign_player1")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        // var 字段应已更新
        assert(afterUpdate!!.world == "world_nether",
            "update 后 world 应为 world_nether, 实际: ${afterUpdate.world}")
        assert(afterUpdate.x == 999.0,
            "update 后 x 应为 999.0, 实际: ${afterUpdate.x}")
        // @Ignore 字段仍为默认值
        assert(afterUpdate.cachedDisplayName == "Unknown",
            "update 后 cachedDisplayName 仍应为 'Unknown', 实际: '${afterUpdate.cachedDisplayName}'")
        assert(afterUpdate.tempScore == 100,
            "update 后 tempScore 仍应为 100, 实际: ${afterUpdate.tempScore}")
        sender.sendMessage("§7  update 完成: var 字段已更新, @Ignore 字段不变")
        sender.sendMessage("§7    world=${afterUpdate.world}, x=${afterUpdate.x}")
        sender.sendMessage("§7    cachedDisplayName=${afterUpdate.cachedDisplayName}, tempScore=${afterUpdate.tempScore}")

        // ==================== 4. 批量查询 ====================
        sender.sendMessage("§7  --- 批量查询测试 ---")

        val all = ignoreHomeMapper.findAll { "username" like "ign_%" }
        assert(all.size == 2, "findAll 应返回 2 条, 实际: ${all.size}")
        // 所有记录的 @Ignore 字段都应为默认值
        all.forEach { home ->
            assert(home.cachedDisplayName == "Unknown",
                "${home.username} 的 cachedDisplayName 应为 'Unknown'")
            assert(home.tempScore == 100,
                "${home.username} 的 tempScore 应为 100")
            assert(home.debugInfo == null,
                "${home.username} 的 debugInfo 应为 null")
        }
        sender.sendMessage("§7  批量查询完成: ${all.size} 条记录的 @Ignore 字段均为默认值")

        // 清理
        ignoreHomeMapper.deleteWhere { "username" like "ign_%" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
