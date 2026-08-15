package com.currencyconverter.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.currencyconverter.app.data.CurrencyInfo
import com.currencyconverter.app.data.PersistedSettings
import com.currencyconverter.app.data.PreferencesRepository
import com.currencyconverter.app.data.RatesRepository
import com.currencyconverter.app.data.RatesSnapshot
import com.currencyconverter.app.data.RatesSource
import com.currencyconverter.app.engine.ConverterEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EntrySide { FROM, TO }
enum class SheetTarget { FROM, TO }

private data class RawState(
    val entry: String = "100",
    val side: EntrySide = EntrySide.FROM,
    val fromCode: String = "USD",
    val toCode: String = "UZS",
    val darkTheme: Boolean = false,
    val offlineMode: Boolean = true,
    val sheetFor: SheetTarget? = null,
    val query: String = "",
    val isRefreshing: Boolean = false,
    /** Ordered by when they were starred; favourites sort first in the picker. */
    val favorites: List<String> = listOf("USD", "UZS", "EUR"),
    val settingsLoaded: Boolean = false,
)

data class SheetRow(
    val code: String,
    val name: String,
    val flagAsset: String?,
    val selected: Boolean,
    val rateText: String,
    val isFavorite: Boolean,
)

data class ConverterDisplay(
    val fromCode: String = "USD",
    val fromName: String = "US Dollar",
    val fromFlagAsset: String? = "flags/us.png",
    val fromValue: String = "100",
    val activeFrom: Boolean = true,
    val toCode: String = "UZS",
    val toName: String = "Uzbekistani soʻm",
    val toFlagAsset: String? = "flags/uz.png",
    val toValue: String = "0",
    val activeTo: Boolean = false,
    val rateLine: String = "",
    val updatedLine: String = "Offline",
    val modeLabel: String = "Offline",
    val offlineMode: Boolean = true,
    val darkTheme: Boolean = false,
    val sheetOpen: Boolean = false,
    val sheetTitle: String = "Convert from",
    val query: String = "",
    val sheetList: List<SheetRow> = emptyList(),
    val ratesReady: Boolean = false,
)

