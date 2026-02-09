import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("io.izzel.taboolib") version "2.0.30"
    kotlin("jvm") version "2.3.0"
}

taboolib {
    env {
        // 安装模块
        install(Basic, Bukkit, BukkitHook, BukkitNMSUtil)
        install(BukkitUI)           // UI界面
        install(Database)           // 数据库
        install(Kether)             // 条件判断
        install(CommandHelper)      // 命令系统
        install(PtcObject)
        repoTabooLib = project.repositories.mavenLocal().url.toString()
    }
    version {
        taboolib = "6.2.4-local-dev"
        coroutines = "1.8.1"
    }
    // relocate("top.maplex.arim","top.maplex.youerproject.arim") 工具库重定向
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        // 枫溪的仓库
        url = uri("https://nexus.maplex.top/repository/maven-public/")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    // taboo("top.maplex.arim:Arim:1.3.2") 小工具库

    compileOnly("com.google.code.gson:gson:2.8.8")

    //compileOnly("ink.ptms.core:v12105:12105-minimize")

    // or
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("1.8")
    }
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
