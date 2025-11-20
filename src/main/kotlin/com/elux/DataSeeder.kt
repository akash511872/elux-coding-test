package com.elux

import com.elux.model.Product
import com.elux.service.ProductService

/**
 * Helper object to seed initial product data
 * Call this from the main application startup if needed
 */
object DataSeeder {
    
    fun seedProducts(productService: ProductService) {
        val products = listOf(
            // Sweden products
            Product("laptop-se-1", "Premium Laptop", 15000.0, "Sweden"),
            Product("phone-se-1", "Smartphone Pro", 8000.0, "Sweden"),
            Product("tablet-se-1", "Tablet Max", 5000.0, "Sweden"),
            Product("monitor-se-1", "4K Monitor", 3000.0, "Sweden"),
            
            // Germany products
            Product("laptop-de-1", "Business Laptop", 12000.0, "Germany"),
            Product("phone-de-1", "Smartphone Basic", 6000.0, "Germany"),
            Product("tablet-de-1", "Tablet Mini", 4000.0, "Germany"),
            Product("keyboard-de-1", "Mechanical Keyboard", 1500.0, "Germany"),
            
            // France products
            Product("laptop-fr-1", "Gaming Laptop", 20000.0, "France"),
            Product("phone-fr-1", "Smartphone Ultra", 10000.0, "France"),
            Product("tablet-fr-1", "Tablet Pro", 6000.0, "France"),
            Product("mouse-fr-1", "Wireless Mouse", 800.0, "France")
        )
        
        products.forEach { product ->
            try {
                productService.createProduct(product)
                println("Created product: ${product.name} (${product.id})")
            } catch (e: Exception) {
                // Product might already exist, that's okay
                println("Product ${product.id} already exists or error: ${e.message}")
            }
        }
    }
}
