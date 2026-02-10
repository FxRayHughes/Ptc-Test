package top.maplex.ptctest

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import top.maplex.ptctest.test.TestBasic
import top.maplex.ptctest.test.TestColumn
import top.maplex.ptctest.test.TestAutoKey
import top.maplex.ptctest.test.TestKey
import top.maplex.ptctest.test.TestRowId
import top.maplex.ptctest.test.TestBatch
import top.maplex.ptctest.test.TestCount
import top.maplex.ptctest.test.TestSort
import top.maplex.ptctest.test.TestSql
import top.maplex.ptctest.test.TestJoin
import top.maplex.ptctest.test.TestAdvancedJoin
import top.maplex.ptctest.test.TestTransaction
import top.maplex.ptctest.test.TestCache
import top.maplex.ptctest.test.TestLinkTable
import top.maplex.ptctest.test.TestNestedLink
import top.maplex.ptctest.test.TestCustomType
import top.maplex.ptctest.test.TestPage
import top.maplex.ptctest.test.TestCursor
import top.maplex.ptctest.test.TestIndexedEnum
import top.maplex.ptctest.test.TestPostgreSQL

/**
 * PTC Object 集成测试命令
 *
 * 通过游戏内命令 `/ptctest <子命令>` 触发各项数据库操作测试。
 * 每个子命令委托给 `test` 包下的独立测试类执行，测试前后自动清理数据，互不干扰。
 *
 * 子命令列表：
 * - basic      基础 CRUD → [TestBasic]
 * - column     @ColumnType 长文本存储 → [TestColumn]
 * - autokey    自增主键获取 → [TestAutoKey]
 * - key        @Key 复合定位 → [TestKey]
 * - rowid      无 @Id 数据类的 rowId 操作 → [TestRowId]
 * - batch      批量操作 → [TestBatch]
 * - count      计数与存在性检查 → [TestCount]
 * - sort       排序查询 → [TestSort]
 * - sql        自定义 SQL → [TestSql]
 * - join       多表联查 → [TestJoin]
 * - advjoin    高级联查 → [TestAdvancedJoin]
 * - tx         事务操作 → [TestTransaction]
 * - cache      缓存测试 → [TestCache]
 * - linktable  @LinkTable 自动关联表 → [TestLinkTable]
 * - nestedlink @LinkTable 嵌套关联 → [TestNestedLink]
 * - customtype CustomType 自定义类型 → [TestCustomType]
 * - page       分页查询 → [TestPage]
 * - cursor     游标查询 → [TestCursor]
 * - indexenum  IndexedEnum 枚举索引 → [TestIndexedEnum]
 * - postgresql PostgreSQL 集成测试 → [TestPostgreSQL]（需要 PostgreSQL 服务）
 * - all        依次执行所有测试并汇总结果（不含 postgresql）
 *
 * @author Ptc-Test
 */
@CommandHeader(
    name = "ptctest",
    description = "PTC Object 集成测试",
    permissionDefault = PermissionDefault.OP
)
object TestCommand {

    /** 主命令 —— 无参数时显示用法提示 */
    @CommandBody
    val main = mainCommand {
        exec<ProxyCommandSender> {
            sender.sendMessage("§e用法: /ptctest <子命令>")
            sender.sendMessage("§7子命令: basic, column, autokey, key, rowid, batch, count, sort, sql, join, advjoin, tx, cache, linktable, nestedlink, customtype, page, cursor, indexenum, postgresql, all")
        }
    }

    // ==================== 子命令定义 ====================

