import XCTest

/// Drives the real app in the simulator to prove the currency picker sheet,
/// favorites, search, and the Clear button all actually work — not just that
/// the views compile. NOTE: these tests read/write the app's real
/// UserDefaults-backed persistence (FROM/TO/favorites/theme), so the app is
/// uninstalled and freshly reinstalled after the full test run to guarantee
/// a clean default state for the proof screenshots.
final class CurrencySheetUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func test_openSheet_search_andPickCurrency_updatesFromCode() throws {
        let app = XCUIApplication()
        app.launch()

        // Default state: FROM is USD, TO is UZS (per docs/SPEC.md section 4).
        let fromCodeText = app.staticTexts["fromCodeText"]
        XCTAssertTrue(fromCodeText.waitForExistence(timeout: 5))
        XCTAssertEqual(fromCodeText.label, "USD")

        // Tap the FROM row to open the "Convert from" sheet.
        app.buttons["fromCurrencyRow"].tap()
        let sheetTitle = app.staticTexts["Convert from"]
        XCTAssertTrue(sheetTitle.waitForExistence(timeout: 3), "Sheet did not open")

        // GBP is not a default favorite and is far enough alphabetically that
        // it isn't laid out by the LazyVStack until scrolled/searched to.
        XCTAssertFalse(app.buttons["currencyRow_GBP"].exists, "GBP should not be laid out yet before searching")

        // Search filters the list down to matches.
        let searchField = app.textFields["currencySearchField"]
        XCTAssertTrue(searchField.waitForExistence(timeout: 3))
        searchField.tap()
        searchField.typeText("pound")

        let gbpRow = app.buttons["currencyRow_GBP"]
        XCTAssertTrue(gbpRow.waitForExistence(timeout: 3), "Search for 'pound' did not surface GBP")
        // A currency that clearly does not match "pound" must be filtered out.
        XCTAssertFalse(app.buttons["currencyRow_JPY"].exists, "Search did not filter out non-matching currency")

        XCTAssertTrue(app.staticTexts["GBP"].exists)

        // Picking GBP closes the sheet and sets FROM = GBP, with the rate
        // line recomputed against the new currency.
        gbpRow.tap()
        XCTAssertFalse(sheetTitle.exists, "Sheet should close after picking a currency")
        XCTAssertTrue(fromCodeText.waitForExistence(timeout: 3))
        XCTAssertEqual(fromCodeText.label, "GBP")

