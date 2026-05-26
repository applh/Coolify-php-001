# Android Blackjack (Professional-Grade Card Game) Applet Plan

**Objective**: Extend the Android Multi-App Hub by introducing the **Blackjack** applet (`BlackjackScreen.kt`, `BlackjackViewModel.kt`, and SQLite-based game telemetry engines). This tool implements a highly polished, offline-first Blackjack simulation utilizing standard casino-grade Vegas rules, supporting advanced play metrics (Double Down, dynamic pairs splitting into independent hands), resilient wallet balance persistence, and an interactive offline Basic Strategy Helper overlay to train professional-style play.

---

## 1. Architectural Highlights & Tech Stack

To ensure pristine responsiveness, high-fidelity frame-based card animations, and absolute integrity of statistics, the applet utilizes standard Jetpack Compose MVVM architecture bound to Kotlin Coroutines and a localized SQLite transactional persistence layer.

```
                    ┌────────────────────────┐
                    │   BlackjackViewModel   │
                    └───────────┬────────────┘
                                │ State Flow (Hands, Wallet, Bet, Turn)
                                ▼
                    ┌────────────────────────┐
                    │   BlackjackScreen UI   │
                    └───────────┬────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│  State Machine │      │  Blackjack   │        │ SoundPool    │
│  & Game Rule │        │  Database    │        │ Audio Engine │
│  (Hit/Stand/ │        │  Helper      │        │ (Deals, Bets,│
│ Split/Double)│        │ (Stats/CRUD) │        │ Wins, Shoves)│
└──────────────┘        └──────────────┘        └──────────────┘
```

- **State Engine**: A structured state transition hierarchy implemented in Kotlin (`GameStatus` including `BETTING`, `PLAYER_TURN`, `DEALER_TURN`, `PAYS_OUT`).
- **Database Engine**: A dedicated local SQL database mapping `blackjack_wallet` and `blackjack_rounds` to record financial telemetry, session states, and lifetime hit/stand/win/loss ratios.
- **Audio Synthesis Layer**: Integration of Android's system `SoundPool` for instantly-responsive low-latency card dealing, chip clinking, shuffling, and outcome notifications.
- **Theme and Presentation**: Responsive Jetpack Compose drawing off-white and charcoal card faces with custom rounded vector suit symbols, resting on high-contrast felt-green CSS-inspired Material canvas.

---

## 2. Core Features (The Requested Scope)

### A. Professional Casino Rule Engine
- **Standard Vegas Rules**: Game incorporates 8 standard 52-card decks shuffled together into a shoe. The virtual dealer strikes a classic hit-on-soft-17 policy (holds on hard 17, hits if Aces allow soft 17).
- **Core Action Suite**:
  - **Hit**: Deal a card to the current active hand. Aces evaluate dynamically as 1 or 11 to maximize hand value without busting.
  - **Stand**: Conclude the action for the current hand and pass control to the next hand or the dealer.
  - **Double Down**: Double the primary bet, obtain exactly one additional card, and stand immediately. Supported only on the initial two cards of a hand.
  - **Split Pair Configuration**: If the initial two cards of a hand bear matching values (e.g., 8-8 or A-A), allow splitting them into two independent hands. Each hand receives its own secondary bet matching the primary stake, and is played out sequentially.
  - **Insurance Offering**: When the dealer's face-up card is an Ace, the player can optionally buy insurance (amounting to 1/2 of their primary bet) against a dealer Blackjack, paying 2:1.

### B. Betting & Interactive Bankroll System
- **Chip Betting Rail**: Visually intuitive betting rail containing clickable values: $5, $10, $25, $100, and $500 chips. Players can customize their primary bet, clear the table, or issue a "Rebet" matching their previous rounds.
- **Wallet Persistence**: Player balances start at an initial $1,000 allowance, stored in Scoped SQLite tables. In the event of a total bust, players can tap a "Reload Wallet" utility card to grant a $500 loan.
- **Secure Transactional Rollback**: Bets are decremented on Deal, and updated dynamically upon hand outcomes to prevent physical state discrepancies or data losses if the application is killed during play.

### C. Visual Polishing & Fluid UX
- **Card-Dealt Transitions**: Cards slide dynamically into view with a subtle scale-up and fade-in entry using Compose's dynamic `AnimatedVisibility` and offset tracking.
- **Active Hand Isolation**: In split states, the screen dims non-active hands, drawing a sharp, borders-pulsing selection ring around the currently controlled split hand for unmatched clarity.
- **Outcome Toast Overlays**: High-contrast, custom notification overlays displaying "WIN (+1.5x)", "BLACKJACK (+2.5x)", "PUSH (Return)", or "BUST (Dealer Wins)" with vibrant success colors.

