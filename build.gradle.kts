import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
    idea
    id("io.spring.dependency-management") version "1.1.7"
    id("org.springframework.boot") version "4.1.1" apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
}

configure<org.gradle.plugins.ide.idea.model.IdeaModel> {
    project {
        // Передаем строку, чтобы плагин корректно распарсил версию
        languageLevel = IdeaLanguageLevel("25")
    }
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

allprojects {
    group = "org.example.platform"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    // Читаем версии из gradle.properties
    val lombokVersion: String by project
    val jsoupVersion: String by project

    // Применяем плагин централизованного управления зависимостями ко всем проектам
    apply(plugin = "io.spring.dependency-management")

    configure<DependencyManagementExtension> {
        dependencies {
            imports {
                // Главный BOM-файл Spring Boot, управляющий версиями всех стартеров
                mavenBom(BOM_COORDINATES)
            }
            // Регистрируем внешние либы, чтобы не хардкодить их версии в модулях
            dependency("org.projectlombok:lombok:$lombokVersion")
            dependency("org.jsoup:jsoup:$jsoupVersion")
        }
    }
}

subprojects {
    plugins.apply(JavaPlugin::class.java)

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.toVersion(25)
        targetCompatibility = JavaVersion.toVersion(25)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-Werror"))
    }

    // Применяем Spotless
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            // ЯВНО фиксируем версию 1.28.0, совместимую с Java 25
            googleJavaFormat("1.28.0").aosp()
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging.showExceptions = true
    }
}


// Полезная таска из твоего шаблона: выводит в консоль список всех управляемых версий
tasks {
    val managedVersions by registering {
        doLast {
            project.extensions.getByType<DependencyManagementExtension>()
                .managedVersions
                .toSortedMap()
                .map { "${it.key}:${it.value}" }
                .forEach(::println)
        }
    }
}
