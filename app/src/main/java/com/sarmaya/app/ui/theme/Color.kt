package com.sarmaya.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary Teal Palette (Vibrant & Fintech-inspired) ───
val TealPrimary = Color(0xFF0D9488) // Strategic brand color (accessible contrast)
val TealSecondary = Color(0xFF14B8A6) // Lighter, for active UI elements
val TealTertiary = Color(0xFF0F766E) // Deeper, for pressed states/headers

// ─── Market Colors ───
val GainGreen = Color(0xFF10B981)
val GainGreenLight = Color(0xFFD1FAE5)
val GainGreenDark = Color(0xFF065F46)

// ─── Gradient Colors (Dynamic App Feel) ───
val GradientTealStart = Color(0xFF14B8A6)
val GradientTealEnd = Color(0xFF0F766E)
val GradientDarkStart = Color(0xFF1E293B)
val GradientDarkEnd = Color(0xFF0F172A)

// Aliases to avoid breaking old v1 screens before their rewrite in Phase 8
val ProfitGreen = GainGreen
val ProfitGreenLight = GainGreenLight
val ProfitGreenDark = GainGreenDark
val EmeraldPrimary = TealPrimary
val EmeraldSecondary = TealSecondary
val EmeraldTertiary = TealTertiary
val GradientEmeraldStart = GradientTealStart
val GradientEmeraldEnd = GradientTealEnd

// ─── Other Semantic Colors ───
val LossRed = Color(0xFFEF4444) // Crisp red for negative
val LossRedLight = Color(0xFFFEE2E2)
val LossRedDark = Color(0xFF991B1B)

val WarningAmber = Color(0xFFF59E0B)
val WarningAmberLight = Color(0xFFFEF3C7)

val DividendBlue = Color(0xFF3B82F6)
val DividendBlueLight = Color(0xFFDBEAFE)

val NeutralGray = Color(0xFF9CA3AF)

// ─── Dark Theme (Deep Navy/Black - Modern Fintech) ───
val DarkBackground = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF151A23)
val DarkSurfaceVariant = Color(0xFF1F2937)
val DarkCardSurface = Color(0xFF19202E)
val DarkOnBackground = Color(0xFFF9FAFB)
val DarkOnSurface = Color(0xFFE5E7EB)
val DarkOnSurfaceVariant = Color(0xFF9CA3AF)

// ─── Light Theme (Crisp, High Contrast) ───
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightCardSurface = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF334155)
val LightOnSurfaceVariant = Color(0xFF64748B)

// ─── Market State Indicators ───
val MarketPreOpen = Color(0xFF8B5CF6) // Purple
val MarketOpen = GainGreen
val MarketSuspended = WarningAmber
val MarketClosed = NeutralGray
val MarketOffline = Color(0xFFEF4444)
