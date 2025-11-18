package com.elux.service

import com.elux.database.Discounts
import com.elux.database.Products
import com.elux.model.Discount
import com.elux.model.Product
import com.elux.model.ProductResponse
import com.elux.util.VATCalculator
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ProductService {
    
    fun getProductsByCountry(country: String): List<ProductResponse> = transaction {
        val products = Products.select { Products.country eq country }.map { row ->
            val productId = row[Products.id]
            val discounts = getDiscountsForProduct(productId)
            val totalDiscountPercent = discounts.sumOf { it.percent }
            val vatRate = VATCalculator.getVATRate(country)
            val finalPrice = VATCalculator.calculateFinalPrice(
                row[Products.basePrice],
                totalDiscountPercent,
                vatRate
            )
            
            ProductResponse(
                id = productId,
                name = row[Products.name],
                basePrice = row[Products.basePrice],
                country = row[Products.country],
                discounts = discounts,
                finalPrice = finalPrice
            )
        }
        products
    }
    
    fun applyDiscount(productId: String, discountId: String, percent: Double): Result<ProductResponse> = transaction {
        try {
            // Check if product exists
            val productRow = Products.select { Products.id eq productId }.singleOrNull()
                ?: return@transaction Result.failure(Exception("Product not found"))
            
            // Try to insert the discount - will fail if already exists due to unique constraint
            Discounts.insert {
                it[Discounts.productId] = productId
                it[Discounts.discountId] = discountId
                it[Discounts.percent] = percent
            }
            
            // Fetch updated product with all discounts
            val discounts = getDiscountsForProduct(productId)
            val totalDiscountPercent = discounts.sumOf { it.percent }
            val country = productRow[Products.country]
            val vatRate = VATCalculator.getVATRate(country)
            val finalPrice = VATCalculator.calculateFinalPrice(
                productRow[Products.basePrice],
                totalDiscountPercent,
                vatRate
            )
            
            Result.success(ProductResponse(
                id = productId,
                name = productRow[Products.name],
                basePrice = productRow[Products.basePrice],
                country = country,
                discounts = discounts,
                finalPrice = finalPrice
            ))
        } catch (e: ExposedSQLException) {
            // Check if it's a unique constraint violation (discount already applied)
            // For PostgreSQL: message contains "unique_product_discount"
            // For H2: message contains "Unique index or primary key violation"
            if (e.message?.contains("unique_product_discount") == true || 
                e.message?.contains("Unique index") == true ||
                e.message?.contains("UNIQUE") == true) {
                // Return current state - idempotent behavior
                val productRow = Products.select { Products.id eq productId }.single()
                val discounts = getDiscountsForProduct(productId)
                val totalDiscountPercent = discounts.sumOf { it.percent }
                val country = productRow[Products.country]
                val vatRate = VATCalculator.getVATRate(country)
                val finalPrice = VATCalculator.calculateFinalPrice(
                    productRow[Products.basePrice],
                    totalDiscountPercent,
                    vatRate
                )
                
                Result.success(ProductResponse(
                    id = productId,
                    name = productRow[Products.name],
                    basePrice = productRow[Products.basePrice],
                    country = country,
                    discounts = discounts,
                    finalPrice = finalPrice
                ))
            } else {
                Result.failure(e)
            }
        }
    }
    
    fun createProduct(product: Product): Product = transaction {
        Products.insert {
            it[id] = product.id
            it[name] = product.name
            it[basePrice] = product.basePrice
            it[country] = product.country
        }
        product
    }
    
    private fun getDiscountsForProduct(productId: String): List<Discount> {
        return Discounts.select { Discounts.productId eq productId }.map { row ->
            Discount(
                discountId = row[Discounts.discountId],
                percent = row[Discounts.percent]
            )
        }
    }
}
