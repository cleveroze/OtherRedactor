plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9 has built-in Kotlin; this only pins the KGP version on the classpath.
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