class ConverterViewModel(
    private val repository: RatesRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow(RawState())
    private val ratesState = MutableStateFlow<RatesSnapshot?>(null)

    private val currencyByCode: Map<String, CurrencyInfo> by lazy {
        repository.currenciesData.currencies.associateBy { it.code }
    }

    val uiState: StateFlow<ConverterDisplay> =
        combine(rawState, ratesState) { raw, rates -> buildDisplay(raw, rates) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ConverterDisplay())

    init {
        viewModelScope.launch {
            val settings = preferences.load()
            rawState.value = rawState.value.copy(
                fromCode = settings.fromCode,
                toCode = settings.toCode,
                darkTheme = settings.darkTheme,
                favorites = settings.favorites,
                settingsLoaded = true,
            )
        }
        viewModelScope.launch {
            val initial = repository.loadInitialRates()
            ratesState.value = initial
        }
    }

    private fun rateOf(rates: Map<String, Double>, code: String): Double = rates[code] ?: 1.0

    private fun buildDisplay(raw: RawState, snapshot: RatesSnapshot?): ConverterDisplay {
        val rates = snapshot?.rates ?: emptyMap()
        val ratesReady = snapshot != null
        val f = currencyByCode[raw.fromCode]
        val t = currencyByCode[raw.toCode]
        val fRate = rateOf(rates, raw.fromCode)
        val tRate = rateOf(rates, raw.toCode)

        val amount = raw.entry.toDoubleOrNull() ?: 0.0
        val other = if (raw.side == EntrySide.FROM) {
            ConverterEngine.convert(amount, fRate, tRate)
        } else {
            ConverterEngine.convert(amount, tRate, fRate)
        }
        val fromStr = if (raw.side == EntrySide.FROM) ConverterEngine.fmtEntry(raw.entry) else ConverterEngine.fmt(other)
        val toStr = if (raw.side == EntrySide.TO) ConverterEngine.fmtEntry(raw.entry) else ConverterEngine.fmt(other)

        val query = raw.query.trim().lowercase()
        val sheetTarget = raw.sheetFor
        val selectedCode = if (sheetTarget == SheetTarget.FROM) raw.fromCode else raw.toCode
        val baseRate = if (sheetTarget == SheetTarget.FROM) tRate else fRate
        val baseCode = if (sheetTarget == SheetTarget.FROM) raw.toCode else raw.fromCode

        val sheetList = if (sheetTarget != null) {
            val filtered = repository.currenciesData.currencies.filter { c ->
                query.isEmpty() || c.code.lowercase().contains(query) || c.name.lowercase().contains(query)
            }
            // Favourites first (in starred order), then the rest in source order — mirrors the design spec.
            val favIndex = raw.favorites.withIndex().associate { (i, code) -> code to i }
            val (favs, rest) = filtered.partition { favIndex.containsKey(it.code) }
            val orderedFavs = favs.sortedBy { favIndex.getValue(it.code) }
            (orderedFavs + rest).map { c ->
                val rate = rateOf(rates, c.code)
                SheetRow(
                    code = c.code,
                    name = c.name,
                    flagAsset = repository.flagAssetPath(c.cc),
                    selected = c.code == selectedCode,
                    rateText = "${ConverterEngine.fmt(if (rate == 0.0) 0.0 else baseRate / rate)} $baseCode",
                    isFavorite = favIndex.containsKey(c.code),
                )
            }
        } else emptyList()

        return ConverterDisplay(
            fromCode = raw.fromCode,
            fromName = f?.name.orEmpty(),
            fromFlagAsset = repository.flagAssetPath(f?.cc),
            fromValue = fromStr,
            activeFrom = raw.side == EntrySide.FROM,
            toCode = raw.toCode,
            toName = t?.name.orEmpty(),
            toFlagAsset = repository.flagAssetPath(t?.cc),
            toValue = toStr,
            activeTo = raw.side == EntrySide.TO,
            rateLine = ConverterEngine.rateLine(raw.fromCode, raw.toCode, fRate, tRate),
            updatedLine = updatedLine(raw.offlineMode, snapshot),
            modeLabel = if (raw.offlineMode) "Offline" else "Live",
            offlineMode = raw.offlineMode,
            darkTheme = raw.darkTheme,
            sheetOpen = sheetTarget != null,
            sheetTitle = if (sheetTarget == SheetTarget.FROM) "Convert from" else "Convert to",
            query = raw.query,
            sheetList = sheetList,
            ratesReady = ratesReady,
        )
    }

    private fun updatedLine(offlineIntent: Boolean, snapshot: RatesSnapshot?): String {
        if (snapshot == null) return "Offline"
        if (!offlineIntent && snapshot.source == RatesSource.REMOTE) return "Just now"
        if (snapshot.fetchedAtMillis <= 0L) return "Offline"
        val hours = ((System.currentTimeMillis() - snapshot.fetchedAtMillis) / 3_600_000L).toInt()
        return if (hours < 1) "Saved <1h ago" else "Saved ${hours}h ago"
    }

    private fun persist(raw: RawState) {
        if (!raw.settingsLoaded) return // don't clobber storage with defaults before the first load completes
        viewModelScope.launch {
            preferences.save(
                PersistedSettings(
                    fromCode = raw.fromCode,
                    toCode = raw.toCode,
                    darkTheme = raw.darkTheme,
                    favorites = raw.favorites,
                ),
            )
        }
    }

    fun press(key: String) {
        rawState.value = rawState.value.copy(entry = ConverterEngine.press(rawState.value.entry, key))
    }

    /** Clears the focused entry back to "0" without touching side/currencies (product addition beyond the design spec). */
    fun clearEntry() {
        rawState.value = rawState.value.copy(entry = "0")
    }

    fun swap() {
        val next = rawState.value.let { it.copy(fromCode = it.toCode, toCode = it.fromCode) }
        rawState.value = next
        persist(next)
    }

    fun toggleTheme() {
        val next = rawState.value.copy(darkTheme = !rawState.value.darkTheme)
        rawState.value = next
        persist(next)
    }

    fun toggleOfflineMode() {
        val next = !rawState.value.offlineMode
        rawState.value = rawState.value.copy(offlineMode = next)
        if (!next) refreshFromRemote() // switching to Live -> attempt a fetch
    }

    private fun refreshFromRemote() {
        if (rawState.value.isRefreshing) return
        rawState.value = rawState.value.copy(isRefreshing = true)
        viewModelScope.launch {
            repository.refreshFromRemote()
                .onSuccess { ratesState.value = it }
            rawState.value = rawState.value.copy(isRefreshing = false)
        }
    }

    fun openSheet(target: SheetTarget) {
        rawState.value = rawState.value.copy(sheetFor = target, query = "")
    }

    fun closeSheet() {
        rawState.value = rawState.value.copy(sheetFor = null, query = "")
    }

    fun search(query: String) {
        rawState.value = rawState.value.copy(query = query)
    }

    fun pickCurrency(code: String) {
        val s = rawState.value
        val target = s.sheetFor ?: return
        val other = if (target == SheetTarget.FROM) s.toCode else s.fromCode
        val clash = code == other
        val next = if (target == SheetTarget.FROM) {
            s.copy(fromCode = code, toCode = if (clash) s.fromCode else s.toCode, sheetFor = null, query = "")
        } else {
            s.copy(toCode = code, fromCode = if (clash) s.toCode else s.fromCode, sheetFor = null, query = "")
        }
        rawState.value = next
        persist(next)
    }

    fun toggleFavorite(code: String) {
        val s = rawState.value
        val next = s.copy(
            favorites = if (s.favorites.contains(code)) s.favorites - code else s.favorites + code,
        )
        rawState.value = next
        persist(next)
    }

    fun focusSide(target: EntrySide) {
        val s = rawState.value
        if (s.side == target) return
        val current = uiState.value
        val currentDisplayed = if (target == EntrySide.FROM) current.fromValue else current.toValue
        rawState.value = s.copy(side = target, entry = ConverterEngine.stripFmt(currentDisplayed))
    }

    class Factory(
        private val repository: RatesRepository,
        private val preferences: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConverterViewModel(repository, preferences) as T
        }
    }
}
