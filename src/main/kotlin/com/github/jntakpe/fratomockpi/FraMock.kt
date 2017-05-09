package com.github.jntakpe.fratomockpi

import org.springframework.http.HttpMethod

data class FraMock(val uri: String, val method: HttpMethod, val content: String, val delay: Int = 0, val params: List<Param> = emptyList())

data class Param(val name: String, val value: String)