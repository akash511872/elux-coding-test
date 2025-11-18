package com.elux.database

import org.jetbrains.exposed.sql.Table

object Products : Table("products") {
    val id = varchar("id", 100)
    val name = varchar("name", 255)
    val basePrice = double("base_price")
    val country = varchar("country", 50)
    
    override val primaryKey = PrimaryKey(id)
}

object Discounts : Table("discounts") {
    val id = integer("id").autoIncrement()
    val productId = varchar("product_id", 100).references(Products.id)
    val discountId = varchar("discount_id", 100)
    val percent = double("percent")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        // Unique constraint to prevent duplicate discounts on the same product
        uniqueIndex("unique_product_discount", productId, discountId)
    }
}
