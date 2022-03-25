
rootProject.name = "QQOnlineCourseBot"

include("app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshot")
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.40.1"
}