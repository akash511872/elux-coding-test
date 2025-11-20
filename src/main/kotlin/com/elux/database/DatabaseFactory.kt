package com.elux.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    
    fun init(jdbcUrl: String, driver: String, user: String, password: String, maxPoolSize: Int = 10) {
        val database = Database.connect(createHikariDataSource(jdbcUrl, driver, user, password, maxPoolSize))
        
        transaction(database) {
            SchemaUtils.create(Products, Discounts)
        }
    }
    
    private fun createHikariDataSource(
        jdbcUrl: String,
        driver: String,
        user: String,
        password: String,
        maxPoolSize: Int
    ): HikariDataSource {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = driver
            this.username = user
            this.password = password
            this.maximumPoolSize = maxPoolSize
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
}
