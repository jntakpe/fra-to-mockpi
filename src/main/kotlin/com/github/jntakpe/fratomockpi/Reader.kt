package com.github.jntakpe.fratomockpi

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Component
class Reader(val batchProperties: BatchProperties) {

    val logger = LoggerFactory.getLogger(javaClass.simpleName)

    fun read(): Flux<List<FraMock>> {
        logger.info("Start reading mocks from {}", batchProperties.fraUrl)
        return retrieveAll()
                .buffer(batchProperties.chunkSize)
                .doOnNext { logger.info("{} mocks read", batchProperties.chunkSize) }
    }

    private fun retrieveAll(): Flux<FraMock> {
        return WebClient.create(batchProperties.fraUrl).get()
                .uri("/endpoints")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .flatMapMany { it.bodyToFlux(FraMock::class.java) }
    }

}