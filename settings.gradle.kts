rootProject.name = "trading-engine"
include("trading-engine-server")
include("trading-engine-deploy")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("spring.boot.starter.web", "org.springframework.boot:spring-boot-starter-web:3.2.0")
            library("spring.boot.starter.parent", "org.springframework.boot:spring-boot-starter-parent:3.2.0")
            library("spring.kafka", "org.springframework.kafka:spring-kafka:3.2.0")
            library("common.api", "com.herron.exchange:common-api:1.0.0-SNAPSHOT")
            library("common", "com.herron.exchange:common:1.0.0-SNAPSHOT")
        }

        create("testlibs") {
            library("spring.boot.starter.test", "org.springframework.boot:spring-boot-starter-test:3.2.0")
            library("junit.jupiter.api", "org.junit.jupiter:junit-jupiter-api:5.8.1")
            library("junit.jupiter.engine", "org.junit.jupiter:junit-jupiter-engine:5.8.1")
            library("spring.kafka.test", "org.springframework.kafka:spring-kafka:3.2.0")
        }
    }
}
