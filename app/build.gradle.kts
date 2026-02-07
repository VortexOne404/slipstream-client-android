import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

val minSdkVersionValue = 24

val cargoProfile = (findProperty("CARGO_PROFILE") as String?) ?: run {
    val isRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
    if (isRelease) "release" else "debug"
}

fun cargoEnvKeyFor(triple: String): String =
    triple.replace('-', '_').uppercase()

fun ndkHostTag(ndkDir: File): String {
    val prebuilt = ndkDir.resolve("toolchains/llvm/prebuilt")
    val host = prebuilt.listFiles()?.firstOrNull { it.isDirectory }?.name
    require(host != null) { "NDK prebuilt toolchain not found under: $prebuilt" }
    return host
}

fun clangFor(triple: String, api: Int, toolchainBin: File): String = when (triple) {
    "aarch64-linux-android" -> toolchainBin.resolve("aarch64-linux-android${api}-clang").absolutePath
    "armv7-linux-androideabi" -> toolchainBin.resolve("armv7a-linux-androideabi${api}-clang").absolutePath
    "i686-linux-android" -> toolchainBin.resolve("i686-linux-android${api}-clang").absolutePath
    "x86_64-linux-android" -> toolchainBin.resolve("x86_64-linux-android${api}-clang").absolutePath
    else -> toolchainBin.resolve("clang").absolutePath
}

val keystorePropsFile = rootProject.file("app/keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}


android {
    namespace = "com.kmk.slipstream.vpn"
    compileSdk = 34
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.kmk.slipstream.vpn"
        minSdk = minSdkVersionValue
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/hev-socks5-tunnel/Android.mk")
        }
    }
    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps.getProperty("storeFile") ?: ""
            if (storeFilePath.isNotBlank()) {
                storeFile = file(storeFilePath)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    packaging {
        jniLibs.useLegacyPackaging = true
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }


}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
}

/* -----------------------------
   OpenSSL submodule build (4 ABIs)
   submodule path:
     app/src/main/rust/third_party/openssl
   output path:
     app/src/main/rust/openssl-out/<abi>/
   ----------------------------- */

data class OpenSslAbi(val abi: String, val opensslTarget: String)
val opensslAbis = listOf(
    OpenSslAbi("arm64-v8a", "android-arm64"),
    OpenSslAbi("armeabi-v7a", "android-arm"),
    OpenSslAbi("x86", "android-x86"),
    OpenSslAbi("x86_64", "android-x86_64"),
)

val opensslSrcDir = file("src/main/rust/third_party/openssl")
val opensslOutDir = file("src/main/rust/openssl-out")

fun opensslTaskName(abi: String) = "buildOpenSsl" + abi.replace("-", "").replace("_", "")

opensslAbis.forEach { a ->
    tasks.register(opensslTaskName(a.abi)) {
        group = "native"
        description = "Build OpenSSL for ${a.abi} (${a.opensslTarget})"

        inputs.dir(opensslSrcDir)
        outputs.dir(opensslOutDir.resolve(a.abi))

        doLast {
            val ndkDir = android.ndkDirectory
            val hostTag = ndkHostTag(ndkDir)
            val bin = ndkDir.resolve("toolchains/llvm/prebuilt").resolve(hostTag).resolve("bin")

            val api = minSdkVersionValue
            val prefix = opensslOutDir.resolve(a.abi).absolutePath
            val buildDir = layout.buildDirectory.dir("openssl-build/${a.abi}").get().asFile

            val cmd = """
        set -euo pipefail
        if [ ! -f "${opensslSrcDir.absolutePath}/Configure" ]; then
          echo "OpenSSL submodule not found or not initialized at: ${opensslSrcDir.absolutePath}"
          echo "Run: git submodule update --init --recursive"
          exit 1
        fi

        mkdir -p "$buildDir"
        cd "$buildDir"

        export ANDROID_NDK_ROOT="${ndkDir.absolutePath}"
        export ANDROID_NDK="${ndkDir.absolutePath}"


        perl "${opensslSrcDir.absolutePath}/Configure" ${a.opensslTarget} -D__ANDROID_API__=$api \
          no-shared no-tests --prefix="$prefix"

        make -j${'$'}(nproc) || make -j${'$'}(nproc)
        make install_sw
    """.trimIndent()

            exec {
                executable = "/usr/bin/bash"
                args("-c", cmd)

                val currentPath = System.getenv("PATH") ?: ""
                environment("PATH", "${bin.absolutePath}:$currentPath")
            }
        }
    }
}

tasks.register("buildOpenSslAll") {
    group = "native"
    description = "Build OpenSSL for all ABIs"
    dependsOn(opensslAbis.map { opensslTaskName(it.abi) })
}

/* -----------------------------
   Rust build (4 ABIs) - Mygod style:
   build a BIN but output/copy it as lib*.so via linker wrapper
   ----------------------------- */

data class RustTarget(val abi: String, val triple: String)
val rustTargets = listOf(
    RustTarget("armeabi-v7a", "armv7-linux-androideabi"),
    RustTarget("arm64-v8a", "aarch64-linux-android"),
    RustTarget("x86", "i686-linux-android"),
    RustTarget("x86_64", "x86_64-linux-android"),
)

val rustDir = file("src/main/rust/slipstream-rust").absolutePath

// The final filename inside APK (it is an executable, but we name it as .so)
val outSoName = "libslipstream.so"

// Must exist: a shim that calls python wrapper, similar to Mygod
val linkerShim = file("src/main/rust/linker-shim.sh").absolutePath

fun rustTaskName(abi: String) = "buildRust" + abi.replace("-", "").replace("_", "")

