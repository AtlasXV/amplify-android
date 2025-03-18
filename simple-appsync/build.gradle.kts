plugins {
    id("com.android.library")
    id("kotlin-android")
}
apply(from = rootProject.file("configuration/publishing.gradle"))

dependencies {
    api(project(":aws-datastore"))
    api(project(":aws-api-appsync"))
    api(project(":core"))
    implementation(libs.okhttp)
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.coroutines)
}
android {
    namespace = "com.atlasv.android.amplify.simpleappsync"
    buildFeatures {
        buildConfig = true
    }
}

android.kotlinOptions {
    jvmTarget = "17"
}