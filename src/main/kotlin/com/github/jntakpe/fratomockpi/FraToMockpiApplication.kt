package com.github.jntakpe.fratomockpi

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class FraToMockpiApplication {

    @Bean
    fun job(reader: Reader): CommandLineRunner {
        return CommandLineRunner {
            reader.read().subscribe()
        }
    }

}

fun main(args: Array<String>) {
    val applicationContext = SpringApplication.run(FraToMockpiApplication::class.java, *args)
    applicationContext.close()
}