### D. Offline Strategy & Counting Assistance (Professional Coaching)
- **Active Smart Strategy Advisor**: An optional HUD panel analyzing the current player hand against the dealer's visible card. It calculates the mathematically perfect mathematical outcome (e.g., "Basic Strategy recommends DOUBLE here!") based on standard card matrix math.
- **Card Counting HUD**: An optional assistant measuring the active running count (using standard Hi-Lo value: +1 for 2-6, 0 for 7-9, -1 for 10-A) and true count based on approximate remaining decks. Trains cards enthusiasts to count dynamically during standard play.

---

## 3. Recommended Professional Features

### 1. Lifetime Analytics & Practice Console
- **Rich Dashboard**: Track lifetime metrics including hands played, absolute Win/Loss/Push ratios, personal Blackjack frequency, maximum payout won, and maximum continuous winning streak.
- **Streak Tracker**: Graph historical wins/losses streaks through elegant local telemetry lists mapping historic transactions.

### 2. Table Theme Customizer
- **Visual felt configurations**: Allow users to switch between three premium casino atmospheres on the fly:
  - **Classic Emerald Felt**: Traditional deep forest green.
  - **High-Roller Burgundy**: Rich, deep purple-red suited for maximum contrast.
  - **Royal Indigo Lounge**: Deep slate blue providing an eye-strain friendly palette.

### 3. Progressive Card-Shoe Stats Helper
- **Shoe Penetration Tracker**: High-contrast meter showing the remaining cards fraction in the 8-deck shoe (e.g., "Cards Remaining: 312 / 416"). Warns when the shoe is 75% depleted and details the automated reshuffling transition.

---

## 4. UI / UX Layout & Composition

The layout utilizes a responsive portrait frame featuring rich contrast variations suitable for mobile.

```
┌────────────────────────────────────────────────────────┐
│  ♣ BLACKJACK PRO      [ Advisor: ON ]     [ Felt: Green]│
├────────────────────────────────────────────────────────┤
│  Dealer Hand: (Total: 14)                             │
│  ┌───┐  ┌───┐                                          │
│  │ 8 │  │ ? │                                          │
│  │ ♠ │  │░░░│                      ◄-- Dealer Cards    │
│  └───┘  └───┘                                          │
├────────────────────────────────────────────────────────┤
│                  ┌───────────────────────┐            │
│                  │ Advisor: Split Aces!  │  ◄-- HUD   │
│                  └───────────────────────┘            │
├────────────────────────────────────────────────────────┤
│  Player Hand 1: [Active] (Total: 12)                  │
│  ┌───┐  ┌───┐                                          │
│  │ A │  │ A │                                          │
│  │ ♥ │  │ ♦ │                      ◄-- Player Cards    │
│  └───┘  └───┘                                          │
├────────────────────────────────────────────────────────┤
│  [ Wallet: $1,250 ]                 [ Primary Bet: $50]│
├────────────────────────────────────────────────────────┤
│  Chips:   ( $5 )   ( $10 )   ( $25 )   ( $100 ) ( $500)│
├────────────────────────────────────────────────────────┤
│  Actions:  [HIT]   [STAND]   [DOUBLE]   [SPLIT]   [DEAL]│
└────────────────────────────────────────────────────────┘
```

---

## 5. Technical Implementation Blueprint

### A. Card Model and Shoe Generation
`CardsModel.kt` defines the structural elements of suits, ranks, physical cards, and card shoes:

```kotlin
package com.example.cameraxapp.blackjack

import java.util.Collections

enum class Suit(val symbol: String, val isRed: Boolean) {
    SPADES("♠", false),
    HEARTS("♥", true),
    DIAMONDS("♦", true),
    CLUBS("♣", false)
}

enum class Rank(val value: Int, val representation: String) {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"),
    SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"), TEN(10, "10"),
    JACK(10, "J"), QUEEN(10, "Q"), KING(10, "K"), ACE(11, "A")
}

data class Card(val rank: Rank, val suit: Suit, var isFaceUp: Boolean = true)

class CardShoe(private val deckCount: Int = 8) {
    private val cards = mutableListOf<Card>()

    init {
        resetAndShuffle()
    }

    fun resetAndShuffle() {
        cards.clear()
        for (deck in 0 until deckCount) {
            for (suit in Suit.values()) {
                for (rank in Rank.values()) {
                    cards.add(Card(rank, suit))
                }
            }
        }
        Collections.shuffle(cards)
    }

    fun draw(): Card {
        if (cards.isEmpty()) {
            resetAndShuffle()
        }
        return cards.removeAt(0)
    }

    fun remainingCount(): Int = cards.size
    fun totalCount(): Int = deckCount * 52
}
```

### B. Core Game State and Hand Rules
`BlackjackGameEngine.kt` encapsulates the mathematical processing models:

