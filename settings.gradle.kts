pluginManagement {
  repositories {
    // Google supplies AGP; the plugin portal resolves Android's marker.
    // KSP alone is mapped below to the published Maven Central module.
    gradlePluginPortal()
    google()
    mavenCentral()
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
