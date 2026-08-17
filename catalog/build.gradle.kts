dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
