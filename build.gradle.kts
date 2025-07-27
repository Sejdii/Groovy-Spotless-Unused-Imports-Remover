import com.diffplug.spotless.FormatterFunc
import pl.sejdii.groovy.parser.GroovyUnusedImportsRemover
import java.io.Serializable

plugins {
    id("groovy")
    id("com.diffplug.spotless").version("7.0.3")
}

group = "pl.sejdii.groovy.parser"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val spockVersion = "2.3-groovy-4.0"

dependencies {
    implementation("org.apache.groovy:groovy:4.0.14")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.spockframework:spock-core:$spockVersion")
}

buildscript {
    dependencies {
        classpath("com.diffplug.spotless:spotless-lib:3.3.1")
        classpath(files("libs/Groovy-Spotless-Unused-Imports-Remover-1.0-SNAPSHOT.jar"))
    }
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    groovy {
        greclipse()
        excludeJava()

        custom(
            "removeUnusedImports",
            object : Serializable, FormatterFunc {
                override fun apply(input: String): String = GroovyUnusedImportsRemover.removeUnusedImports(input)
            },
        )
    }
    kotlinGradle {
        ktlint()
    }
}
