# Sarmaya (سرمایہ)

**Sarmaya** is a robust, lightweight, and mathematically airtight portfolio tracker for stocks and assets. It uses an event-sourced calculation engine to ensure that your holdings are always accurate, even when backdated transactions or complex splits occur.

## Key Features

- **Event-Sourced Math**: Computes holdings by replaying your transaction history, ensuring 100% data integrity.
- **Support for All Transaction Types**: Seamlessly handles `BUY`, `SELL`, `DIVIDEND`, `BONUS`, and `SPLIT` (ratio-based).
- **Adversarial Resilience**: Built with a strict **Domain Validation Layer** that blocks time-travel paradoxes (selling before buying), NaN poisoning, and invalid state mutations.
- **Offline First**: Powered by a local Room database with Coroutine-backed transactional safety.
- **Modern UI**: A clean Jetpack Compose interface with native Dark Mode support.

## Project Structure

- `app/src/main/java/com/sarmaya/app/domain`: Contains the **2026 Fix** - Centralized `TransactionDomainModel` for ironclad validation.
- `app/src/main/java/com/sarmaya/app/data`: The data layer with Room entities, DAOs, and the event-sourced `PortfolioCalculator`.
- `app/src/main/java/com/sarmaya/app/viewmodel`: Lifecycle-aware ViewModels managing state and business logic coordination.
- `app/src/test/java/com/sarmaya/app`: A comprehensive suite of **Adversarial & Unit Tests** (29+ tests) covering edge cases and "Chaos" scenarios.

## Technical Stack

- **UI**: Jetpack Compose
- **Database**: Room Persistence Library
- **Concurrency**: Kotlin Coroutines & Flow
- **Architecture**: MVVM with a dedicated Domain Layer
- **Validation**: Point-in-time chronological sequence enforcement

## Getting Started

1. Clone the repository.
2. Open in Android Studio (Koala or later).
3. Run the application on an emulator or physical device.
4. (Optional) Run `./gradlew testDebugUnitTest` to verify the adversarial security layer.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