    @CommandBody
    val basic = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "basic") { TestBasic.run(it) } }
    }

    @CommandBody
    val column = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "column") { TestColumn.run(it) } }
    }

    @CommandBody
    val autokey = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "autokey") { TestAutoKey.run(it) } }
    }

    @CommandBody
    val key = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "key") { TestKey.run(it) } }
    }

    @CommandBody
    val rowid = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "rowid") { TestRowId.run(it) } }
    }

    @CommandBody
    val batch = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "batch") { TestBatch.run(it) } }
    }

    @CommandBody
    val count = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "count") { TestCount.run(it) } }
    }

    @CommandBody
    val sort = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "sort") { TestSort.run(it) } }
    }

    @CommandBody
    val sql = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "sql") { TestSql.run(it) } }
    }

    @CommandBody
    val join = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "join") { TestJoin.run(it) } }
    }

    @CommandBody
    val advjoin = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "advjoin") { TestAdvancedJoin.run(it) } }
    }

    @CommandBody
    val tx = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "tx") { TestTransaction.run(it) } }
    }

    @CommandBody
    val cache = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "cache") { TestCache.run(it) } }
    }

    @CommandBody
    val linktable = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "linktable") { TestLinkTable.run(it) } }
    }

    @CommandBody
    val nestedlink = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "nestedlink") { TestNestedLink.run(it) } }
    }

    @CommandBody
    val customtype = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "customtype") { TestCustomType.run(it) } }
    }

    @CommandBody
    val page = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "page") { TestPage.run(it) } }
    }

    @CommandBody
    val cursor = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "cursor") { TestCursor.run(it) } }
    }

    @CommandBody
    val indexenum = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "indexenum") { TestIndexedEnum.run(it) } }
    }

    @CommandBody
    val postgresql = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "postgresql") { TestPostgreSQL.run(it) } }
    }

    /**
     * /ptctest all —— 依次执行所有测试并汇总结果
     *
     * 每个测试独立 try-catch，单个失败不影响后续测试执行。
     * 最终输出通过/失败/总计统计。
     */
    @CommandBody
    val all = subCommand {
        exec<ProxyCommandSender> {
            sender.sendMessage("§6========== 开始全部测试 ==========")
            val tests = listOf(
                "basic" to { s: ProxyCommandSender -> TestBasic.run(s) },
                "column" to { s: ProxyCommandSender -> TestColumn.run(s) },
                "autokey" to { s: ProxyCommandSender -> TestAutoKey.run(s) },
                "key" to { s: ProxyCommandSender -> TestKey.run(s) },
                "rowid" to { s: ProxyCommandSender -> TestRowId.run(s) },
                "batch" to { s: ProxyCommandSender -> TestBatch.run(s) },
                "count" to { s: ProxyCommandSender -> TestCount.run(s) },
                "sort" to { s: ProxyCommandSender -> TestSort.run(s) },
                "sql" to { s: ProxyCommandSender -> TestSql.run(s) },
                "join" to { s: ProxyCommandSender -> TestJoin.run(s) },
                "advjoin" to { s: ProxyCommandSender -> TestAdvancedJoin.run(s) },
                "tx" to { s: ProxyCommandSender -> TestTransaction.run(s) },
                "cache" to { s: ProxyCommandSender -> TestCache.run(s) },
                "linktable" to { s: ProxyCommandSender -> TestLinkTable.run(s) },
                "nestedlink" to { s: ProxyCommandSender -> TestNestedLink.run(s) },
                "customtype" to { s: ProxyCommandSender -> TestCustomType.run(s) },
                "page" to { s: ProxyCommandSender -> TestPage.run(s) },
                "cursor" to { s: ProxyCommandSender -> TestCursor.run(s) },
                "indexenum" to { s: ProxyCommandSender -> TestIndexedEnum.run(s) },
            )
            var passed = 0
            var failed = 0
            for ((name, test) in tests) {
                sender.sendMessage("§e  开始测试 $name...")
                try {
                    test(sender)
                    sender.sendMessage("§a  ✓ $name")
                    passed++
                } catch (e: Throwable) {
                    sender.sendMessage("§c  ✗ $name: ${e.message}")
                    e.printStackTrace()
                    failed++
                }
            }
            sender.sendMessage("§6========== 测试完成 ==========")
            sender.sendMessage("§a通过: $passed  §c失败: $failed  §7总计: ${passed + failed}")
        }
    }

    // ==================== 辅助函数 ====================

    /**
     * 测试运行器 —— 包装单个测试的执行与异常处理
     */
    private fun runTest(sender: ProxyCommandSender, name: String, block: (ProxyCommandSender) -> Unit) {
        try {
            block(sender)
            sender.sendMessage("§a[$name] 测试通过!")
        } catch (e: Throwable) {
            sender.sendMessage("§c[$name] 测试失败: ${e.message}")
            e.printStackTrace()
        }
    }
}
