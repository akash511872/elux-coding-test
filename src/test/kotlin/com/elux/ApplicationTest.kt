package com.elux

import com.elux.database.DatabaseFactory
import com.elux.model.ProductResponse
import com.elux.service.ProductService
import com.elux.model.Product
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import kotlin.test.*

class ApplicationTest {
    
    companion object {
        private var dbInitialized = false
        
        fun ensureDbInitialized() {
            if (!dbInitialized) {
                // Initialize in-memory H2 database for testing once
                DatabaseFactory.init(
                    jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                    driver = "org.h2.Driver",
                    user = "sa",
                    password = ""
                )
                
                // Seed test data
                val productService = ProductService()
                productService.createProduct(Product("p1", "Laptop", 1000.0, "Sweden"))
                productService.createProduct(Product("p2", "Mouse", 50.0, "Germany"))
                productService.createProduct(Product("p3", "Keyboard", 75.0, "France"))
                productService.createProduct(Product("p4", "Monitor", 500.0, "Sweden"))
                
                dbInitialized = true
            }
        }
    }
    
    @Test
    fun testGetProductsByCountry() {
        ensureDbInitialized()
        val productService = ProductService()
        
        val products = productService.getProductsByCountry("Sweden")
        
        assertTrue(products.isNotEmpty())
        assertTrue(products.all { it.country == "Sweden" })
    }
    
    @Test
    fun testApplyDiscount() {
        ensureDbInitialized()
        val productService = ProductService()
        
        val result = productService.applyDiscount("p1", "discount1", 10.0)
        
        assertTrue(result.isSuccess)
        val product = result.getOrNull()
        assertNotNull(product)
        assertTrue(product.discounts.any { it.discountId == "discount1" && it.percent == 10.0 })
    }
    
    @Test
    fun testApplyDiscountIdempotency() {
        ensureDbInitialized()
        val productService = ProductService()
        
        val uniqueDiscountId = "discount2-${System.currentTimeMillis()}"
        
        // Apply discount first time
        val result1 = productService.applyDiscount("p2", uniqueDiscountId, 15.0)
        assertTrue(result1.isSuccess)
        
        // Apply same discount second time - should be idempotent
        val result2 = productService.applyDiscount("p2", uniqueDiscountId, 15.0)
        assertTrue(result2.isSuccess)
        
        // Verify product has the discount
        val products = productService.getProductsByCountry("Germany")
        val product = products.find { it.id == "p2" }
        assertNotNull(product)
        assertTrue(product.discounts.any { it.discountId == uniqueDiscountId })
    }
    
    @Test
    fun testConcurrentDiscountApplication() {
        ensureDbInitialized()
        val productService = ProductService()
        val numConcurrentRequests = 10
        val uniqueDiscountId = "discount3-${System.currentTimeMillis()}"
        
        runBlocking {
            val jobs = (1..numConcurrentRequests).map {
                async(Dispatchers.IO) {
                    productService.applyDiscount("p3", uniqueDiscountId, 20.0)
                }
            }
            
            val results = jobs.awaitAll()
            
            // All requests should succeed (idempotent)
            results.forEach { result ->
                assertTrue(result.isSuccess, "Expected all concurrent discount applications to succeed")
            }
            
            // Verify that discount was applied
            val products = productService.getProductsByCountry("France")
            val product = products.find { it.id == "p3" }
            assertNotNull(product)
            
            // Should have the discount applied
            assertTrue(product.discounts.any { it.discountId == uniqueDiscountId })
        }
    }
    
    @Test
    fun testFinalPriceCalculation() {
        ensureDbInitialized()
        val productService = ProductService()
        
        // Get initial product
        val products1 = productService.getProductsByCountry("Sweden")
        val product1 = products1.find { it.id == "p4" }
        assertNotNull(product1)
        
        // basePrice = 500, VAT = 25%
        // Verify final price calculation
        assertTrue(product1.finalPrice > 0)
        
        val uniqueDiscountId = "d1-${System.currentTimeMillis()}"
        
        // Apply 10% discount
        productService.applyDiscount("p4", uniqueDiscountId, 10.0)
        
        // Get product with discount
        val products2 = productService.getProductsByCountry("Sweden")
        val product2 = products2.find { it.id == "p4" }
        assertNotNull(product2)
        
        // Verify final price is less than without discount
        assertTrue(product2.finalPrice < product1.finalPrice)
        assertTrue(product2.discounts.any { it.discountId == uniqueDiscountId })
    }
}