rustTargets.forEach { t ->
    tasks.register(rustTaskName(t.abi)) {
        group = "rust"
        description = "Build Rust for ${t.abi} (${t.triple})"

        // Build OpenSSL for this ABI first
        dependsOn(opensslTaskName(t.abi))

        doLast {
            val ndkDir = android.ndkDirectory
            val hostTag = ndkHostTag(ndkDir)
            val binDir = ndkDir.resolve("toolchains/llvm/prebuilt").resolve(hostTag).resolve("bin")

            val api = minSdkVersionValue
            val triple = t.triple
            val key = cargoEnvKeyFor(triple)

            val cc = clangFor(triple, api, binDir)
            val cxx = cc.replace("clang", "clang++")
            val ar = binDir.resolve("llvm-ar").absolutePath
            val ranlib = binDir.resolve("llvm-ranlib").absolutePath

            val opensslRoot = "$projectDir/src/main/rust/openssl-out/${t.abi}"

            // Per-ABI picoquic build directory (must be isolated to avoid arch mixing)
            val picoDir = "$rustDir/.picoquic-build/${t.abi}"
            file(picoDir).deleteRecursively()

            // Output path inside jniLibs
            val outDir = file("$projectDir/src/main/jniLibs/${t.abi}")
            outDir.mkdirs()
            val outSo = outDir.resolve(outSoName).absolutePath
            project.exec {
                workingDir = file(rustDir)
                executable = "cargo"
                args(
                    "build", "--verbose",
                    "--target=$triple",
                    *if (cargoProfile == "release") arrayOf("--release") else emptyArray(),
                    "-p", "slipstream-client",
                    "--bin", "slipstream-client",
                    "--features", "picoquic-minimal-build"
                )

                // --- Android / NDK environment ---
                environment("ANDROID_NDK_HOME", ndkDir.absolutePath)
                environment("ANDROID_ABI", t.abi)
                environment("ANDROID_PLATFORM", "android-$api")

                // --- Toolchain env for C/C++ subbuilds ---
                environment("CC", cc)
                environment("CXX", cxx)
                environment("AR", ar)
                environment("RANLIB", ranlib)

                // --- Force Cargo linker to our shim (Mygod style) ---
                environment("CARGO_TARGET_${key}_LINKER", linkerShim)

                // This is required by the python wrapper you posted
                environment("CARGO_NDK_MAJOR_VERSION", "27")
                environment("RUST_ANDROID_GRADLE_CC", cc)
                environment("RUST_ANDROID_GRADLE_CC_LINK_ARG", "-Wl,-z,max-page-size=16384,-soname,$outSoName")

                // The wrapper will copy the final -o output to this path
                environment("RUST_ANDROID_GRADLE_TARGET", outSo)

                // --- Per-ABI picoquic build directory ---
                environment("PICOQUIC_BUILD_DIR", picoDir)
                environment("PICOQUIC_AUTO_BUILD", "1")
                environment("BUILD_TYPE", if (cargoProfile == "release") "Release" else "Debug")

                // --- OpenSSL paths for openssl-sys + CMake ---
                environment("OPENSSL_ROOT_DIR", opensslRoot)
                environment("OPENSSL_DIR", opensslRoot)
                environment("OPENSSL_INCLUDE_DIR", "$opensslRoot/include")
                environment("OPENSSL_LIB_DIR", "$opensslRoot/lib")
                environment("OPENSSL_CRYPTO_LIBRARY", "$opensslRoot/lib/libcrypto.a")
                environment("OPENSSL_SSL_LIBRARY", "$opensslRoot/lib/libssl.a")
                environment("OPENSSL_USE_STATIC_LIBS", "TRUE")

                // --- Force cmake compilers to prevent host/other-ABI objects ---
                environment(
                    "CMAKE_ARGS",
                    """
                    -DCMAKE_SYSTEM_NAME=Android
                    -DCMAKE_ANDROID_NDK=${ndkDir.absolutePath}
                    -DCMAKE_ANDROID_ARCH_ABI=${t.abi}
                    -DCMAKE_ANDROID_API=$api
                    -DCMAKE_C_COMPILER=$cc
                    -DCMAKE_CXX_COMPILER=$cxx
                    -DCMAKE_AR=$ar
                    -DCMAKE_RANLIB=$ranlib
                    -DANDROID=TRUE
                    -DANDROID_PLATFORM=android-$api
                    -DANDROID_ABI=${t.abi}
                    -DOPENSSL_ROOT_DIR=$opensslRoot
                    -DOPENSSL_INCLUDE_DIR=$opensslRoot/include
                    -DOPENSSL_CRYPTO_LIBRARY=$opensslRoot/lib/libcrypto.a
                    -DOPENSSL_SSL_LIBRARY=$opensslRoot/lib/libssl.a
                    """.trimIndent()
                )

                environment("RUSTUP_NO_UPDATE_CHECK", "1")
            }

            if (!file(outSo).exists()) {
                throw GradleException("Expected output not found: $outSo (wrapper did not copy final output).")
            }
        }
    }
}
tasks.register("cargoBuild") {
    group = "rust"
    description = "Build Rust for all Android ABIs"
    dependsOn(rustTargets.map { rustTaskName(it.abi) })
}




/* -----------------------------
   Dependencies
   ----------------------------- */

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")
        force("androidx.activity:activity:1.9.3")
        force("androidx.activity:activity-ktx:1.9.3")
        force("androidx.activity:activity-compose:1.9.3")
    }
}

// ---- Hook Rust build into Android build ----
afterEvaluate {
    // When building APK/AAB, make sure Rust/OpenSSL are built first
    tasks.matching { t ->
        t.name in setOf(
            "preBuild",
            "preDebugBuild",
            "preReleaseBuild"
        )
    }.configureEach {
        dependsOn("cargoBuild")
    }
}