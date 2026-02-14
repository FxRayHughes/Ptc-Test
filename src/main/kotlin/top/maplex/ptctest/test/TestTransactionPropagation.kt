package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.statsMapper
import top.maplex.ptctest.data.PlayerHome
import top.maplex.ptctest.data.PlayerStats

/**
 * 事务传播（Transaction Propagation）集成测试
 *
 * 本测试验证嵌套 `transaction {}` 调用时，内层事务自动复用外层事务的数据库连接，
 * 所有操作在同一事务中提交或回滚。
 *
 * ## 问题背景
 *
 * 在没有事务传播之前，每次调用 `transaction {}` 都会创建新的 Connection：
 *
 * ```kotlin
 * homeMapper.transaction {
 *     insert(home1)
 *     // 嵌套调用 → 创建新连接、新事务，与外层事务无关！
 *     homeMapper.transaction {
 *         insert(home2)  // 不在同一事务中
 *     }
 * }
 * ```
 *
 * ## 事务传播机制
 *
 * 框架使用 `ThreadLocal<Connection>` 跟踪当前线程的活跃事务连接：
 *
 * 1. 外层 `transaction {}` 创建新连接，设置 `ThreadLocal`
 * 2. 内层 `transaction {}` 检测到 `ThreadLocal` 中有活跃连接，直接复用
 * 3. 内层事务不执行 commit/rollback，只返回 Result
 * 4. 外层事务负责最终的提交或回滚
 * 5. 外层 `finally` 块清理 `ThreadLocal`
 *
 * ## 关键行为
 *
 * - **嵌套复用**：内层事务复用外层连接，所有操作在同一事务中
 * - **统一提交**：只有外层事务执行 commit
 * - **统一回滚**：外层回滚时，内层的操作也被回滚
 * - **非事务操作器参与**：在事务块内使用普通操作器（非事务感知），
 *   也会通过 ThreadLocal 自动参与当前事务
 *
 * ## 测试覆盖
 *
 * 1. 嵌套事务提交 —— 内外操作在同一事务中提交
 * 2. 外层回滚传播 —— 外层回滚导致内层数据也回滚
 * 3. 内层异常传播 —— 内层异常导致外层回滚
 * 4. 跨 Mapper 嵌套事务 —— 不同 Mapper 的嵌套事务共享连接
 */
object TestTransactionPropagation {

    fun run(sender: ProxyCommandSender) {
        // 前置清理
        homeMapper.deleteWhere { "username" like "txp_%" }
        statsMapper.deleteWhere { "username" like "txp_%" }

        // ==================== 1. 嵌套事务提交 ====================
        sender.sendMessage("§7  --- 嵌套事务提交测试 ---")

        val result1 = homeMapper.transaction {
            insert(PlayerHome("txp_user1", "lobby", "world", 0.0, 64.0, 0.0, true))

            // 嵌套事务：应复用外层连接
            val innerResult = homeMapper.transaction {
                insert(PlayerHome("txp_user2", "survival", "world", 100.0, 64.0, 100.0, false))
                "inner-ok"
            }
            assert(innerResult.isSuccess, "内层事务应成功, 错误: ${innerResult.exceptionOrNull()?.message}")
            assert(innerResult.getOrNull() == "inner-ok", "内层返回值应为 inner-ok")

            "outer-ok"
        }

        assert(result1.isSuccess, "外层事务应成功, 错误: ${result1.exceptionOrNull()?.message}")
        assert(result1.getOrNull() == "outer-ok", "外层返回值应为 outer-ok")

        // 两条数据都应被提交
        val user1 = homeMapper.findById("txp_user1")
        val user2 = homeMapper.findById("txp_user2")
        assert(user1 != null, "txp_user1 应存在")
        assert(user2 != null, "txp_user2 应存在")
        sender.sendMessage("§7  嵌套事务提交完成: user1=${user1?.username}, user2=${user2?.username}")

        // 清理
        homeMapper.deleteWhere { "username" like "txp_%" }

        // ==================== 2. 外层回滚传播 ====================
        sender.sendMessage("§7  --- 外层回滚传播测试 ---")

        val result2 = homeMapper.transaction {
            insert(PlayerHome("txp_user3", "lobby", "world", 0.0, 64.0, 0.0, true))

            // 嵌套事务插入数据
            homeMapper.transaction {
                insert(PlayerHome("txp_user4", "survival", "world", 100.0, 64.0, 100.0, false))
            }

            // 外层抛出异常 → 整个事务回滚（包括内层插入的数据）
            error("outer-rollback")
        }

        assert(result2.isFailure, "外层事务应失败")
        assert(result2.exceptionOrNull()?.message == "outer-rollback", "异常消息应为 outer-rollback")

        // 内外数据都应被回滚
        val user3 = homeMapper.findById("txp_user3")
        val user4 = homeMapper.findById("txp_user4")
        assert(user3 == null, "txp_user3 应被回滚（不存在）, 实际: $user3")
        assert(user4 == null, "txp_user4 应被回滚（不存在）, 实际: $user4")
        sender.sendMessage("§7  外层回滚传播完成: user3=${user3}, user4=${user4}")

        // ==================== 3. 内层异常传播 ====================
        sender.sendMessage("§7  --- 内层异常传播测试 ---")

        val result3 = homeMapper.transaction {
            insert(PlayerHome("txp_user5", "lobby", "world", 0.0, 64.0, 0.0, true))

            val innerResult = homeMapper.transaction {
                insert(PlayerHome("txp_user6", "survival", "world", 100.0, 64.0, 100.0, false))
                error("inner-error")
            }

            // 内层异常被捕获为 Result.failure，外层选择重新抛出
            throw innerResult.exceptionOrNull()!!
        }

        assert(result3.isFailure, "外层事务应失败")
        assert(result3.exceptionOrNull()?.message == "inner-error", "异常消息应为 inner-error")

        val user5 = homeMapper.findById("txp_user5")
        val user6 = homeMapper.findById("txp_user6")
        assert(user5 == null, "txp_user5 应被回滚, 实际: $user5")
        assert(user6 == null, "txp_user6 应被回滚, 实际: $user6")
        sender.sendMessage("§7  内层异常传播完成: user5=${user5}, user6=${user6}")

        // ==================== 4. 跨 Mapper 嵌套事务 ====================
        sender.sendMessage("§7  --- 跨 Mapper 嵌套事务测试 ---")

        val result4 = homeMapper.transaction {
            insert(PlayerHome("txp_user7", "lobby", "world", 0.0, 64.0, 0.0, true))

            // 使用不同的 Mapper 嵌套事务
            // statsMapper.transaction 检测到 ThreadLocal 中有活跃连接，复用
            statsMapper.transaction {
                insert(PlayerStats("txp_user7", 100, 50, 10))
            }

            "cross-mapper-ok"
        }

        assert(result4.isSuccess, "跨 Mapper 事务应成功, 错误: ${result4.exceptionOrNull()?.message}")
        val home7 = homeMapper.findById("txp_user7")
        val stats7 = statsMapper.findById("txp_user7")
        assert(home7 != null, "txp_user7 home 应存在")
        assert(stats7 != null, "txp_user7 stats 应存在")
        sender.sendMessage("§7  跨 Mapper 嵌套事务完成: home=${home7?.username}, stats=${stats7?.username}")

        // 清理
        homeMapper.deleteWhere { "username" like "txp_%" }
        statsMapper.deleteWhere { "username" like "txp_%" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
