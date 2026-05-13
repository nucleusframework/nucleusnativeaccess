plugins {
    alias(libs.plugins.pluginPublish) apply false
    alias(libs.plugins.versionCheck)
}

val resolvedVersion = providers
    .environmentVariable("GITHUB_REF")
    .orNull
    ?.removePrefix("refs/tags/v")
    ?: "0.1.0"

allprojects {
    group = property("GROUP").toString()
    version = resolvedVersion
}

tasks.register<Delete>("clean") {
    description = "Delete the root project build directory"
    delete(rootProject.layout.buildDirectory)
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
