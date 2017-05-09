package com.github.jntakpe.fratomockpi

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class Reader(val batchProperties: BatchProperties) {

    val logger = LoggerFactory.getLogger(javaClass.simpleName)

    fun read(): Flux<FraMock> {
        logger.info("Start reading mocks from {}", batchProperties.fraUrl)
        return Flux.just(FraMock("test", HttpMethod.GET, "sdf"))
    }

}