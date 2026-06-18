import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.pixies.recetario.MainKt"

        val localProperties = Properties().apply {
            val propertiesFile = rootProject.file("local.properties")
            if (propertiesFile.exists()) {
                propertiesFile.inputStream().use { load(it) }
            }
        }

        val apiKey = localProperties.getProperty("spoonacular.api.key") ?: ""

        jvmArgs("-DSPOONACULAR_API_KEY=$apiKey")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.pixies.recetario"
            packageVersion = "1.0.0"
        }
    }
}