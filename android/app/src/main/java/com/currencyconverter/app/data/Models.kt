package com.currencyconverter.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrencyInfo(
    val code: String,
    val name: String,
    val cc: String? = null,
)

@Serializable
data class CurrenciesFile(
    val popular: List<String> = emptyList(),
    val currencies: List<CurrencyInfo> = emptyList(),
)

@Serializable
data class SeedRatesFile(
    val base: String = "USD",
    @SerialName("updated_utc") val updatedUtc: String = "",
    val rates: Map<String, Double> = emptyMap(),
)

enum class RatesSource { BUNDLED, CACHE, REMOTE }

/** A snapshot of exchange rates plus where they came from and when they were current as of. */
data class RatesSnapshot(
    val base: String,
    val rates: Map<String, Double>,
    val source: RatesSource,
    /** Wall-clock millis this snapshot was fetched/cached at (used for "Saved Nh ago"). */
    val fetchedAtMillis: Long,
)

/** Persisted on-disk cache shape (filesDir/rates_cache.json). */
@Serializable
data class CachedRates(
    val base: String,
    val fetchedAtMillis: Long,
    val rates: Map<String, Double>,
)
