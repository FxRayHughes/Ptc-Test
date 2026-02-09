# Ptc-Test

### 项目使用 TabooLib Start Jar 创建!
### 爱来自枫溪

## 构建发行版本

发行版本用于正常使用, 不含 TabooLib 本体。

```bash
./gradlew clean build
```

## 构建开发版本

开发版本包含 TabooLib 本体, 用于开发者使用, 但不可运行。

```bash
./gradlew clean taboolibBuildApi -PDeleteCode
```

> 参数 -PDeleteCode 表示移除所有逻辑代码以减少体积。

## 项目信息

- 包名: 请在 gradle.properties 中查看
- 主类: 请在 src/main/kotlin 中查看

## 开发指南

此项目基于 TabooLib 框架开发，更多信息请访问:
- [TabooLib 官方文档](https://docs.tabooproject.org/)
- [TabooLib GitHub](https://github.com/TabooLib/taboolib)