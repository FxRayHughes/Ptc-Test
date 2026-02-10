package top.maplex.ptctest.test

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.warning
import top.maplex.ptctest.accountMapper
import top.maplex.ptctest.data.AccountData
import top.maplex.ptctest.data.AccountType

/**
 * IndexedEnum 枚举索引测试
 *
 * 本测试验证实现了 [taboolib.expansion.IndexedEnum] 接口的枚举类型，
 * 在数据库中以数值（index）存储而非枚举名称字符串。
 *
 * ## 核心特性
 *
 * - **数值存储**：`AccountType.VIP` 在数据库中存储为 `2`，而非 `"VIP"` 字符串
 * - **自动转换**：读取时自动将数值 `2` 转换回 `AccountType.VIP` 枚举常量
 * - **WHERE 条件**：`"type" eq AccountType.VIP` 自动使用 `index` 值拼接 SQL
 * - **列类型**：MySQL 使用 `BIGINT`，SQLite 使用 `INTEGER`
 *
 * ## 对比普通枚举
 *
 * | 特性       | 普通枚举（默认）        | IndexedEnum            |
 * |-----------|----------------------|------------------------|
 * | 存储值     | `"VIP"`（字符串）      | `2`（数值）             |
 * | 列类型     | VARCHAR / TEXT        | BIGINT / INTEGER       |
 * | 查询条件   | `WHERE type = 'VIP'` | `WHERE type = 2`       |
 *
 * ## 生成的 SQL（SQLite）
 *
 * ```sql
 * -- 插入
 * INSERT INTO `account_data` (`username`, `type`, `score`) VALUES (?, ?, ?)
 * -- 参数: ["user1", 2, 100.0]  （type 存储为数值 2）
 *
 * -- 按枚举条件查询
 * SELECT * FROM `account_data` WHERE `type` = ?
 * -- 参数: [2]  （自动使用 index 值）
 * ```
 */
object TestIndexedEnum {

    fun run(sender: ProxyCommandSender) {
        // 清理测试数据
        accountMapper.deleteWhere { "username" eq "ie_1" }
        accountMapper.deleteWhere { "username" eq "ie_2" }
        accountMapper.deleteWhere { "username" eq "ie_3" }

        // === 插入 IndexedEnum 数据 ===
        accountMapper.insert(AccountData("ie_1", AccountType.NORMAL, 50.0))
        accountMapper.insert(AccountData("ie_2", AccountType.VIP, 100.0))
        accountMapper.insert(AccountData("ie_3", AccountType.ADMIN, 200.0))
        sender.sendMessage("§7  插入 3 条 IndexedEnum 数据完成")

        // === 读取并验证枚举值正确还原 ===
        val found1 = accountMapper.findById("ie_1")
        assert(found1 != null, "ie_1 应存在")
        assert(found1!!.type == AccountType.NORMAL, "ie_1 type 应为 NORMAL, 实际: ${found1.type}")
        assert(found1.score == 50.0, "ie_1 score 应为 50.0, 实际: ${found1.score}")
        sender.sendMessage("§7  读取验证完成: ie_1 type=${found1.type} (index=${found1.type.index})")

        val found2 = accountMapper.findById("ie_2")
        assert(found2 != null, "ie_2 应存在")
        assert(found2!!.type == AccountType.VIP, "ie_2 type 应为 VIP, 实际: ${found2.type}")
        sender.sendMessage("§7  读取验证完成: ie_2 type=${found2.type} (index=${found2.type.index})")

        val found3 = accountMapper.findById("ie_3")
        assert(found3 != null, "ie_3 应存在")
        assert(found3!!.type == AccountType.ADMIN, "ie_3 type 应为 ADMIN, 实际: ${found3.type}")
        sender.sendMessage("§7  读取验证完成: ie_3 type=${found3.type} (index=${found3.type.index})")

        // === WHERE 条件中使用枚举值 ===
        // "type" eq AccountType.VIP 应自动使用 index=2 进行查询
        val vipList = accountMapper.findAll { "type" eq AccountType.VIP }
        assert(vipList.size == 1, "VIP 应有 1 条, 实际: ${vipList.size}")
        assert(vipList[0].username == "ie_2", "VIP 用户应为 ie_2, 实际: ${vipList[0].username}")
        sender.sendMessage("§7  WHERE 枚举条件查询完成: ${vipList.size} 条 VIP")

        // === 更新枚举字段 ===
        found1.type = AccountType.VIP
        accountMapper.update(found1)
        val updated = accountMapper.findById("ie_1")
        assert(updated!!.type == AccountType.VIP, "更新后 ie_1 type 应为 VIP, 实际: ${updated.type}")
        sender.sendMessage("§7  更新枚举字段完成: ie_1 type=${updated.type}")

        // 再次查询 VIP，应有 2 条
        val vipList2 = accountMapper.findAll { "type" eq AccountType.VIP }
        assert(vipList2.size == 2, "更新后 VIP 应有 2 条, 实际: ${vipList2.size}")
        sender.sendMessage("§7  更新后 VIP 查询完成: ${vipList2.size} 条")

        // === 排序查询（按 index 数值排序）===
        val sorted = accountMapper.sort("type", 10)
        assert(sorted.size == 3, "排序应返回 3 条, 实际: ${sorted.size}")
        // VIP(2), VIP(2), ADMIN(3) — 两个 VIP 在前，ADMIN 在后
        assert(sorted[0].type == AccountType.VIP, "排序第 1 条应为 VIP, 实际: ${sorted[0].type}")
        assert(sorted[1].type == AccountType.VIP, "排序第 2 条应为 VIP, 实际: ${sorted[1].type}")
        assert(sorted[2].type == AccountType.ADMIN, "排序第 3 条应为 ADMIN, 实际: ${sorted[2].type}")
        sender.sendMessage("§7  排序查询完成: ${sorted.map { "${it.username}(${it.type})" }}")

        // === count 按枚举条件统计 ===
        val vipCount = accountMapper.count { "type" eq AccountType.VIP }
        assert(vipCount == 2L, "VIP 数量应为 2, 实际: $vipCount")
        val adminCount = accountMapper.count { "type" eq AccountType.ADMIN }
        assert(adminCount == 1L, "ADMIN 数量应为 1, 实际: $adminCount")
        sender.sendMessage("§7  计数完成: VIP=$vipCount, ADMIN=$adminCount")

        // 清理测试数据
        accountMapper.deleteWhere { "username" eq "ie_1" }
        accountMapper.deleteWhere { "username" eq "ie_2" }
        accountMapper.deleteWhere { "username" eq "ie_3" }
    }

    private fun assert(condition: Boolean, message: String) {
        if (!condition) warning("断言失败: $message")
    }
}
