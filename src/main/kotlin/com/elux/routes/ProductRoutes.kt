package com.elux.routes

import com.elux.model.ApplyDiscountRequest
import com.elux.model.ErrorResponse
import com.elux.service.ProductService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes(productService: ProductService) {
    
    route("/products") {
        
        get {
            val country = call.request.queryParameters["country"]
            
            if (country.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("bad_request", "Country parameter is required")
                )
                return@get
            }
            
            try {
                val products = productService.getProductsByCountry(country)
                call.respond(HttpStatusCode.OK, products)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("invalid_country", e.message ?: "Invalid country")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("server_error", e.message ?: "Internal server error")
                )
            }
        }
        
        put("/{id}/discount") {
            val productId = call.parameters["id"]
            
            if (productId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("bad_request", "Product ID is required")
                )
                return@put
            }
            
            try {
                val request = call.receive<ApplyDiscountRequest>()
                
                if (request.percent < 0 || request.percent > 100) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("invalid_discount", "Discount percent must be between 0 and 100")
                    )
                    return@put
                }
                
                val result = productService.applyDiscount(productId, request.discountId, request.percent)
                
                result.onSuccess { product ->
                    call.respond(HttpStatusCode.OK, product)
                }.onFailure { error ->
                    if (error.message?.contains("not found") == true) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse("not_found", error.message ?: "Product not found")
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("server_error", error.message ?: "Internal server error")
                        )
                    }
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("bad_request", e.message ?: "Invalid request body")
                )
            }
        }
    }
}
