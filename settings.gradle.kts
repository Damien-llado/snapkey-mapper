pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        // Only for the in-app setup libraries of the "full" edition
        maven("https://jitpack.io") { content { includeGroupByRegex("com\\.github\\..*") } }
    }
}
rootProject.name = "snapkey-mapper"
include(":app")
