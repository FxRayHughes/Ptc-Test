package top.maplex.ptctest.data

import taboolib.expansion.Id
import taboolib.expansion.Length

/**
 * 版本迁移测试数据类
 *
 * 初始建表只有 username 和 world 两个字段。
 * 通过 `migration {}` 配置版本迁移，逐步添加 x、y、z 字段。
 *
 * 迁移计划：
 * - version(1): ALTER TABLE 添加 x, y 列
 * - version(2): ALTER TABLE 添加 z 列
 *
 * @property username 玩家名，逻辑主键
 * @property world    世界名
 * @property x        X 坐标（version 1 迁移添加）
 * @property y        Y 坐标（version 1 迁移添加）
 * @property z        Z 坐标（version 2 迁移添加）
 */
data class MigrationHome(
    @Id val username: String,
    @Length(64) var world: String,
    var x: Double,
    var y: Double,
    var z: Double
)
