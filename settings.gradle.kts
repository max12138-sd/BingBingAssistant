pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 规格书 §2：仅 OpenCV（Maven Central）与 ONNX Runtime；不引入 GMS。
        // 如后续里程碑确需第三方仓库，先在 docs/需求变更记录.md 登记原因。
    }
}

rootProject.name = "BingBingAssistant"
include(":app")
