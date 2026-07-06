plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.compose") version "1.6.0"
}

repositories {
    google()
    mavenCentral()
    maven("https://jetbrains.space")
}

dependencies {
    implementation(compose.desktop.currentOs)
    // PC 端同样使用 Bouncy Castle 开源密码库来计算证书
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageVersion = "1.0.0"
            description = "自签名证书生成器"
        }
    }
}
