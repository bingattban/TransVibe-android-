pluginManagement {
  repositories {
    // Resolve recent KSP versions directly from Maven Central first.
    mavenCentral()
    google()
    gradlePluginPortal()
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "com.google.devtools.ksp") {
        useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")
      }
    }
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    flatDir { dirs("app/libs") }
  }
}

rootProject.name = "TransVibe"

include(":app")
