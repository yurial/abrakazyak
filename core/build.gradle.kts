import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

// Coverage gate: line and branch coverage stay at 100 percent;
// enforced by the koverVerify task.
kover {
    reports {
        verify {
            rule {
                minBound(100, CoverageUnit.LINE, AggregationType.COVERED_PERCENTAGE)
                minBound(100, CoverageUnit.BRANCH, AggregationType.COVERED_PERCENTAGE)
            }
        }
    }
}
