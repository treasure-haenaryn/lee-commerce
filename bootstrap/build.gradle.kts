dependencies {
    implementation(project(":common"))
    implementation(project(":user"))
    implementation(project(":catalog"))
    implementation(project(":inventory"))
    implementation(project(":cart"))
    implementation(project(":order"))
    implementation(project(":payment"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.archunit.junit5)
}
