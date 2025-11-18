package com.elux

import com.elux.database.DatabaseFactory
import com.elux.routes.productRoutes
import com.elux.service.ProductService
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val config = HoconApplicationConfig(ConfigFactory.load())
    
    // Database configuration
    val jdbcUrl = config.propertyOrNull("database.jdbcUrl")?.getString() 
        ?: System.getenv("DATABASE_URL") 
        ?: "jdbc:postgresql://localhost:5432/elux_products"
    val driver = config.propertyOrNull("database.driver")?.getString() 
        ?: "org.postgresql.Driver"
    val user = config.propertyOrNull("database.user")?.getString() 
        ?: System.getenv("DATABASE_USER") 
        ?: "elux_user"
    val password = config.propertyOrNull("database.password")?.getString() 
        ?: System.getenv("DATABASE_PASSWORD") 
        ?: "elux_pass"
    val maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toIntOrNull() 
        ?: 10
    
    // Initialize database
    DatabaseFactory.init(jdbcUrl, driver, user, password, maxPoolSize)
    
    val productService = ProductService()
    
    // Seed initial data (only creates if doesn't exist)
    try {
        DataSeeder.seedProducts(productService)
        log.info("Sample products initialized")
    } catch (e: Exception) {
        log.warn("Could not seed products: ${e.message}")
    }
    
    // Configure JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Configure status pages for error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Unknown error")))
        }
    }
    
    routing {
        get("/") {
            call.respondText("Elux Product API - Use /products?country={country} to get products", ContentType.Text.Plain)
        }
        
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        
        productRoutes(productService)
    }
}