        let rateLine = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH '1 GBP ='")).firstMatch
        XCTAssertTrue(rateLine.waitForExistence(timeout: 3), "Rate line did not recompute for the new FROM currency")
    }

    func test_defaultFavoritesPinnedToTop_andStarTogglesWork() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["fromCodeText"].waitForExistence(timeout: 5))

        app.buttons["fromCurrencyRow"].tap()
        XCTAssertTrue(app.staticTexts["Convert from"].waitForExistence(timeout: 3))

        // Default favorites (USD, UZS, EUR) must be visible without any
        // scrolling or search — they're pinned to the top of the list.
        XCTAssertTrue(app.buttons["currencyRow_USD"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["currencyRow_UZS"].exists)
        XCTAssertTrue(app.buttons["currencyRow_EUR"].exists)
        XCTAssertEqual(app.buttons["starButton_USD"].label, "★", "Default favorite should render a filled star")

        // AED is not a default favorite but is alphabetically first, so it's
        // laid out near the top too (just after the 3 pinned favorites).
        let aedStar = app.buttons["starButton_AED"]
        XCTAssertTrue(aedStar.waitForExistence(timeout: 3))
        XCTAssertEqual(aedStar.label, "☆", "AED should not be a favorite by default")

        // Tapping the star favorites it — the glyph flips to filled.
        aedStar.tap()
        XCTAssertEqual(aedStar.label, "★", "Star did not toggle to favorited after tap")

        // Un-favorite it again to leave persisted state clean for other runs.
        aedStar.tap()
        XCTAssertEqual(aedStar.label, "☆", "Star did not toggle back to un-favorited")
    }

    func test_clearButton_resetsEntryToZero() throws {
        let app = XCUIApplication()
        app.launch()

        let fromCodeText = app.staticTexts["fromCodeText"]
        XCTAssertTrue(fromCodeText.waitForExistence(timeout: 5))

        // Default entry is "100"; type a couple more digits, then Clear.
        app.buttons["key_1"].tap()
        app.buttons["key_2"].tap()

        let clearButton = app.buttons["clearButton"]
        XCTAssertTrue(clearButton.waitForExistence(timeout: 3))
        clearButton.tap()

        // The active (FROM) side's big value should now read exactly "0".
        let fromValue = app.buttons["fromValueText"]
        XCTAssertTrue(fromValue.waitForExistence(timeout: 3))
        XCTAssertEqual(fromValue.label, "0", "Clear did not reset the entry to 0")
    }

    /// Proves item 4 (persistence) for real: change FROM + theme, kill the
    /// app process, relaunch it as a brand-new process, and confirm the
    /// change survived — not just that UserDefaults.set() was called.
    ///
    /// Named `zzx_` deliberately: `xcodebuild test` installs the app ONCE and
    /// reuses that same install (and its on-disk UserDefaults) across every
    /// test method in this run, so a test that intentionally leaves
    /// persisted state mutated (FROM=EUR, dark theme) must run after every
    /// other test that assumes the fresh-install default (USD/UZS, light).
    func test_zzx_fromCurrencyAndTheme_persistAcrossRelaunch() throws {
        let app1 = XCUIApplication()
        app1.launch()
        XCTAssertTrue(app1.staticTexts["fromCodeText"].waitForExistence(timeout: 5))
        // Doesn't assume a specific starting FROM/theme — an earlier test in
        // this same run (same install, same on-disk UserDefaults) may have
        // already changed them. EUR is just a deterministic *target* no
        // other test in this file ever picks, so switching to it is always
        // a real, observable change regardless of run order.
        let themeIconBefore = app1.buttons["themeToggleButton"].label

        // Change FROM to EUR.
        app1.buttons["fromCurrencyRow"].tap()
        XCTAssertTrue(app1.buttons["currencyRow_EUR"].waitForExistence(timeout: 3))
        app1.buttons["currencyRow_EUR"].tap()
        XCTAssertTrue(app1.staticTexts["fromCodeText"].waitForExistence(timeout: 3))
        XCTAssertEqual(app1.staticTexts["fromCodeText"].label, "EUR")

        // Toggle theme and confirm the icon actually flipped.
        app1.buttons["themeToggleButton"].tap()
        let themeIconAfter = app1.buttons["themeToggleButton"].label
        XCTAssertNotEqual(themeIconBefore, themeIconAfter, "Theme toggle had no visible effect")

        // Kill the process entirely (not just backgrounding).
        app1.terminate()
        Thread.sleep(forTimeInterval: 1)

        // Relaunch as a fresh process and confirm both changes stuck.
        let app2 = XCUIApplication()
        app2.launch()
        XCTAssertTrue(app2.staticTexts["fromCodeText"].waitForExistence(timeout: 5))
        XCTAssertEqual(app2.staticTexts["fromCodeText"].label, "EUR", "FROM currency did not persist across relaunch")
        XCTAssertEqual(app2.buttons["themeToggleButton"].label, themeIconAfter, "Theme did not persist across relaunch")

        // Deliberately does NOT restore state here — this test mutates real
        // on-disk UserDefaults, so the app is uninstalled and freshly
        // reinstalled afterward (outside XCTest) before any proof screenshot
        // is taken, which is a more reliable reset than more UI taps.
    }

    /// Not a functional assertion — opens the "Convert from" sheet and
    /// attaches a real screenshot proving the favorite stars (★ USD/UZS/EUR
    /// pinned to top, ☆ everything else) actually render, per user feedback
    /// item 3. Does not toggle anything, so it leaves persisted state as-is.
    ///
    /// Named `zzw_` (before `zzx_`) deliberately: it must run against the
    /// clean default state, not after the persistence test's dark-theme +
    /// FROM=EUR mutation — chaining straight into a sheet-open UI
    /// interaction right after that test's relaunch proved unreliable in
    /// this environment (simulator/XCUITest event-delivery, not app logic).
    func test_zzw_sheetScreenshotShowsFavoriteStars() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["fromCodeText"].waitForExistence(timeout: 5))
        Thread.sleep(forTimeInterval: 1) // let the launch animation fully settle before tapping

        // NOTE: after the preceding test relaunches into a persisted dark
        // theme, the very first tap right after launch is occasionally
        // swallowed (SwiftUI re-render/hit-test settle, not app logic) —
        // but a THIRD tap while the sheet is already open would hit the
        // scrim behind it and close it again. So: at most ONE bounded
        // retry, each time confirming state before deciding whether to tap
        // again.
        app.buttons["fromCurrencyRow"].tap()
        if !app.buttons["currencyRow_USD"].waitForExistence(timeout: 2) {
            app.buttons["fromCurrencyRow"].tap()
        }
        XCTAssertTrue(app.buttons["currencyRow_USD"].waitForExistence(timeout: 6), "Sheet with favorite rows never opened")
        Thread.sleep(forTimeInterval: 0.5)

        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "currency-sheet-favorites"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// Not a functional assertion — just drives the simulator's real Home
    /// button so we can attach a screenshot proving the AppIcon asset
    /// (shared/branding/icon-1024.png, wired via Assets.xcassets/AppIcon.appiconset)
    /// actually renders on the Home Screen, not just that it compiles into Assets.car.
    func test_zzz_appIconVisibleOnHomeScreen() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["fromCodeText"].waitForExistence(timeout: 5))

        XCUIDevice.shared.press(.home)
        Thread.sleep(forTimeInterval: 1.5)

        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let icon = springboard.icons["Currency Converter"]
        XCTAssertTrue(icon.waitForExistence(timeout: 5), "AppIcon / CFBundleDisplayName not found on Home Screen")

        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "home-screen-app-icon"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
