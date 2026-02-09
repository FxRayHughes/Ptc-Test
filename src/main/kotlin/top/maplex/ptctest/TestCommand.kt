package top.maplex.ptctest

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.expansion.subQuery
import top.maplex.ptctest.data.PlayerHome
import top.maplex.ptctest.data.PlayerStats
import top.maplex.ptctest.data.SimpleNote

/**
 * PTC Object 集成测试命令
 *
 * 通过游戏内命令 `/ptctest <子命令>` 触发各项数据库操作测试。
 * 每个子命令对应一个独立的测试方法，测试前后自动清理数据，互不干扰。
 *
 * 子命令列表：
 * - basic    基础 CRUD（insert/findById/findAll/update/exists/deleteById）
 * - column   @ColumnType 长文本存储
 * - autokey  insertAndGetKey / insertBatchAndGetKeys 自增主键获取
 * - key      @Key 复合定位（findByKey/existsByKey/deleteByKey）
 * - rowid    无 @Id 数据类的 rowId 操作（findByRowId/deleteByRowId）
 * - batch    批量操作（insertBatch/findByIds/updateBatch/deleteByIds）
 * - count    计数与存在性检查（count/exists）
 * - sort     排序查询（sort/sortDescending）
 * - sql      自定义 SQL（query/queryOne/rawQuery/rawUpdate/rawDelete）
 * - join     多表联查（innerJoin + selectAs）
 * - advjoin  高级联查（同表自连接 + 子查询 JOIN + 表别名）
 * - tx       事务操作（transaction）
 * - cache    缓存测试（缓存命中 → 写入失效 → 重新加载）
 * - all      依次执行所有测试并汇总结果
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
            sender.sendMessage("§7子命令: basic, column, autokey, key, rowid, batch, count, sort, sql, join, advjoin, tx, cache, all")
        }
    }

    // ==================== 子命令定义 ====================
    // 每个 @CommandBody val 字段名即为子命令名（如 val basic → /ptctest basic）

    /** /ptctest basic —— 基础 CRUD 测试 */
    @CommandBody
    val basic = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "basic") { testBasic(it) } }
    }

    /** /ptctest column —— @ColumnType 长文本测试 */
    @CommandBody
    val column = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "column") { testColumn(it) } }
    }

    /** /ptctest autokey —— insertAndGetKey / insertBatchAndGetKeys 测试 */
    @CommandBody
    val autokey = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "autokey") { testAutoKey(it) } }
    }

    /** /ptctest key —— @Key 复合定位测试 */
    @CommandBody
    val key = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "key") { testKey(it) } }
    }

    /** /ptctest rowid —— 无 @Id 数据类的 rowId 操作测试 */
    @CommandBody
    val rowid = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "rowid") { testRowId(it) } }
    }

    /** /ptctest batch —— 批量操作测试 */
    @CommandBody
    val batch = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "batch") { testBatch(it) } }
    }

    /** /ptctest count —— 计数与存在性检查测试 */
    @CommandBody
    val count = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "count") { testCount(it) } }
    }

    /** /ptctest sort —— 排序查询测试 */
    @CommandBody
    val sort = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "sort") { testSort(it) } }
    }

    /** /ptctest sql —— 自定义 SQL 测试 */
    @CommandBody
    val sql = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "sql") { testSql(it) } }
    }

    /** /ptctest join —— 多表联查测试 */
    @CommandBody
    val join = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "join") { testJoin(it) } }
    }

    /** /ptctest advjoin —— 高级联查测试（同表自连接 + 子查询 JOIN） */
    @CommandBody
    val advjoin = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "advjoin") { testAdvancedJoin(it) } }
    }

    /** /ptctest tx —— 事务操作测试 */
    @CommandBody
    val tx = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "tx") { testTransaction(it) } }
    }

    /** /ptctest cache —— 缓存行为测试 */
    @CommandBody
    val cache = subCommand {
        exec<ProxyCommandSender> { runTest(sender, "cache") { testCache(it) } }
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
            // 测试名 → 测试函数 的有序列表
            val tests = listOf(
                "basic" to { s: ProxyCommandSender -> testBasic(s) },
                "column" to { s: ProxyCommandSender -> testColumn(s) },
                "autokey" to { s: ProxyCommandSender -> testAutoKey(s) },
                "key" to { s: ProxyCommandSender -> testKey(s) },
                "rowid" to { s: ProxyCommandSender -> testRowId(s) },
                "batch" to { s: ProxyCommandSender -> testBatch(s) },
                "count" to { s: ProxyCommandSender -> testCount(s) },
                "sort" to { s: ProxyCommandSender -> testSort(s) },
                "sql" to { s: ProxyCommandSender -> testSql(s) },
                "join" to { s: ProxyCommandSender -> testJoin(s) },
                "advjoin" to { s: ProxyCommandSender -> testAdvancedJoin(s) },
                "tx" to { s: ProxyCommandSender -> testTransaction(s) },
                "cache" to { s: ProxyCommandSender -> testCache(s) },
            )
            var passed = 0
            var failed = 0
            for ((name, test) in tests) {
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
     *
     * @param sender 命令发送者，用于输出测试结果
     * @param name   测试名称，用于日志标识
     * @param block  测试逻辑，抛出异常视为失败
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

    /**
     * 断言工具 —— 条件不满足时抛出 IllegalStateException
     *
     * @param condition 断言条件
     * @param message   失败时的错误描述
     */
    private fun assert(condition: Boolean, message: String) {
        if (!condition) error("断言失败: $message")
    }

    // ==================== 测试实现 ====================

    /**
     * 基础 CRUD 测试
     *
     * 测试流程：insert → findById → findAll → update → exists → deleteById
     * 验证完整的单条记录生命周期。
     */
    private fun testBasic(sender: ProxyCommandSender) {
        // 前置清理：确保测试数据不存在残留
        homeMapper.deleteWhere { "username" eq "test_basic" }

        // 1. insert —— 插入单条记录
        val home = PlayerHome("test_basic", "lobby", "world", 1.0, 2.0, 3.0, true)
        homeMapper.insert(home)
        sender.sendMessage("§7  insert 完成")

        // 2. findById —— 通过 @Id 值查询单条记录
        val found = homeMapper.findById("test_basic")
        assert(found != null, "findById 应返回非空")
        assert(found!!.world == "world", "findById world 应为 world, 实际: ${found.world}")
        assert(found.x == 1.0, "findById x 应为 1.0")
        sender.sendMessage("§7  findById 完成")

        // 3. findAll(id) —— 通过 @Id 值查询所有匹配记录（SQLite 下 @Id 唯一，最多 1 条）
        val allById = homeMapper.findAll("test_basic")
        assert(allById.isNotEmpty(), "findAll(id) 应返回非空列表")
        sender.sendMessage("§7  findAll(id) 完成, 数量: ${allById.size}")

        // 4. update —— 通过 @Id 定位并更新 var 字段
        //    注意：val 字段（username, serverName）不会被更新，只有 var 字段参与 SET 子句
        val updated = found.copy(world = "world_nether", x = 10.0)
        homeMapper.update(updated)
        val afterUpdate = homeMapper.findById("test_basic")
        assert(afterUpdate != null, "update 后 findById 应返回非空")
        assert(afterUpdate!!.world == "world_nether", "update 后 world 应为 world_nether, 实际: ${afterUpdate.world}")
        assert(afterUpdate.x == 10.0, "update 后 x 应为 10.0")
        sender.sendMessage("§7  update 完成")

        // 5. exists —— 通过 @Id 检查记录是否存在
        val ex = homeMapper.exists("test_basic")
        assert(ex, "exists 应返回 true")
        sender.sendMessage("§7  exists 完成")

        // 6. deleteById —— 通过 @Id 删除记录
        homeMapper.deleteById("test_basic")
        val afterDelete = homeMapper.findById("test_basic")
        assert(afterDelete == null, "deleteById 后 findById 应返回 null")
        sender.sendMessage("§7  deleteById 完成")
    }

    /**
     * @ColumnType 注解测试
     *
     * 验证 @ColumnType(sql=TEXT, sqlite=TEXT) 能正确存储超过 VARCHAR 默认长度的长文本。
     * SimpleNote.content 使用 TEXT 类型，可存储任意长度字符串。
     */
    private fun testColumn(sender: ProxyCommandSender) {
        noteMapper.deleteWhere { "title" eq "test_column_title" }

        // 构造 500 字符的长文本，超过 VARCHAR 默认长度 64
        val longContent = "A".repeat(500)
        val note = SimpleNote("test_column_title", longContent, System.currentTimeMillis())
        noteMapper.insert(note)
        sender.sendMessage("§7  insert SimpleNote 完成")

        // findOne —— 无 @Id 数据类通过 filter 条件查询单条
        val found = noteMapper.findOne { "title" eq "test_column_title" }
        assert(found != null, "findOne 应返回非空")
        // 验证长文本完整性：内容未被截断
        assert(found!!.content == longContent, "content 长度应为 500, 实际: ${found.content.length}")
        sender.sendMessage("§7  查询验证完成, content 长度: ${found.content.length}")

        noteMapper.deleteWhere { "title" eq "test_column_title" }
    }

    /**
     * 自增主键获取测试
     *
     * 测试 insertAndGetKey（单条）和 insertBatchAndGetKeys（批量）。
     * 使用无 @Id 的 SimpleNote，框架自动生成自增 id 列。
     *
     * 已知限制：SQLite 批量插入时 generatedKeys 仅返回最后一条的 ID，
     * 因此 insertBatchAndGetKeys 返回列表长度可能为 1 而非 dataList.size。
     */
    private fun testAutoKey(sender: ProxyCommandSender) {
        noteMapper.deleteWhere { "title" eq "autokey_1" }
        noteMapper.deleteWhere { "title" eq "autokey_2" }
        noteMapper.deleteWhere { "title" eq "autokey_3" }

        // insertAndGetKey —— 插入并返回自增主键值
        val note = SimpleNote("autokey_1", "content_1", System.currentTimeMillis())
        val key = noteMapper.insertAndGetKey(note)
        assert(key > 0, "insertAndGetKey 应返回正数, 实际: $key")
        sender.sendMessage("§7  insertAndGetKey 返回: $key")

        // insertBatchAndGetKeys —— 批量插入并返回自增主键列表
        val notes = listOf(
            SimpleNote("autokey_2", "content_2", System.currentTimeMillis()),
            SimpleNote("autokey_3", "content_3", System.currentTimeMillis())
        )
        val keys = noteMapper.insertBatchAndGetKeys(notes)
        // ⚠ SQLite 限制：批量插入可能只返回最后一个 key，这里只断言非空
        assert(keys.isNotEmpty(), "insertBatchAndGetKeys 应返回非空列表, 实际: ${keys.size}")
        assert(keys.all { it > 0 }, "所有 key 应为正数")
        sender.sendMessage("§7  insertBatchAndGetKeys 返回: $keys (数量: ${keys.size})")

        // 额外验证：通过 findOne 确认批量插入的数据确实存在
        val found2 = noteMapper.findOne { "title" eq "autokey_2" }
        val found3 = noteMapper.findOne { "title" eq "autokey_3" }
        assert(found2 != null, "autokey_2 应存在")
        assert(found3 != null, "autokey_3 应存在")
        sender.sendMessage("§7  批量插入数据验证完成")

        // 清理：用 deleteByRowId 和 deleteWhere 两种方式
        noteMapper.deleteByRowId(key)
        noteMapper.deleteWhere { "title" eq "autokey_2" }
        noteMapper.deleteWhere { "title" eq "autokey_3" }
    }

    /**
     * @Key 复合定位测试
     *
     * 测试 findByKey / existsByKey / deleteByKey，这些方法通过 @Id + @Key 组合定位记录。
     *
     * 注意：SQLite 下 @Id 是 PRIMARY KEY（唯一约束），因此每条记录必须使用不同的 username。
     * @Key 复合定位（同一 @Id + 不同 @Key 区分多条记录）仅在 MySQL 模式下可用。
     */
    private fun testKey(sender: ProxyCommandSender) {
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

        // findByKey —— 构造探针对象，框架提取其 @Id(username) + @Key(serverName) 作为 WHERE 条件
        //   只有 username 和 serverName 参与查询，其他字段值无关紧要
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
        homeMapper.deleteByKey(probe)
        val afterDelete = homeMapper.existsByKey(probe)
        assert(!afterDelete, "deleteByKey 后 existsByKey 应返回 false")
        sender.sendMessage("§7  deleteByKey 完成")

        // 验证 deleteByKey 只删除了目标记录，其他记录不受影响
        val r1 = homeMapper.exists("key_user_1")
        val r3 = homeMapper.exists("key_user_3")
        assert(r1, "key_user_1 应仍存在")
        assert(r3, "key_user_3 应仍存在")
        sender.sendMessage("§7  其他记录未受影响")

        homeMapper.deleteWhere { "username" eq "key_user_1" }
        homeMapper.deleteWhere { "username" eq "key_user_3" }
    }

    /**
     * 无 @Id 数据类的 rowId 操作测试
     *
     * SimpleNote 没有 @Id 注解，框架自动添加自增 `id` 列。
     * 通过 insertAndGetKey 获取自增 ID，再用 findByRowId / deleteByRowId 操作。
     */
    private fun testRowId(sender: ProxyCommandSender) {
        noteMapper.deleteWhere { "title" eq "rowid_test" }

        // insertAndGetKey —— 获取框架自动生成的自增 rowId
        val note = SimpleNote("rowid_test", "rowid content", System.currentTimeMillis())
        val rowId = noteMapper.insertAndGetKey(note)
        assert(rowId > 0, "insertAndGetKey 应返回正数, 实际: $rowId")
        sender.sendMessage("§7  insertAndGetKey 返回 rowId: $rowId")

        // findByRowId —— 通过自增 ID 查询
        val found = noteMapper.findByRowId(rowId)
        assert(found != null, "findByRowId 应返回非空")
        assert(found!!.title == "rowid_test", "findByRowId title 应为 rowid_test")
        sender.sendMessage("§7  findByRowId 完成: ${found.title}")

        // deleteByRowId —— 通过自增 ID 删除
        noteMapper.deleteByRowId(rowId)
        val afterDelete = noteMapper.findByRowId(rowId)
        assert(afterDelete == null, "deleteByRowId 后 findByRowId 应返回 null")
        sender.sendMessage("§7  deleteByRowId 完成")
    }

    /**
     * 批量操作测试
     *
     * 测试 insertBatch / findByIds / updateBatch / deleteByIds。
     * 批量操作使用 batch PreparedStatement，性能优于逐条操作。
     */
    private fun testBatch(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "batch_user_1" }
        homeMapper.deleteWhere { "username" eq "batch_user_2" }
        homeMapper.deleteWhere { "username" eq "batch_user_3" }

        // insertBatch —— 一次性插入多条记录
        val homes = listOf(
            PlayerHome("batch_user_1", "lobby", "world", 1.0, 64.0, 1.0, true),
            PlayerHome("batch_user_2", "lobby", "world", 2.0, 64.0, 2.0, true),
            PlayerHome("batch_user_3", "lobby", "world", 3.0, 64.0, 3.0, false)
        )
        homeMapper.insertBatch(homes)
        sender.sendMessage("§7  insertBatch 完成")

        // findByIds —— 通过多个 @Id 值批量查询（生成 IN 子句）
        val found = homeMapper.findByIds(listOf("batch_user_1", "batch_user_2", "batch_user_3"))
        assert(found.size == 3, "findByIds 应返回 3 条, 实际: ${found.size}")
        sender.sendMessage("§7  findByIds 完成, 数量: ${found.size}")

        // updateBatch —— 批量更新，通过 @Id + @Key 定位每条记录
        val updated = found.map { it.copy(world = "world_end") }
        homeMapper.updateBatch(updated)
        val afterUpdate = homeMapper.findByIds(listOf("batch_user_1", "batch_user_2"))
        assert(afterUpdate.all { it.world == "world_end" }, "updateBatch 后 world 应全为 world_end")
        sender.sendMessage("§7  updateBatch 完成")

        // deleteByIds —— 通过多个 @Id 值批量删除（生成 IN 子句）
        homeMapper.deleteByIds(listOf("batch_user_1", "batch_user_2", "batch_user_3"))
        val afterDelete = homeMapper.findByIds(listOf("batch_user_1", "batch_user_2", "batch_user_3"))
        assert(afterDelete.isEmpty(), "deleteByIds 后应为空, 实际: ${afterDelete.size}")
        sender.sendMessage("§7  deleteByIds 完成")
    }

    /**
     * 计数与存在性检查测试
     *
     * 测试 count(filter) / exists(id) / exists(filter)。
     * 注意：SQLite 下 @Id 唯一，因此用不同 username 插入多条，通过 world 字段做分组过滤。
     */
    private fun testCount(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "count_1" }
        homeMapper.deleteWhere { "username" eq "count_2" }
        homeMapper.deleteWhere { "username" eq "count_3" }

        // 插入 3 条记录，共享 world="count_world" 用于分组过滤
        homeMapper.insert(PlayerHome("count_1", "lobby", "count_world", 0.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("count_2", "survival", "count_world", 0.0, 0.0, 0.0, false))
        homeMapper.insert(PlayerHome("count_3", "creative", "count_world", 0.0, 0.0, 0.0, true))

        // count(filter) —— 统计满足条件的记录数
        val total = homeMapper.count { "world" eq "count_world" }
        assert(total == 3L, "count 应为 3, 实际: $total")
        sender.sendMessage("§7  count 完成: $total")

        // count(filter) —— 多条件组合（Filter 中多个条件默认 AND 连接）
        val activeCount = homeMapper.count { "world" eq "count_world"; "active" eq true }
        assert(activeCount == 2L, "active count 应为 2, 实际: $activeCount")
        sender.sendMessage("§7  count(filter) 完成: $activeCount")

        // exists(id) —— 通过 @Id 检查存在性
        val ex1 = homeMapper.exists("count_1")
        assert(ex1, "exists(id) 应返回 true")
        sender.sendMessage("§7  exists(id) 完成: $ex1")

        // exists(filter) —— 通过自定义条件检查存在性
        val ex2 = homeMapper.exists { "world" eq "count_world"; "active" eq false }
        assert(ex2, "exists(filter) 应返回 true")
        val ex3 = homeMapper.exists { "world" eq "count_world_none" }
        assert(!ex3, "exists(filter) 对不存在的 world 应返回 false")
        sender.sendMessage("§7  exists(filter) 完成")

        homeMapper.deleteWhere { "username" eq "count_1" }
        homeMapper.deleteWhere { "username" eq "count_2" }
        homeMapper.deleteWhere { "username" eq "count_3" }
    }

    /**
     * 排序查询测试
     *
     * 测试 sort(row, limit, filter) 和 sortDescending(row, limit, filter)。
     * sort 按指定列升序排列，sortDescending 降序排列，均支持 limit 和 filter。
     */
    private fun testSort(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "sort_1" }
        homeMapper.deleteWhere { "username" eq "sort_2" }
        homeMapper.deleteWhere { "username" eq "sort_3" }
        homeMapper.deleteWhere { "username" eq "sort_4" }
        homeMapper.deleteWhere { "username" eq "sort_5" }

        // 插入 5 条记录，x 值乱序：10, 30, 20, 50, 40
        homeMapper.insert(PlayerHome("sort_1", "s1", "sort_world", 10.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("sort_2", "s2", "sort_world", 30.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("sort_3", "s3", "sort_world", 20.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("sort_4", "s4", "sort_world", 50.0, 0.0, 0.0, true))
        homeMapper.insert(PlayerHome("sort_5", "s5", "sort_world", 40.0, 0.0, 0.0, true))

        // sort("x", 3) —— 按 x 升序取前 3 条，期望 10, 20, 30
        val asc = homeMapper.sort("x", 3) { "world" eq "sort_world" }
        assert(asc.size == 3, "sort 应返回 3 条, 实际: ${asc.size}")
        assert(asc[0].x == 10.0, "sort 第一条 x 应为 10.0, 实际: ${asc[0].x}")
        assert(asc[1].x == 20.0, "sort 第二条 x 应为 20.0, 实际: ${asc[1].x}")
        assert(asc[2].x == 30.0, "sort 第三条 x 应为 30.0, 实际: ${asc[2].x}")
        sender.sendMessage("§7  sort 完成: ${asc.map { it.x }}")

        // sortDescending("x", 3) —— 按 x 降序取前 3 条，期望 50, 40, 30
        val desc = homeMapper.sortDescending("x", 3) { "world" eq "sort_world" }
        assert(desc.size == 3, "sortDescending 应返回 3 条, 实际: ${desc.size}")
        assert(desc[0].x == 50.0, "sortDescending 第一条 x 应为 50.0, 实际: ${desc[0].x}")
        assert(desc[1].x == 40.0, "sortDescending 第二条 x 应为 40.0, 实际: ${desc[1].x}")
        assert(desc[2].x == 30.0, "sortDescending 第三条 x 应为 30.0, 实际: ${desc[2].x}")
        sender.sendMessage("§7  sortDescending 完成: ${desc.map { it.x }}")

        homeMapper.deleteWhere { "world" eq "sort_world" }
    }

    /**
     * 自定义 SQL 测试
     *
     * 测试 query / queryOne / rawQuery / rawUpdate / rawDelete。
     * - query/queryOne：自定义 SELECT，结果自动映射为数据类
     * - rawQuery：自定义 SELECT，手动处理 ResultSet
     * - rawUpdate：自定义 UPDATE，返回受影响行数
     * - rawDelete：自定义 DELETE，返回受影响行数
     */
    private fun testSql(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "sql_user_1" }
        homeMapper.deleteWhere { "username" eq "sql_user_2" }

        homeMapper.insert(PlayerHome("sql_user_1", "lobby", "sql_world", 1.0, 2.0, 3.0, true))
        homeMapper.insert(PlayerHome("sql_user_2", "survival", "sql_world", 4.0, 5.0, 6.0, false))

        // query —— 自定义 SELECT，结果自动映射为 PlayerHome 列表
        //   ActionSelect DSL：where {} 设置条件，limit() 设置数量限制
        val results = homeMapper.query {
            where { "world" eq "sql_world" }
        }
        assert(results.size == 2, "query 应返回 2 条, 实际: ${results.size}")
        sender.sendMessage("§7  query 完成, 数量: ${results.size}")

        // queryOne —— 自定义 SELECT 查询单条
        val one = homeMapper.queryOne {
            where { "username" eq "sql_user_1" }
        }
        assert(one != null, "queryOne 应返回非空")
        assert(one!!.serverName == "lobby", "queryOne serverName 应为 lobby")
        sender.sendMessage("§7  queryOne 完成: ${one.serverName}")

        // rawQuery —— 自定义 SELECT + 手动处理 ResultSet
        //   第一个 lambda 构建 ActionSelect，第二个 lambda 处理 ResultSet
        val worlds = homeMapper.rawQuery({
            rows("world")                          // 只查询 world 列
            where { "world" eq "sql_world" }
        }) { rs ->
            buildList {
                while (rs.next()) add(rs.getString("world"))
            }
        }
        assert(worlds.size == 2, "rawQuery 应返回 2 条, 实际: ${worlds.size}")
        sender.sendMessage("§7  rawQuery 完成: $worlds")

        // rawUpdate —— 自定义 UPDATE，set() 设置新值，where {} 设置条件
        val affected = homeMapper.rawUpdate {
            set("active", true)                    // SET active = true
            where { "username" eq "sql_user_2" }   // WHERE username = 'sql_user_2'
        }
        assert(affected == 1, "rawUpdate 应影响 1 行, 实际: $affected")
        // 验证更新结果
        val verified = homeMapper.findById("sql_user_2")
        assert(verified != null && verified.active, "rawUpdate 后 active 应为 true")
        sender.sendMessage("§7  rawUpdate 完成, 影响行数: $affected")

        // rawDelete —— 自定义 DELETE，where {} 设置条件
        val deleted = homeMapper.rawDelete {
            where { "username" eq "sql_user_2" }
        }
        assert(deleted == 1, "rawDelete 应影响 1 行, 实际: $deleted")
        sender.sendMessage("§7  rawDelete 完成, 影响行数: $deleted")

        homeMapper.deleteWhere { "username" eq "sql_user_1" }
    }

    /**
     * 多表联查测试
     *
     * 测试 DataMapper.join {} DSL，通过 INNER JOIN 关联 player_home 和 player_stats 表。
     * 两个 mapper 使用同一个 test.db 文件，表在同一数据库中，可以直接联查。
     *
     * selectAs 用于解决多表同名列冲突：指定 "表名.列名" to "别名"，
     * 别名作为 BundleMap 的 key。
     */
    private fun testJoin(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "join_user" }
        statsMapper.deleteWhere { "username" eq "join_user" }

        // 分别向两张表插入关联数据
        homeMapper.insert(PlayerHome("join_user", "lobby", "world", 1.0, 64.0, 1.0, true))
        statsMapper.insert(PlayerStats("join_user", 100, 50, 36000L))
        sender.sendMessage("§7  插入 home + stats 完成")

        // join DSL：
        //   - homeMapper.join {} 自动将 player_home 设为主表
        //   - innerJoin<PlayerStats> {} 关联 player_stats 表
        //   - on() 指定连接条件，pre() 表示引用列名而非占位符值
        //   - selectAs() 指定查询列及别名，避免同名列冲突
        val results = homeMapper.join {
            innerJoin<PlayerStats> {
                on("player_home.username" eq pre("player_stats.username"))
            }
            selectAs(
                "player_home.username" to "username",
                "player_home.world" to "world",
                "player_stats.kills" to "kills",
                "player_stats.deaths" to "deaths"
            )
            where { "player_home.username" eq "join_user" }
        }.execute()  // execute() 返回 List<BundleMap>

        assert(results.isNotEmpty(), "join 应返回非空结果")
        val row = results[0]
        // BundleMap.get<T>() 需要显式指定类型参数
        assert(row.get<Any>("username").toString() == "join_user", "join username 应为 join_user")
        assert(row.get<Any>("kills").toString().toInt() == 100, "join kills 应为 100")
        sender.sendMessage("§7  join 完成, 结果: username=${row.get<Any>("username")}, kills=${row.get<Any>("kills")}, deaths=${row.get<Any>("deaths")}")

        homeMapper.deleteWhere { "username" eq "join_user" }
        statsMapper.deleteWhere { "username" eq "join_user" }
    }

    /**
     * 高级联查测试：同表自连接 + 子查询 JOIN
     *
     * 场景 1 - 同表自连接（Self-Join）：
     *   player_home AS h1 INNER JOIN player_home AS h2
     *   找出同一个 world 中的不同玩家对。
     *   关键：同一张表 join 两次必须用不同别名，且不能用泛型重载，
     *   必须用 innerJoin(name: String) 传入预格式化的 "`表名` AS `别名`"。
     *
     * 场景 2 - 子查询 JOIN（DSL）：
     *   player_home AS h INNER JOIN (SELECT ... GROUP BY ...) AS sub
     *   使用 subQuery() DSL 构建子查询，复用 ActionSelect 的能力，
     *   自动处理参数收集和绑定。
     *
     * 场景 3 - 子查询带参数绑定：
     *   子查询内部的 WHERE 条件使用 ? 占位符，参数通过 SubQuery 自动传递。
     */
    private fun testAdvancedJoin(sender: ProxyCommandSender) {
        // 清理测试数据
        listOf("adv_alice", "adv_bob", "adv_carol").forEach {
            homeMapper.deleteWhere { "username" eq it }
            statsMapper.deleteWhere { "username" eq it }
        }

        // 准备数据：3 个玩家，alice 和 bob 在 adv_world_1，carol 在 adv_world_2
        homeMapper.insert(PlayerHome("adv_alice", "s1", "adv_world_1", 10.0, 64.0, 10.0, true))
        homeMapper.insert(PlayerHome("adv_bob", "s1", "adv_world_1", 20.0, 64.0, 20.0, true))
        homeMapper.insert(PlayerHome("adv_carol", "s1", "adv_world_2", 30.0, 64.0, 30.0, true))
        statsMapper.insert(PlayerStats("adv_alice", 100, 10, 3600L))
        statsMapper.insert(PlayerStats("adv_bob", 200, 20, 7200L))
        statsMapper.insert(PlayerStats("adv_carol", 50, 5, 1800L))
        sender.sendMessage("§7  准备数据完成 (3 homes + 3 stats)")

        // === 场景 1：同表自连接 ===
        // 目标 SQL:
        //   SELECT `h1`.`username` AS `user1`, `h2`.`username` AS `user2`, `h1`.`world` AS `world`
        //   FROM `player_home` AS `h1`
        //   INNER JOIN `player_home` AS `h2`
        //     ON `h1`.`world` = `h2`.`world` AND `h1`.`username` < `h2`.`username`
        //   WHERE `h1`.`world` LIKE ?
        //
        // 要点：
        //   - from() 和 innerJoin() 都用 "`表名` AS `别名`" 格式
        //   - lt + pre() 实现列与列的 < 比较，避免 (A,B)/(B,A) 重复对
        //   - where/selectAs 中统一用别名 h1/h2 引用
        val selfJoinResults = homeMapper.join {
            from("`player_home` AS `h1`")
            innerJoin("`player_home` AS `h2`") {
                on("h1.world" eq pre("h2.world"))
                on("h1.username" lt pre("h2.username"))
            }
            selectAs(
                "h1.username" to "user1",
                "h2.username" to "user2",
                "h1.world" to "world"
            )
            where { "h1.world" like "adv_world%" }
        }.execute()

        // alice < bob 且同在 adv_world_1，应找到 1 对
        assert(selfJoinResults.size == 1, "自连接应返回 1 对, 实际: ${selfJoinResults.size}")
        val pair = selfJoinResults[0]
        sender.sendMessage("§7  自连接结果: ${pair.get<Any>("user1")} + ${pair.get<Any>("user2")} in ${pair.get<Any>("world")}")

        // === 场景 2：子查询 JOIN（DSL 版） ===
        // 目标 SQL:
        //   SELECT `h`.`username` AS `username`, `h`.`world` AS `world`,
        //          `sub`.`total_kills` AS `total_kills`, `sub`.`total_deaths` AS `total_deaths`
        //   FROM `player_home` AS `h`
        //   INNER JOIN (
        //       SELECT `username`, SUM(kills) AS total_kills, SUM(deaths) AS total_deaths
        //       FROM `player_stats` GROUP BY `username`
        //   ) AS `sub` ON `h`.`username` = `sub`.`username`
        //   WHERE `h`.`world` = ?
        //
        // 要点：
        //   - subQuery("表名", "别名") { DSL } 复用 ActionSelect 构建子查询
        //   - 子查询的 SQL 和参数由框架自动生成和收集
        //   - 外层通过别名 "sub" 引用子查询的列
        val subqueryJoin = homeMapper.join {
            from("`player_home` AS `h`")
            innerJoin(
                subQuery("player_stats", "sub") {
                    rows("username", "SUM(kills) AS total_kills", "SUM(deaths) AS total_deaths")
                    groupBy("username")
                }
            ) {
                on("h.username" eq pre("sub.username"))
            }
            selectAs(
                "h.username" to "username",
                "h.world" to "world",
                "sub.total_kills" to "total_kills",
                "sub.total_deaths" to "total_deaths"
            )
            where { "h.world" eq "adv_world_1" }
        }.execute()

        // adv_world_1 有 alice 和 bob，各自有 stats 记录
        assert(subqueryJoin.size == 2, "子查询 JOIN 应返回 2 条, 实际: ${subqueryJoin.size}")
        for (row in subqueryJoin) {
            val name = row.get<Any>("username")
            val kills = row.get<Any>("total_kills")
            val deaths = row.get<Any>("total_deaths")
            sender.sendMessage("§7  子查询结果: $name kills=$kills deaths=$deaths")
        }

        // === 场景 3：子查询带参数绑定 ===
        // 目标 SQL:
        //   SELECT `h`.`username` AS `username`, `sub`.`total_kills` AS `total_kills`
        //   FROM `player_home` AS `h`
        //   INNER JOIN (
        //       SELECT `username`, SUM(kills) AS total_kills
        //       FROM `player_stats` WHERE `kills` > ? GROUP BY `username`
        //   ) AS `sub` ON `h`.`username` = `sub`.`username`
        //   WHERE `h`.`world` = ?
        //
        // 要点：
        //   - 子查询内部 where { "kills" gt 50 } 生成 ? 占位符
        //   - SubQuery 自动收集参数，框架按正确顺序绑定：子查询参数(50) → ON参数(无) → WHERE参数("adv_world_1")
        val paramResults = homeMapper.join {
            from("`player_home` AS `h`")
            innerJoin(
                subQuery("player_stats", "sub") {
                    rows("username", "SUM(kills) AS total_kills")
                    where { "kills" gt 50 }
                    groupBy("username")
                }
            ) {
                on("h.username" eq pre("sub.username"))
            }
            selectAs(
                "h.username" to "username",
                "sub.total_kills" to "total_kills"
            )
            where { "h.world" eq "adv_world_1" }
        }.execute()

        // alice(kills=100) 和 bob(kills=200) 满足 kills>50 且在 adv_world_1
        // carol(kills=50) 不满足 kills>50
        assert(paramResults.size == 2, "子查询参数绑定应返回 2 条, 实际: ${paramResults.size}")
        for (row in paramResults) {
            sender.sendMessage("§7  参数绑定结果: ${row.get<Any>("username")} total_kills=${row.get<Any>("total_kills")}")
        }

        // 清理
        listOf("adv_alice", "adv_bob", "adv_carol").forEach {
            homeMapper.deleteWhere { "username" eq it }
            statsMapper.deleteWhere { "username" eq it }
        }
    }

    /**
     * 事务操作测试
     *
     * 测试 DataMapper.transaction {}，事务内的所有操作要么全部提交，要么全部回滚。
     * transaction 返回 Result<R>，可通过 isSuccess / getOrNull() 检查结果。
     *
     * 事务内的 this 是一个 DataMapper<T>，可直接调用 insert/update/findById 等方法。
     */
    private fun testTransaction(sender: ProxyCommandSender) {
        homeMapper.deleteWhere { "username" eq "tx_user_1" }
        homeMapper.deleteWhere { "username" eq "tx_user_2" }

        // transaction {} —— 事务块，返回 Result<Int>
        val result = homeMapper.transaction {
            // 事务内插入两条记录
            insert(PlayerHome("tx_user_1", "lobby", "world", 0.0, 64.0, 0.0, true))
            insert(PlayerHome("tx_user_2", "survival", "world", 100.0, 64.0, 100.0, false))

            // 事务内查询并更新
            val home = findById("tx_user_1")
            if (home != null) {
                update(home.copy(world = "world_nether"))
            }

            // 事务内验证：两条记录都应存在
            val r1 = findById("tx_user_1")
            val r2 = findById("tx_user_2")
            (if (r1 != null) 1 else 0) + (if (r2 != null) 1 else 0)
        }

        // 检查事务执行结果
        assert(result.isSuccess, "事务应成功, 错误: ${result.exceptionOrNull()?.message}")
        assert(result.getOrNull() == 2, "事务内应找到 2 条记录, 实际: ${result.getOrNull()}")
        sender.sendMessage("§7  transaction 完成, 结果: ${result.getOrNull()}")

        // 事务外验证：update 的效果应已持久化
        val afterTx = homeMapper.findById("tx_user_1")
        assert(afterTx != null, "事务后 findById 应返回非空")
        assert(afterTx!!.world == "world_nether", "事务后 world 应为 world_nether, 实际: ${afterTx.world}")
        sender.sendMessage("§7  事务外验证完成: world=${afterTx.world}")

        homeMapper.deleteWhere { "username" eq "tx_user_1" }
        homeMapper.deleteWhere { "username" eq "tx_user_2" }
    }

    /**
     * 缓存行为测试
     *
     * 使用 cachedHomeMapper（配置了 cache { maximumSize=100; expireAfterWrite=60 }）。
     * 验证缓存的三个核心行为：
     * 1. 首次查询 → 从数据库加载并填充缓存
     * 2. 再次查询 → 命中缓存，不查库
     * 3. 写入操作 → 自动失效缓存，下次查询重新从数据库加载
     */
    private fun testCache(sender: ProxyCommandSender) {
        cachedHomeMapper.deleteWhere { "username" eq "cache_user" }

        // 插入测试数据
        cachedHomeMapper.insert(PlayerHome("cache_user", "lobby", "world", 1.0, 64.0, 1.0, true))
        sender.sendMessage("§7  insert 完成")

        // 第一次查询 —— 缓存未命中，从数据库加载，结果写入缓存
        val first = cachedHomeMapper.findById("cache_user")
        assert(first != null, "第一次 findById 应返回非空")
        assert(first!!.x == 1.0, "第一次查询 x 应为 1.0")
        sender.sendMessage("§7  第一次查询完成: x=${first.x}")

        // 第二次查询 —— 缓存命中，直接返回缓存数据，不查库
        val second = cachedHomeMapper.findById("cache_user")
        assert(second != null, "缓存命中 findById 应返回非空")
        assert(second!!.x == 1.0, "缓存命中 x 应为 1.0")
        sender.sendMessage("§7  缓存命中查询完成: x=${second.x}")

        // update —— 写入操作自动失效相关缓存条目
        cachedHomeMapper.update(first.copy(x = 99.0))
        sender.sendMessage("§7  update 完成 (缓存应失效)")

        // 第三次查询 —— 缓存已失效，重新从数据库加载新数据
        val third = cachedHomeMapper.findById("cache_user")
        assert(third != null, "缓存失效后 findById 应返回非空")
        assert(third!!.x == 99.0, "缓存失效后 x 应为 99.0, 实际: ${third.x}")
        sender.sendMessage("§7  缓存失效后查询完成: x=${third.x}")

        cachedHomeMapper.deleteWhere { "username" eq "cache_user" }
    }
}