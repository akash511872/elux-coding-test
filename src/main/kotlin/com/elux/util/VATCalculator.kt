package com.elux.util

object VATCalculator {
    private val vatRates = mapOf(
        "Sweden" to 0.25,
        "Germany" to 0.19,
        "France" to 0.20
    )
    
    fun getVATRate(country: String): Double {
        return vatRates[country] ?: throw IllegalArgumentException("Unsupported country: $country")
    }
    
    fun calculateFinalPrice(basePrice: Double, totalDiscountPercent: Double, vatRate: Double): Double {
        val priceAfterDiscount = basePrice * (1 - totalDiscountPercent / 100.0)
        return priceAfterDiscount * (1 + vatRate)
    }
}
