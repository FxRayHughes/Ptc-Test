package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.IndexedEnum

/**
 * 实现 [IndexedEnum] 的枚举示例 —— 账户类型
 *
 * 数据库中存储 [index] 数值（1, 2, 3），而非枚举名称字符串。
 *
 * | 枚举常量 | index | 数据库存储值 |
 * |---------|-------|------------|
 * | NORMAL  | 1     | 1          |
 * | VIP     | 2     | 2          |
 * | ADMIN   | 3     | 3          |
 */
enum class AccountType(override val index: Long, val desc: String) : IndexedEnum {
    NORMAL(1, "普通用户"),
    VIP(2, "VIP用户"),
    ADMIN(3, "管理员"),
}

/**
 * 含 [IndexedEnum] 枚举字段的数据类
 *
 * 建表结构（SQLite）：
 * ```sql
 * CREATE TABLE account_data (
 *     username TEXT PRIMARY KEY,
 *     type     INTEGER,          -- IndexedEnum → INTEGER（存储 index 数值）
 *     score    REAL
 * )
 * ```
 */
data class AccountData(
    @Id val username: String,
    var type: AccountType,
    var score: Double
)