```kotlin
package com.example.cameraxapp.blackjack

data class BlackjackHand(
    val cards: MutableList<Card> = mutableListOf(),
    var bet: Int = 0,
    var isDone: Boolean = false,
    var isBust: Boolean = false
) {
    fun calculateScore(): Int {
        var total = cards.sumOf { it.rank.value }
        var acesCount = cards.count { it.rank == Rank.ACE }
        
        // Handle Aces adjusting dynamically from 11 down to 1 if score busts
        while (total > 21 && acesCount > 0) {
            total -= 10
            acesCount--
        }
        return total
    }

    fun isBlackjack(): Boolean = cards.size == 2 && calculateScore() == 21
}
```

### C. SQLite Telemetry Storage Update API
`BlackjackDatabaseHelper.kt` manages robust user wallet values and metrics tracking:

```kotlin
package com.example.cameraxapp.blackjack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BlackjackDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Persistent Player Wallet Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS blackjack_wallet (
                id INTEGER PRIMARY KEY DEFAULT 1,
                balance INTEGER NOT NULL DEFAULT 1000
            )
        """)
        // Cumulative Metrics Tracking
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS blackjack_stats (
                id INTEGER PRIMARY KEY DEFAULT 1,
                hands_played INTEGER NOT NULL DEFAULT 0,
                wins INTEGER NOT NULL DEFAULT 0,
                losses INTEGER NOT NULL DEFAULT 0,
                pushes INTEGER NOT NULL DEFAULT 0,
                blackjacks INTEGER NOT NULL DEFAULT 0,
                max_win INTEGER NOT NULL DEFAULT 0,
                max_streak INTEGER NOT NULL DEFAULT 0
            )
        """)
        // Bootstrap initial records
        db.execSQL("INSERT OR IGNORE INTO blackjack_wallet (id, balance) VALUES (1, 1000)")
        db.execSQL("INSERT OR IGNORE INTO blackjack_stats (id, hands_played) VALUES (1, 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun getBalance(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT balance FROM blackjack_wallet WHERE id = 1", null)
        var balance = 1000
        if (cursor.moveToFirst()) {
            balance = cursor.getInt(0)
        }
        cursor.close()
        return balance
    }

    fun updateBalance(newBalance: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("balance", newBalance)
        }
        db.update("blackjack_wallet", values, "id = 1", null)
    }

    fun getStats(): PlayerStats {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM blackjack_stats WHERE id = 1", null)
        var stats = PlayerStats()
        if (cursor.moveToFirst()) {
            stats = PlayerStats(
                handsPlayed = cursor.getInt(1),
                wins = cursor.getInt(2),
                losses = cursor.getInt(3),
                pushes = cursor.getInt(4),
                blackjacks = cursor.getInt(5),
                maxWin = cursor.getInt(6),
                maxStreak = cursor.getInt(7)
            )
        }
        cursor.close()
        return stats
    }

    fun recordGameRound(outcome: String, winAmount: Int) {
        val db = writableDatabase
        val stats = getStats()
        val isWin = outcome == "WIN" || outcome == "BLACKJACK"
        val isLoss = outcome == "LOSS" || outcome == "BUST"
        val isPush = outcome == "PUSH"
        val isBJ = outcome == "BLACKJACK"

        val values = ContentValues().apply {
            put("hands_played", stats.handsPlayed + 1)
            put("wins", stats.wins + if (isWin) 1 else 0)
            put("losses", stats.losses + if (isLoss) 1 else 0)
            put("pushes", stats.pushes + if (isPush) 1 else 0)
            put("blackjacks", stats.blackjacks + if (isBJ) 1 else 0)
            if (winAmount > stats.maxWin) {
                put("max_win", winAmount)
            }
        }
        db.update("blackjack_stats", values, "id = 1", null)
    }

    companion object {
        private const val DATABASE_NAME = "blackjack_game.db"
        private const val DATABASE_VERSION = 1
    }
}

data class PlayerStats(
    val handsPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val pushes: Int = 0,
    val blackjacks: Int = 0,
    val maxWin: Int = 0,
    val maxStreak: Int = 0
)
```

---

## 6. Full CRUD Mappings and State Transitions

To ensure rigorous state safety, game actions trigger sequential CRUD updates in SQLite and state changes in the ViewModel:

### A. The Action Workflow (The Core Loop)

