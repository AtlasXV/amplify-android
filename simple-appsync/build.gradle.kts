plugins {
    id("com.android.library")
    id("kotlin-android")
}
apply(from = rootProject.file("configuration/publishing.gradle"))

dependencies {
    api(project(":core"))
    api(project(":core-kotlin"))
    api(project(":aws-api"))
    api(project(":aws-auth-cognito"))
    api(project(":aws-api-appsync"))
    api(project(":aws-datastore"))
    api(libs.aws.signing)
    api(libs.aws.credentials)
    implementation(libs.okhttp)
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.coroutines)
}

android.kotlinOptions {
    jvmTarget = "11"
}