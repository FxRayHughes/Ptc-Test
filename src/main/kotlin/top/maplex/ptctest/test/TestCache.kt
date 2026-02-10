package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.cachedHomeMapper
import top.maplex.ptctest.data.PlayerHome

/**
 * L2 缓存行为测试
 *
 * 本测试验证 DataMapper 的 L2 双层缓存机制，确保缓存的命中、失效和重新加载行为正确。
 *
 * ## 缓存架构
 *
 * ```
 * ┌─────────────────────────────────────────────┐
 * │              DataMapper (cached)             │
 * │  ┌─────────────┐    ┌──────────────┐        │
 * │  │ Bean Cache  │    │ Query Cache  │        │
 * │  │ (按 ID 存储) │    │ (按查询哈希)  │        │
 * │  └──────┬──────┘    └──────┬───────┘        │
 * │         │                  │                 │
 * │         └────────┬─────────┘                 │
 * │                  ↓                           │
 * │           ┌──────────┐                       │
 * │           │ Database │                       │
 * │           └──────────┘                       │
 * └─────────────────────────────────────────────┘
 * ```
 *
 * ## 双层缓存说明
 *
 * **Bean Cache（实体缓存）**：
 * - 按实体 @Id 值存储完整对象
 * - `findById("xxx")` 命中时直接返回缓存对象，不查库
 * - 配置：`maximumSize=100`（最多缓存 100 个实体），`expireAfterWrite=60`（写入后 60 秒过期）
 *
 * **Query Cache（查询缓存）**：
 * - 按查询条件的哈希值存储查询结果列表
 * - `findAll { ... }` / `sort(...)` 等条件查询命中时直接返回
 * - 配置：`maximumSize=50`，`expireAfterWrite=60`
 *
 * ## 缓存失效策略
 *
 * | 操作类型                | Bean Cache          | Query Cache    |
 * |------------------------|---------------------|----------------|
 * | 读操作（findById 等）   | 命中则返回，未命中则填充 | 同左            |
 * | 插入操作（insert）      | 不受影响              | 全部清空        |
 * | 单条更新/删除           | 失效该 ID 的缓存条目   | 全部清空        |
 * | 批量/不确定范围操作      | 全部清空              | 全部清空        |
 *
 * ## 缓存配置方式
 *
 * ```kotlin
 * val cachedMapper by mapper<PlayerHome>(dbFile("test_cached.db")) {
 *     cache {
 *         beanCache { maximumSize = 100; expireAfterWrite = 60 }
 *         queryCache { maximumSize = 50; expireAfterWrite = 60 }
 *     }
 * }
 * ```
 *
 * ## 测试流程
 *
 * ```
 * insert → findById(1st, 缓存未命中) → findById(2nd, 缓存命中)
 *        → update(缓存失效) → findById(3rd, 重新加载)
 * ```
 */
object TestCache {

    fun run(sender: ProxyCommandSender) {
        cachedHomeMapper.deleteWhere { "username" eq "cache_user" }

        // 插入测试数据
        // insert 操作会清空 Query Cache，但不影响 Bean Cache
        cachedHomeMapper.insert(PlayerHome("cache_user", "lobby", "world", 1.0, 64.0, 1.0, true))
        sender.sendMessage("§7  insert 完成")

        // 第一次查询 —— 缓存未命中（MISS）
        // Bean Cache 中没有 "cache_user" 的缓存条目
        // 框架从数据库加载数据，返回结果的同时写入 Bean Cache
        val first = cachedHomeMapper.findById("cache_user")
        assert(first != null, "第一次 findById 应返回非空")
        assert(first!!.x == 1.0, "第一次查询 x 应为 1.0")
        sender.sendMessage("§7  第一次查询完成: x=${first.x}")

        // 第二次查询 —— 缓存命中（HIT）
        // Bean Cache 中已有 "cache_user" 的缓存条目
        // 直接返回缓存数据，不查库（可通过日志或调试确认无 SQL 执行）
        val second = cachedHomeMapper.findById("cache_user")
        assert(second != null, "缓存命中 findById 应返回非空")
        assert(second!!.x == 1.0, "缓存命中 x 应为 1.0")
        sender.sendMessage("§7  缓存命中查询完成: x=${second.x}")

        // update —— 写入操作自动失效相关缓存条目
        // 框架自动：
        //   1. 执行 UPDATE SQL
        //   2. 从 Bean Cache 中移除 "cache_user" 的条目
        //   3. 清空整个 Query Cache
        cachedHomeMapper.update(first.copy(x = 99.0))
        sender.sendMessage("§7  update 完成 (缓存应失效)")

        // 第三次查询 —— 缓存已失效，重新从数据库加载
        // Bean Cache 中 "cache_user" 的条目已被移除
        // 框架重新查库，获取更新后的数据（x=99.0），并重新填充缓存
        val third = cachedHomeMapper.findById("cache_user")
        assert(third != null, "缓存失效后 findById 应返回非空")
        assert(third!!.x == 99.0, "缓存失效后 x 应为 99.0, 实际: ${third.x}")
        sender.sendMessage("§7  缓存失效后查询完成: x=${third.x}")

        cachedHomeMapper.deleteWhere { "username" eq "cache_user" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