```
[ Betting State ] --(Place Bet & Deal Button)--> [ Deal Sequence (Deduct Bet in SQLite) ]
                                                            │
                                                            ▼
                                              [ Check Natural Blackjacks ]
                                                            │
    ┌───────────────────────────────────────────────────────┴───────────────────────────────────────────────────────┐
    ▼                                                                                                               ▼
[ No Blackjack ]                                                                                             [ Blackjack Occurs ]
    │                                                                                                               │
    ├───────────────┬───────────────────┬───────────────┐                                                           ▼
    │ (Hit Action)  │ (Double Action)   │ (Split Action)│ (Stand Action)                                     [ Settle & Record Stats ]
    ▼               ▼                   ▼               ▼                                                           │
[ Deal Card ]   [ Add Bet ]         [ Split Hands ]  [ Finish Hand ]                                                ▼
  Check Bust      Draw Card           Clone Bets       Lock State                                            [ Wallet Update SQLite ]
  Lock Hand       Lock Hand           Play Hand 1 & 2       │
    │               │                                       │
    └───────────────┴───────────────────┬───────────────────┘
                                        ▼
                                 [ Dealer's Turn ] --(Hit to 17)--> [ Resolve Outstandings ]
                                                                             │
                                                                             ▼
                                                                  [ Update Database Stats ]
```

1.  **Deducting the Bet**: Clicking "Deal" pulls the current active bet value from the UI layout. The ViewModel calls `updateBalance(currentBalance - activeBet)` to record the decrement live in `blackjack_wallet` table.
2.  **Evaluating Hand Splits**: When Split is selected, the player hand splits into `Hand[0]` and `Hand[1]`.
    - Double down and Split validation checks: The database helper's balance must exceed the split bet value before splitting is permitted.
    - If validated, the secondary bet is deducted from the wallet record in SQLite immediately.
3.  **End-of-Round Payback Resolution**:
    - If Player wins, wallet increments: `updateBalance(currentBalance + winAmount)`.
    - `recordGameRound(...)` updates columns live in the lifetime metrics tracker, calculating streaks and historical record maximum pools.

### B. Dynamic Basic Strategy Engine (Coaching API)

Inside `BlackjackViewModel.kt`, an automated lookup rule scans player values to prompt precise strategic suggestions:

```kotlin
fun getBasicStrategyAdvice(playerHandScore: Int, dealerUpCardValue: Int, isPair: Boolean): String {
    if (isPair) {
        return when (playerHandScore) {
            22 -> "Always SPLIT Aces (11 + 11)."
            16 -> "Always SPLIT 8s. Prevents bad scores."
            20 -> "Always STAND on 10s. Never split a 20."
            18 -> if (dealerUpCardValue in 2..9 && dealerUpCardValue != 7) "SPLIT 9s against dealer $dealerUpCardValue." else "STAND on 18."
            14 -> if (dealerUpCardValue in 2..7) "SPLIT 7s against dealer $dealerUpCardValue." else "HIT 14."
            12 -> if (dealerUpCardValue in 2..6) "SPLIT 6s against dealer $dealerUpCardValue." else "HIT 12."
            10 -> if (dealerUpCardValue in 2..9) "DOUBLE bet on 5s, otherwise HIT." else "HIT 10."
            else -> "HIT to build score."
        }
    }

    if (playerHandScore >= 17) return "STAND immediately on high score."
    if (playerHandScore <= 8) return "HIT on low scope values."

    return when (playerHandScore) {
        11 -> "DOUBLE DOWN. High chance of a 10-value."
        10 -> if (dealerUpCardValue in 2..9) "DOUBLE DOWN against dealer $dealerUpCardValue." else "HIT 10."
        9 -> if (dealerUpCardValue in 3..6) "DOUBLE DOWN against dealer $dealerUpCardValue." else "HIT 9."
        12 -> if (dealerUpCardValue in 4..6) "STAND. Let dealer take bust risks." else "HIT 12."
        in 13..16 -> if (dealerUpCardValue in 2..6) "STAND. High dealer bust risk." else "HIT scope."
        else -> "HIT to construct score."
    }
}
```

---

## 7. Rollout Phases

1. **Phase 1: Domain Cards Engine & Scoring Models**
   - Create core `Suit`, `Rank`, `Card`, `BlackjackHand`, and `CardShoe` classes.
   - Validate ACE-rollover score transformations (11/1 scoring checks) with targeted local unit tests.

2. **Phase 2: SQLite Schema Setup & Storage Drivers**
   - Construct `BlackjackDatabaseHelper.kt` to define game wallets and cumulative telemetry indices.
   - Securely script the CRUD state synchronizers keeping the wallet state updated under heavy operations.

3. **Phase 3: Core Game Screen & MVVM State flows**
   - Build out the custom felt board UI in modern Jetpack Compose.
   - Create the dealing visible card layout sheets and interactive animations.
   - Add classic Chip Betting Rail elements containing betting options.

4. **Phase 4: Advanced Strategy Advisor, Multi-Hand Split, Sound and Coaching Metrics**
   - Implement the interactive Advisor Overlay mapping real-time advices.
   - Code out the Split-Pair control engine dimming and tracking active hands dynamically.
   - Bind low-latency system sound outcomes using robust Android `SoundPool` classes.
