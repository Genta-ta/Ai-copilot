import com.google.gson.Gson
import java.net.URL
import com.google.gson.JsonObject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.genta.copilot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.genta.copilot"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Jika ingin mengaktifkan ProGuard, pastikan tambahkan @Keep pada main class agar Xed-Editor bisa menemukannya.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
    buildFeatures {
        compose = true
    }
}

// Dependensi disesuaikan dengan versi yang digunakan di Xed-Editor
dependencies {
    // SDK Ekstensi Xed-Editor, wajib ada untuk interaksi antar aplikasi
    compileOnly(files("libs/sdk.jar"))

    compileOnly(libs.androidx.appcompat)
    compileOnly(libs.material)
    compileOnly(libs.androidx.constraintlayout)
    compileOnly(libs.androidx.navigation.fragment)
    compileOnly(libs.androidx.navigation.ui)
    compileOnly(libs.androidx.navigation.fragment.ktx)
    compileOnly(libs.androidx.navigation.ui.ktx)
    compileOnly(libs.androidx.lifecycle.viewmodel)
    compileOnly(libs.androidx.lifecycle.runtime)
    compileOnly(libs.androidx.activity.compose)
    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly(libs.androidx.compose.ui)
    compileOnly(libs.androidx.compose.ui.graphics)
    compileOnly(libs.androidx.compose.material3)
    compileOnly(libs.androidx.navigation.compose)
    compileOnly(libs.utilcode)
    compileOnly(libs.coil.compose)
    compileOnly(libs.gson)
    compileOnly(libs.commons.net)
    compileOnly(libs.okhttp)
    compileOnly(libs.material.motion.compose)
    compileOnly(libs.nanohttpd)
    compileOnly(libs.photoview)
    compileOnly(libs.glide)
    compileOnly(libs.androidx.browser)
    compileOnly(libs.quickjs.android)
    compileOnly(libs.anrwatchdog)
    compileOnly(libs.lsp4j)
    compileOnly(libs.kotlin.reflect)
    compileOnly(libs.androidx.documentfile)
    compileOnly(libs.compose.dnd)
    compileOnly(libs.androidx.compose.material.icons.core)
    compileOnly(libs.pine.core)
    compileOnly(libs.androidx.lifecycle.process)
    compileOnly(libs.androidsvg.aar)
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}

//  ---------------- Otomatisasi Pembaruan sdk.jar --------------------

val GITHUB_OWNER = "Xed-Editor"
val GITHUB_REPO = "Xed-Editor"
val TAG_NAME = "sdk-latest"
val ASSET_NAME = "sdk.jar"

val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/tags/$TAG_NAME"
val DOWNLOAD_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$TAG_NAME/$ASSET_NAME"

val timestampFile = project.layout.buildDirectory.file("sdk_updated_at.txt")
val outputFile = project.layout.projectDirectory.file("libs/$ASSET_NAME")

tasks.register<DefaultTask>("downloadLatestJar") {
    outputs.upToDateWhen { false }
    description = "Memeriksa dan mengunduh $ASSET_NAME terbaru dari GitHub."
    group = "build"

    outputs.file(outputFile)
    outputs.file(timestampFile)

    doLast {
        outputFile.asFile.parentFile.mkdirs()
        timestampFile.get().asFile.parentFile.mkdirs()

        val remoteUpdatedAt: String
        try {
            val json = URL(API_URL).readText()
            val releaseObj = Gson().fromJson(json, JsonObject::class.java)
            remoteUpdatedAt = releaseObj.get("updated_at").asString
        } catch (e: Exception) {
            logger.error("Gagal mengakses GitHub API di $API_URL", e)
            throw GradleException("Tidak dapat memeriksa timestamp release terbaru.", e)
        }

        val storedUpdatedAt = if (timestampFile.get().asFile.exists()) {
            timestampFile.get().asFile.readText().trim()
        } else {
            null
        }

        if (remoteUpdatedAt == storedUpdatedAt) {
            println("✅ $ASSET_NAME sudah versi terbaru (Timestamp: $remoteUpdatedAt). Unduhan dilewati.")
            return@doLast
        }

        println("Release diperbarui ($storedUpdatedAt -> $remoteUpdatedAt). Mengunduh JAR baru...")

        try {
            URL(DOWNLOAD_URL).openStream().use { inputStream ->
                outputFile.asFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            timestampFile.get().asFile.writeText(remoteUpdatedAt)
            println("Berhasil mengunduh $ASSET_NAME ke ${outputFile.asFile.path}")
        } catch (e: Exception) {
            logger.error("Gagal mengunduh JAR dari $DOWNLOAD_URL", e)
            throw GradleException("Proses unduh gagal.", e)
        }
    }
}

tasks.register<Delete>("cleanApkOutputs") {
    description = "Menghapus semua file hasil build dan subdirektori di dalam folder build/outputs/apk."
    group = "cleanup"
    delete(layout.buildDirectory.dir("outputs/apk"))
}

tasks.named("preBuild").configure {
    dependsOn("cleanApkOutputs")
    dependsOn("downloadLatestJar")
}

// --------------- Pembuatan Dokumen Akhir ZIP -----------------

tasks.register<Zip>("createFinalZip") {
    // Alur wajib: Buat file APK kompilasi terlebih dahulu sebelum dibungkus ke ZIP
    dependsOn("assembleRelease")

    outputs.upToDateWhen { false }
    description = "Membungkus file APK yang dihasilkan beserta komponen lainnya ke dalam satu file ZIP."
    group = "build"

    val manifest = File(rootDir, "manifest.json")
    val iconFile = File(rootDir, "icon.png")
    val readmeFile = File(rootDir, "README.md")
    val changelogFile = File(rootDir, "CHANGELOG.md")

    // Membaca nama ekstensi secara aman saat configuration time
    val extensionName = providers.provider {
        if (manifest.exists()) {
            val text = manifest.readText()
            Gson().fromJson(text, JsonObject::class.java).get("name").asString
        } else {
            "extension"
        }
    }
    archiveFileName.set(extensionName.map { "$it.zip" })
    destinationDirectory.set(File(rootDir, "output"))

    // Memasukkan berkas manifest dan info ke root ZIP
    from(manifest) { into("") }
    from(iconFile) { into("") }
    from(readmeFile) { into("") }
    from(changelogFile) { into("") }

    // Mengambil APK secara dinamis setelah build selesai agar tidak error "File tidak ditemukan"
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("**/*.apk")
        eachFile {
            // Memastikan file APK ditaruh di root dalam ZIP, bukan di dalam folder terpisah
            path = name 
        }
    }

    // Validasi akhir untuk memastikan zip sukses dibuat
    doLast {
        val zipFile = archiveFile.get().asFile
        if (zipFile.exists()) {
            println("🎉 Berhasil! Modul ZIP siap di: ${zipFile.absolutePath}")
        } else {
            throw GradleException("Gagal membuat file ZIP ekstensi.")
        }
    }
}
