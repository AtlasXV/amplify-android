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
    implementation(dependency.okhttp)
    testImplementation(testDependency.junit)
    androidTestImplementation(testDependency.androidx.test.junit)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}