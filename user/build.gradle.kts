dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
