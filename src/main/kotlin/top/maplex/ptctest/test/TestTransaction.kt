package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.homeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * 事务操作测试
 *
 * 本测试验证 `DataMapper.transaction {}` 方法，事务内的所有操作
 * 要么全部提交（commit），要么全部回滚（rollback）。
 *
 * ## 事务的 ACID 特性
 *
 * - **原子性（Atomicity）**：事务内的操作要么全部成功，要么全部失败
 * - **一致性（Consistency）**：事务前后数据库状态一致
 * - **隔离性（Isolation）**：事务之间互不干扰
 * - **持久性（Durability）**：事务提交后数据永久保存
 *
 * ## transaction {} 的使用方式
 *
 * ```kotlin
 * val result: Result<R> = homeMapper.transaction {
 *     // this 是 DataMapper<PlayerHome>
 *     // 可以直接调用 insert/update/findById 等方法
 *     insert(home1)
 *     insert(home2)
 *     val found = findById("xxx")
 *     // lambda 的返回值作为 Result 的成功值
 *     found?.world ?: "not found"
 * }
 *
 * // 检查结果
 * result.isSuccess       // 事务是否成功
 * result.getOrNull()     // 获取返回值（失败时为 null）
 * result.exceptionOrNull() // 获取异常（成功时为 null）
 * ```
 *
 * ## 关键概念
 *
 * - **this 上下文**：transaction lambda 内的 `this` 是 `DataMapper<T>`，
 *   可以直接调用所有 DataMapper 方法，无需前缀。
 *
 * - **返回值**：lambda 的最后一个表达式作为 `Result<R>` 的成功值。
 *   如果 lambda 抛出异常，事务自动回滚，Result 包含异常信息。
 *
 * - **自动回滚**：如果 lambda 内任何操作抛出异常，
 *   框架自动调用 `connection.rollback()`，所有操作都不会生效。
 *
 * - **Result<R>**：Kotlin 标准库的 Result 类型，封装成功值或异常。
 *   使用 `isSuccess` / `getOrNull()` / `exceptionOrNull()` 检查结果。
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- 事务开始
 * BEGIN TRANSACTION
 *
 * -- 事务内的操作
 * INSERT INTO `player_home` (...) VALUES (?, ?, ?, ?, ?, ?, ?)
 * INSERT INTO `player_home` (...) VALUES (?, ?, ?, ?, ?, ?, ?)
 * SELECT * FROM `player_home` WHERE `username` = ?
 * UPDATE `player_home` SET ... WHERE `username` = ?
 *
 * -- 事务提交（或回滚）
 * COMMIT  -- 成功时
 * ROLLBACK  -- 异常时
 * ```
 */
object TestTransaction {

    fun run(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "tx_user_1" }
        homeMapper.deleteWhere { "username" eq "tx_user_2" }

        // transaction {} —— 事务块
        // lambda 内的 this 是 DataMapper<PlayerHome>
        // 返回 Result<Int>（lambda 最后一个表达式的类型）
        val result = homeMapper.transaction {
            // 事务内插入两条记录
            // 如果第二条插入失败，第一条也会被回滚
            insert(PlayerHome("tx_user_1", "lobby", "world", 0.0, 64.0, 0.0, true))
            insert(PlayerHome("tx_user_2", "survival", "world", 100.0, 64.0, 100.0, false))

            // 事务内查询并更新
            // 事务内的查询能看到事务内之前的插入（读己之写）
            val home = findById("tx_user_1")
            if (home != null) {
                update(home.copy(world = "world_nether"))
            }

            // 事务内验证：两条记录都应存在
            val r1 = findById("tx_user_1")
            val r2 = findById("tx_user_2")
            // 返回值：找到的记录数（作为 Result<Int> 的成功值）
            (if (r1 != null) 1 else 0) + (if (r2 != null) 1 else 0)
        }

        // 检查事务执行结果
        // result.isSuccess：事务是否成功提交
        // result.getOrNull()：获取 lambda 的返回值
        // result.exceptionOrNull()：获取异常信息（成功时为 null）
        assert(result.isSuccess, "事务应成功, 错误: ${result.exceptionOrNull()?.message}")
        assert(result.getOrNull() == 2, "事务内应找到 2 条记录, 实际: ${result.getOrNull()}")
        sender.sendMessage("§7  transaction 完成, 结果: ${result.getOrNull()}")

        // 事务外验证：update 的效果应已持久化
        // 事务提交后，所有修改对外部可见
        val afterTx = homeMapper.findById("tx_user_1")
        assert(afterTx != null, "事务后 findById 应返回非空")
        assert(afterTx!!.world == "world_nether", "事务后 world 应为 world_nether, 实际: ${afterTx.world}")
        sender.sendMessage("§7  事务外验证完成: world=${afterTx.world}")

        homeMapper.deleteWhere { "username" eq "tx_user_1" }
        homeMapper.deleteWhere { "username" eq "tx_user_2" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
