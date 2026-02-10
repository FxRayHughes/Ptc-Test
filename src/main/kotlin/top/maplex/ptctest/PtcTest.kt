package top.maplex.ptctest

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import java.io.File

object PtcTest : Plugin() {

    override fun onEnable() {
        // 清空测试数据库文件，确保每次启动都是干净的测试环境
        arrayOf("test.db", "test_cached.db").forEach { name ->
            val file = File(getDataFolder(), name)
            if (file.exists() && file.delete()) {
                info("已删除测试数据库: $name")
            }
        }
        info("Successfully running Ptc-Test!")
    }
}