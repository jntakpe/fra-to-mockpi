package com.github.jntakpe.fratomockpi

import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.connection.ClusterSettings
import com.mongodb.connection.SslSettings
import com.mongodb.connection.netty.NettyStreamFactoryFactory
import com.mongodb.reactivestreams.client.MongoClients
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
@EnableReactiveMongoRepositories
@EnableConfigurationProperties(MongoProperties::class)
@EnableAutoConfiguration(exclude = arrayOf(MongoAutoConfiguration::class, MongoDataAutoConfiguration::class))
class FraToMockpiApplication {

    val logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Value("\${fraUrl}") lateinit var fraUrl: String
    @Value("\${fraPrefix}") lateinit var fraPrefix: String
    @Value("\${mockpiPrefix}") lateinit var mockpiPrefix: String

    @Bean
    fun job(mockPIRepo: MockPIRepo) = CommandLineRunner {
        logger.info("Starting job")
        read()
                .scan(Pair<Int, FraMock?>(0, null), { (first), t -> Pair(first + 1, t) })
                .filter { it.second != null }
                .map { Pair(it.first, it.second!!) }
                .doOnNext { logger.info("Reading item n° ${it.first} with path ${it.second.uri}") }
                .map { it.second.toMockPI(it.first) }
                .doOnNext { logger.info("Saving item ${it.name}") }
                .flatMap { mockPIRepo.save(it) }
                .doOnNext { logger.info("Item ${it.name} saved") }
                .blockLast()

    }

    private fun read() = WebClient.create(fraUrl).get()
            .uri("/endpoints")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .exchange()
            .flatMapMany { it.bodyToFlux(FraMock::class.java) }

    private fun FraMock.toMockPI(idx: Int) = Mock("import_$idx", mapRequest(this), mapResponse(this), "import", this.delay, "desc_$idx", null)

    private fun mapRequest(input: FraMock) = Req(prefixUri(input.uri), input.method, mapParams(input.params), emptyMap())

    private fun prefixUri(uri: String) = uri.replaceFirst(fraPrefix, mockpiPrefix)

    private fun mapParams(input: List<Param>) = input.associate { Pair(it.name, it.value) }

    private fun mapResponse(input: FraMock) = Res(input.content, HttpStatus.OK.value(), MediaType.APPLICATION_JSON_UTF8_VALUE)
}


fun main(args: Array<String>) {
    val applicationContext = SpringApplication.run(FraToMockpiApplication::class.java, *args)
    applicationContext.close()
}

data class FraMock(val uri: String, val method: HttpMethod, val content: String, val delay: Int = 0, val params: List<Param> = emptyList())

data class Param(val name: String, val value: String)

data class Req(val path: String, val method: HttpMethod, val params: Map<String, String>, val headers: Map<String, String>)

data class Res(val body: String, val status: Int, val contentType: String)

@Document
@TypeAlias("com.github.jntakpe.mockpi.domain.Mock")
data class Mock(val name: String, val request: Req, val response: Res, val collection: String, val delay: Int, val description: String,
                var id: ObjectId?)

interface MockPIRepo : ReactiveMongoRepository<Mock, ObjectId>

@Configuration
class MongoDBConfig(val mongoProperties: MongoProperties) : AbstractReactiveMongoConfiguration() {

    override fun mongoClient() = MongoClients.create(settings())

    override fun getDatabaseName() = mongoProperties.database

    private fun settings() = MongoClientSettings.builder()
            .sslSettings(SslSettings.builder()
                    .enabled(true)
                    .build())
            .clusterSettings(clusterSettings())
            .credentialList(listOf(mongoCredentials()))
            .streamFactoryFactory(NettyStreamFactoryFactory.builder().build())
            .build()

    private fun clusterSettings() = ClusterSettings.builder()
            .hosts(listOf(ServerAddress(mongoProperties.host, mongoProperties.port)))
            .build()

    private fun mongoCredentials() = MongoCredential.createMongoCRCredential(
            mongoProperties.username,
            mongoProperties.database,
            mongoProperties.password
    )

}