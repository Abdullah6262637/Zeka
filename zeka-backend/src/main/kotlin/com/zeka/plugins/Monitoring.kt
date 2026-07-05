package com.zeka.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val traceId = call.request.headers["X-Trace-Id"] ?: "no-trace-id"
            "Trace ID: $traceId - HTTP Method: $httpMethod - Status: $status - UA: $userAgent"
        }
    }
}
