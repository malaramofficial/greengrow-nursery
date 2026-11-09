# GreenGrow Nursery

Hindi-first, offline-first native Android nursery management app.

## MVP foundation
- Kotlin + Jetpack Compose + Material 3 (native Android; no WebView)
- Room database for local plant catalogue
- Dashboard, plant list and add-plant form
- INR pricing with basic input validation
- GitHub Actions debug APK build

## Build
GitHub Actions builds a debug APK on pushes and pull requests to `main`. Download `greengrow-nursery-debug-apk` from the workflow run artifacts.

## Product integrity
Plant species and counts are never inferred from photos. Cloud sync, payments, notifications, storefront, expenses, staff roles and Play Store release are not configured yet. No fabricated business records are seeded.

## Next milestones
Stock adjustment ledger UI, order/payment workflow, expense tracking, reports, multi-business tenant model, authentication, secure cloud sync, tests and release hardening.
