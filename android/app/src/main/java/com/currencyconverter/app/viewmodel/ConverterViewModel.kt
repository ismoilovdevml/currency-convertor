package com.currencyconverter.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.currencyconverter.app.R
import com.currencyconverter.app.data.ConnectivityObserver
import com.currencyconverter.app.data.CurrencyInfo
import com.currencyconverter.app.data.PersistedSettings
import com.currencyconverter.app.data.PreferencesRepository
import com.currencyconverter.app.data.RatesRepository
import com.currencyconverter.app.data.RatesSnapshot
import com.currencyconverter.app.engine.ConverterEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale

enum class EntrySide { FROM, TO }
enum class SheetTarget { FROM, TO }

private data class RawState(
    val entry: String = "100",
    val side: EntrySide = EntrySide.FROM,
    val fromCode: String = "USD",
    val toCode: String = "UZS",
    val darkTheme: Boolean = false,
    val sheetFor: SheetTarget? = null,
    val query: String = "",
    val isRefreshing: Boolean = false,
    /** Ordered by when they were starred; favourites sort first in the picker. Empty by default. */
    val favorites: List<String> = emptyList(),
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
    val updatedLine: String = "",
    val modeLabel: String = "Offline",
    /** Real network state — the app follows this automatically; there is no manual toggle. */
    val online: Boolean = false,
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
    private val connectivity: ConnectivityObserver,
    private val appContext: Context,
) : ViewModel() {

    private val rawState = MutableStateFlow(RawState())
    private val ratesState = MutableStateFlow<RatesSnapshot?>(null)
    private val onlineState = MutableStateFlow(connectivity.isOnlineNow())

    private val currencyByCode: Map<String, CurrencyInfo> by lazy {
        repository.currenciesData.currencies.associateBy { it.code }
    }

    /**
     * Currency display name in the device's language, via the OS CLDR data
     * (java.util.Currency). Falls back to the bundled English name from
     * currencies.json for codes the JDK's ISO-4217 table doesn't recognise
     * (e.g. XCG, XCD, XDR, XAF, XOF, XPF, ANG, and other regional/legacy codes).
     */
    private fun localizedCurrencyName(code: String, fallbackName: String): String {
        return try {
            Currency.getInstance(code).getDisplayName(Locale.getDefault())
        } catch (e: Exception) {
            fallbackName
        }
    }

    val uiState: StateFlow<ConverterDisplay> =
        combine(rawState, ratesState, onlineState) { raw, rates, online ->
            buildDisplay(raw, rates, online)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ConverterDisplay())

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
            // Offline-first: seed/cache immediately, never touches the network.
            ratesState.value = repository.loadInitialRates()
        }
        viewModelScope.launch {
            // Follow real connectivity; refresh rates automatically whenever we become online.
            connectivity.isOnline.collect { online ->
                onlineState.value = online
                if (online) refreshFromRemote()
            }
        }
    }

    private fun rateOf(rates: Map<String, Double>, code: String): Double = rates[code] ?: 1.0

    private fun buildDisplay(raw: RawState, snapshot: RatesSnapshot?, online: Boolean): ConverterDisplay {
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
            // Match on code, the OS-localized name, and the bundled English name (currencies.json)
            // so e.g. a Russian-locale user can still find "Dollar" in Latin script.
            val named = repository.currenciesData.currencies.map { c ->
                c to localizedCurrencyName(c.code, c.name)
            }
            val filtered = named.filter { (c, localizedName) ->
                query.isEmpty() ||
                    c.code.lowercase().contains(query) ||
                    localizedName.lowercase().contains(query) ||
                    c.name.lowercase().contains(query)
            }
            val favIndex = raw.favorites.withIndex().associate { (i, code) -> code to i }
            val (favs, rest) = filtered.partition { (c, _) -> favIndex.containsKey(c.code) }
            val orderedFavs = favs.sortedBy { (c, _) -> favIndex.getValue(c.code) }
            (orderedFavs + rest).map { (c, localizedName) ->
                val rate = rateOf(rates, c.code)
                SheetRow(
                    code = c.code,
                    name = localizedName,
                    flagAsset = repository.flagAssetPath(c.cc),
                    selected = c.code == selectedCode,
                    rateText = "${ConverterEngine.fmt(if (rate == 0.0) 0.0 else baseRate / rate)} $baseCode",
                    isFavorite = favIndex.containsKey(c.code),
                )
            }
        } else emptyList()

        return ConverterDisplay(
            fromCode = raw.fromCode,
            fromName = f?.let { localizedCurrencyName(it.code, it.name) }.orEmpty(),
            fromFlagAsset = repository.flagAssetPath(f?.cc),
            fromValue = fromStr,
            activeFrom = raw.side == EntrySide.FROM,
            toCode = raw.toCode,
            toName = t?.let { localizedCurrencyName(it.code, it.name) }.orEmpty(),
            toFlagAsset = repository.flagAssetPath(t?.cc),
            toValue = toStr,
            activeTo = raw.side == EntrySide.TO,
            rateLine = ConverterEngine.rateLine(raw.fromCode, raw.toCode, fRate, tRate),
            updatedLine = if (raw.isRefreshing) appContext.getString(R.string.updating) else updatedLine(snapshot),
            modeLabel = if (online) appContext.getString(R.string.status_online) else appContext.getString(R.string.status_offline),
            online = online,
            darkTheme = raw.darkTheme,
            sheetOpen = sheetTarget != null,
            sheetTitle = if (sheetTarget == SheetTarget.FROM) {
                appContext.getString(R.string.convert_from)
            } else {
                appContext.getString(R.string.convert_to)
            },
            query = raw.query,
            sheetList = sheetList,
            ratesReady = ratesReady,
        )
    }

    /** Human "Updated Xm/Xh/Xd ago" from the snapshot's real fetch time, localized. */
    private fun updatedLine(snapshot: RatesSnapshot?): String {
        if (snapshot == null) return ""
        if (snapshot.fetchedAtMillis <= 0L) return appContext.getString(R.string.not_updated)
        val diff = System.currentTimeMillis() - snapshot.fetchedAtMillis
        val sec = diff / 1000
        return when {
            sec < 60 -> appContext.getString(R.string.updated_just_now)
            sec < 3600 -> appContext.getString(R.string.updated_minutes, (sec / 60).toInt())
            sec < 86_400 -> appContext.getString(R.string.updated_hours, (sec / 3600).toInt())
            else -> appContext.getString(R.string.updated_days, (sec / 86_400).toInt())
        }
    }

    private fun persist(raw: RawState) {
        if (!raw.settingsLoaded) return
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

    private fun refreshFromRemote() {
        if (rawState.value.isRefreshing) return
        rawState.value = rawState.value.copy(isRefreshing = true)
        viewModelScope.launch {
            repository.refreshFromRemote().onSuccess { ratesState.value = it }
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
        private val connectivity: ConnectivityObserver,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConverterViewModel(repository, preferences, connectivity, appContext) as T
        }
    }
}
