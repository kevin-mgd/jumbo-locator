package nl.jumbo.assignement.jumbolocator

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgisContainer(): PostgreSQLContainer<*> {
        val postgisImage = DockerImageName.parse("postgis/postgis:17-3.5")
            .asCompatibleSubstituteFor("postgres")

        return PostgreSQLContainer(postgisImage)
            .withDatabaseName("jumbolocator")
            .withUsername("test")
            .withPassword("test")
    }
}
