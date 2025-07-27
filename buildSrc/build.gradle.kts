plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
    groovy
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-lib:3.3.1")
    implementation("com.diffplug.spotless:spotless-lib-extra:3.3.1")
}