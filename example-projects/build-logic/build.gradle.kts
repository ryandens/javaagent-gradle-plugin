plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.shadow.plugin)
    implementation(libs.muzzle.generation.plugin)
    implementation(libs.muzzle.check.plugin)
}
