package com.currencyconverter.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "RatesRepository"
private const val CACHE_FILE_NAME = "rates_cache.json"
private const val PRIMARY_URL = "https://open.er-api.com/v6/latest/USD"
private const val FALLBACK_URL =
    "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json"
private const val CONNECT_TIMEOUT_MS = 6000
private const val READ_TIMEOUT_MS = 6000

/**
 * Offline-first rates source: BundledSeed (assets) -> LocalCache (filesDir) -> RemoteApi.
 * Currency metadata (names/flags) always comes from the bundled `currencies.json` asset.
 */
class RatesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile: File get() = File(context.filesDir, CACHE_FILE_NAME)

    /** Static currency metadata (166 currencies). Small bundled asset, safe to parse eagerly. */
    val currenciesData: CurrenciesFile by lazy { loadCurrenciesFromAssets() }

    private fun loadCurrenciesFromAssets(): CurrenciesFile {
        val text = context.assets.open("currencies.json").bufferedReader().use { it.readText() }
        return json.decodeFromString(CurrenciesFile.serializer(), text)
    }

    private fun loadSeedFromAssets(): SeedRatesFile {
        val text = context.assets.open("seed-rates.json").bufferedReader().use { it.readText() }
        return json.decodeFromString(SeedRatesFile.serializer(), text)
    }

    /** Flag PNG bytes for a country code, or null if absent (caller renders a neutral circle). */
    fun flagAssetPath(cc: String?): String? {
        if (cc.isNullOrBlank()) return null
        return "flags/$cc.png"
    }

    /** On-launch load: LocalCache if present & valid, else BundledSeed. Never touches network. */
    suspend fun loadInitialRates(): RatesSnapshot = withContext(Dispatchers.IO) {
        readCache()?.let { cached ->
            return@withContext RatesSnapshot(
                base = cached.base,
                rates = cached.rates,
                source = RatesSource.CACHE,
                fetchedAtMillis = cached.fetchedAtMillis,
            )
        }
        val seed = loadSeedFromAssets()
        RatesSnapshot(
            base = seed.base,
            rates = seed.rates,
            source = RatesSource.BUNDLED,
            fetchedAtMillis = 0L, // unknown real-world age -> treated as "Offline", not "Saved Nh ago"
        )
    }

    private fun readCache(): CachedRates? {
        return try {
            if (!cacheFile.exists()) return null
            val text = cacheFile.readText()
            json.decodeFromString(CachedRates.serializer(), text)
        } catch (e: Exception) {
            Log.w(TAG, "cache read failed: ${e.message}")
            null
        }
    }

    private fun writeCache(snapshot: RatesSnapshot) {
        try {
            val cached = CachedRates(snapshot.base, snapshot.fetchedAtMillis, snapshot.rates)
            cacheFile.writeText(json.encodeToString(CachedRates.serializer(), cached))
        } catch (e: Exception) {
            Log.w(TAG, "cache write failed: ${e.message}")
        }
    }

    /** Attempts a live fetch (primary, then fallback). On success, persists to LocalCache. */
    suspend fun refreshFromRemote(): Result<RatesSnapshot> = withContext(Dispatchers.IO) {
        fetchPrimary().recoverCatching { fetchFallback().getOrThrow() }
            .onSuccess { writeCache(it) }
    }

    private fun fetchPrimary(): Result<RatesSnapshot> = runCatching {
        val body = httpGet(PRIMARY_URL)
        val obj = json.parseToJsonElement(body).jsonObject
        val result = (obj["result"] as? JsonPrimitive)?.content
        require(result == "success") { "open.er-api.com result != success" }
        val ratesObj = obj["rates"]?.jsonObject ?: error("missing rates")
        val rates = ratesObj.mapValues { (_, v) -> (v as JsonPrimitive).double }
        require(rates.containsKey("USD")) { "missing USD in primary rates" }
        RatesSnapshot(
            base = "USD",
            rates = rates,
            source = RatesSource.REMOTE,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun fetchFallback(): Result<RatesSnapshot> = runCatching {
        val body = httpGet(FALLBACK_URL)
        val obj = json.parseToJsonElement(body).jsonObject
        val usdObj: JsonObject = obj["usd"]?.jsonObject ?: error("missing usd map")
        val rates = mutableMapOf<String, Double>()
        rates["USD"] = 1.0
        for ((code, value) in usdObj) {
            val d = (value as? JsonPrimitive)?.double ?: continue
            rates[code.uppercase()] = d
        }
        RatesSnapshot(
            base = "USD",
            rates = rates,
            source = RatesSource.REMOTE,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun httpGet(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.requestMethod = "GET"
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code for $urlString")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
