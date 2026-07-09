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
// The io.github.kuiralabs.contract plugin syncs the compiled Compact
// artifacts from the contract source into the app's assets before each
// build: the runtime JS as assets/runtime/vault-contract.js and each
// circuit's keys as assets/keys/<circuit>.{prover,verifier,bzkir}.
// VaultContract reads those canonical paths at runtime.
kuiraContract {
    source.set("../contract/src/managed/Vault")
    alias.set("vault")
    // Offline bundle (#256): ship the protocol wallet proving keys in the APK so a
    // fresh device proves without the runtime S3 download. ~33MB; downloaded once
    // at build time into a shared Gradle cache, then staged into assets/wallet-keys.
    bundleWalletKeys.set(true)
}

// ─── Private Vault contract assets (second contract) ───────────────
// The contract plugin syncs ONE contract (the public Vault above). The private
// sibling is synced by hand into DEDICATED asset dirs so its keys stay separate
// from the public vault's — its impure circuits are renamed (pv*) so proving
// keys never collide, and dedicated dirs keep verifier-key loading unambiguous.
// Source of truth is the committed contract/src/managed/PrivateVault; these
// asset dirs are generated (gitignored), same model as the plugin's output.
val syncPrivateVaultRuntime by tasks.registering(Copy::class) {
    from("../contract/src/managed/PrivateVault/contract/index.js")
    into(layout.projectDirectory.dir("src/main/assets/private-vault-runtime"))
    rename { "private-vault-contract.js" }
}
val syncPrivateVaultKeys by tasks.registering(Copy::class) {
    // compact compile splits a circuit's key set across two dirs: keys/ holds .prover + .verifier,
    // zkir/ holds the .bzkir circuit IR. Local proving needs ALL THREE, so pull the .bzkir too —
    // the contract plugin does this for the public vault; the manual sync must match it.
    from("../contract/src/managed/PrivateVault/keys")
    from("../contract/src/managed/PrivateVault/zkir") { include("*.bzkir") }
    into(layout.projectDirectory.dir("src/main/assets/private-vault-keys"))
}
tasks.named("preBuild") { dependsOn(syncPrivateVaultRuntime, syncPrivateVaultKeys) }

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
