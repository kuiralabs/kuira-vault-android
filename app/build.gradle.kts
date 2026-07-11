import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("io.github.kuiralabs.contract") version "0.1.0-alpha05"
    // Auto `adb reverse` of the localnet ports on installDebug to a physical
    // device — no manual step. No-op on emulators (they use 10.0.2.2).
    id("io.github.kuiralabs.localnet") version "0.1.0-alpha05"
}

android {
    namespace = "com.kuiralabs.vault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kuiralabs.vault"
        // Kuira SDK requires minSdk 30 (Block Store API, passkey
        // CredentialManager, Android 11+ scoped storage assumptions).
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // TODO before shipping: turn on R8 + ProGuard. This app
            // ships minify-off so newcomers see exactly the byte code
            // their source compiled to. A production app should set
            // isMinifyEnabled = true with a `proguardFiles(...)` line
            // listing the SDK's consumer rules and your own keep rules.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// ─── Contract assets ───────────────────────────────────────────────
// The io.github.kuiralabs.contract plugin syncs BOTH contracts' compiled Compact artifacts into
// the app's assets before each build, via the contracts{} container. Each contract's circuit keys
// go to a per-alias dir (<alias>-keys) so the public Vault and the PrivateVault — which share the
// circuit names getProposalStatus / getApprovalCount / getUnshieldedBalance — don't overwrite each
// other's keys. The runtime JS is alias-named in assets/runtime/. VaultContract / PrivateVaultContract
// read these paths via the generated facades' RUNTIME_ASSET / KEYS_ASSET_DIR constants (no magic
// strings). The private sibling keeps alias "private-vault" so its asset names are unchanged.
kuiraContract {
    contracts {
        register("vault") { source.set("../contract/src/managed/Vault") }
        register("privateVault") {
            source.set("../contract/src/managed/PrivateVault")
            alias.set("private-vault")
        }
    }
    // Offline bundle (#256): ship the protocol wallet proving keys in the APK so a
    // fresh device proves without the runtime S3 download. ~33MB; downloaded once
    // at build time into a shared Gradle cache, then staged into assets/wallet-keys.
    bundleWalletKeys.set(true)
}

dependencies {
    // Kuira SDK — one dep, full graph (Sigil identity, embedded wallet,
    // contract runtime, indexer, design system). See README "Pinned
    // versions" for the upgrade story.
    implementation(libs.kuira.dapp.ui)

    // Compose stack.
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Activity + Hilt + lifecycle plumbing.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Instrumented on-chain e2e (VaultDeployE2ETest): deploy the Vault multisig via
    // the SDK on localnet, then deposit.
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
