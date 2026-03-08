# Sarmaya (سرمایہ)

**Sarmaya** is a modern, offline-first portfolio tracker for the Pakistan Stock Exchange (PSX). It uses an event-sourced calculation engine to ensure your holdings, realized profits, and dividends are always accurate — even with backdated edits or complex corporate actions.

## ✨ Features

- **Event-Sourced Portfolio Engine** — Computes holdings by replaying your full transaction history, guaranteeing mathematical accuracy across BUY, SELL, DIVIDEND, BONUS, and SPLIT transactions.
- **Realized P/L Tracking** — Accurately tracks profit/loss on every sell, separate from unrealized gains. Total return = unrealized + realized + dividends.
- **Dashboard at a Glance** — Gradient portfolio card, quick stats (invested, realized P/L, dividends), top movers, sector allocation, and recent activity.
- **Transaction Management** — Add, edit, and delete transactions with full validation. Filter by type (BUY/SELL/DIVIDEND/BONUS/SPLIT).
- **Price Updates** — Manually update current stock prices to see live P/L across your portfolio.
- **Adversarial Resilience** — Domain validation layer blocks time-travel paradoxes (selling before buying), NaN poisoning, and invalid state mutations.
- **Offline First** — Powered by a local Room database with no internet dependency.
- **Dark Mode** — Full dark/light theme support with a curated emerald color palette.

## 📸 Screenshots

*Coming soon*

## 🏗 Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design System | Material 3 |
| Database | Room (SQLite) |
| Architecture | MVVM + Event Sourcing |
| Async | Kotlin Coroutines & Flow |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 14 (API 34) |

## 📂 Project Structure

```
app/src/main/java/com/sarmaya/app/
├── data/            # Room entities, DAOs, PortfolioCalculator
├── domain/          # TransactionDomainModel (validation layer)
├── viewmodel/       # MVVM ViewModels
├── ui/
│   ├── screens/     # Dashboard, Holdings, Transactions, Settings
│   ├── components/  # Bottom sheets, pickers
│   ├── navigation/  # Tab navigation with Crossfade
│   └── theme/       # Colors, typography, SarmayaFinanceColors
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Koala (2024.1) or later
- JDK 17+

### Build & Run
```bash
# Clone
git clone https://github.com/dev-Aatif/sarmaya.git
cd sarmaya

# Run tests (48 tests including adversarial scenarios)
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk` (or `release/`).

## 🧪 Testing

The project includes **48+ unit tests** covering:
- Core `PortfolioCalculator` logic (BUY, SELL, DIVIDEND, BONUS, SPLIT)
- Realized P/L calculations across multiple sell scenarios
- `totalReturn` combining unrealized + realized + dividends
- Adversarial scenarios (chronological paradoxes, ghost shares, NaN injection)
- ViewModel integration tests

```bash
./gradlew test
```

## 📝 Changelog

### v1.0.0 (2026-03-08)
- 🎉 Initial release
- Event-sourced portfolio calculator with realized P/L tracking
- Dashboard with portfolio summary, top movers, sector allocation
- Holdings screen with unrealized/realized P/L and dividend tracking
- Transaction management with type filtering
- Manual price updates
- Dark/light theme with emerald color palette
- About & credits page
- 48+ unit and adversarial tests

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Aatif** — [GitHub](https://github.com/dev-Aatif)
