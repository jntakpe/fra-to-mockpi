package com.github.jntakpe.fratomockpi

import org.jetbrains.annotations.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("batch")
class BatchProperties {

    @NotNull lateinit var fraUrl: String
    var chunkSize: Int = 10

}