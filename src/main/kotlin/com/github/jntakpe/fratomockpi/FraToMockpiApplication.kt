package com.github.jntakpe.fratomockpi

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
class FraToMockpiApplication {

    @Value("\${fraUrl}") lateinit var fraUrl: String
    @Value("\${fraPrefix}") lateinit var fraPrefix: String
    @Value("\${mockpiPrefix}") lateinit var mockpiPrefix: String

    @Bean
    fun job() = CommandLineRunner {
        read()
                .scan(Pair<Int, FraMock?>(0, null), { (first), t -> Pair(first + 1, t) })
                .skip(1)
                .map { it.second?.toMockPI(it.first) }
                .blockLast()
    }

    private fun read() = WebClient.create(fraUrl).get()
            .uri("/endpoints")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .exchange()
            .flatMapMany { it.bodyToFlux(FraMock::class.java) }

    private fun FraMock.toMockPI(idx: Int) = MockPI("import_$idx", mapRequest(this), mapResponse(this), "import", this.delay, "desc_$idx")

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

data class MockPI(val name: String, val request: Req, val response: Res, val collection: String, val delay: Int, val description: String)

data class Req(val path: String, val method: HttpMethod, val params: Map<String, String>, val headers: Map<String, String>)

data class Res(val body: String, val status: Int, val contentType: String)