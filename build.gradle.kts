compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)

            packageName = "ca-generator"
            packageVersion = "1.0.0"
        }
    }
}
