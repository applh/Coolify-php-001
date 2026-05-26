package com.example.cameraxapp.blackjack

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class GameState {
    BETTING,
    PLAYER_TURN,
    DEALER_TURN,
    PAYS_OUT
}

class BlackjackViewModel(context: Context) : ViewModel() {

    private val dbHelper = BlackjackDatabaseHelper(context.applicationContext)
    private val cardShoe = CardShoe(8)

    // State Variables
    private val _gameState = mutableStateOf(GameState.BETTING)
    val gameState: State<GameState> = _gameState

    private val _walletBalance = mutableStateOf(1000)
    val walletBalance: State<Int> = _walletBalance

    private val _activeBet = mutableStateOf(0)
    val activeBet: State<Int> = _activeBet

    private val _previousBet = mutableStateOf(0)
    val previousBet: State<Int> = _previousBet

    // Dealer Hand
    val dealerCards = mutableStateListOf<Card>()
    
    // Player Hands (to support splits, up to 4 hands)
    val playerHands = mutableStateListOf<BlackjackHand>()
    
    private val _currentHandIndex = mutableStateOf(0)
    val currentHandIndex: State<Int> = _currentHandIndex

    // Analytics and Settings state
    private val _stats = mutableStateOf(PlayerStats())
    val stats: State<PlayerStats> = _stats

    private val _settings = mutableStateOf(BlackjackSettings())
    val settings: State<BlackjackSettings> = _settings

    // HUD coaching and counting streams
    private val _adviceMessage = mutableStateOf("Place a bet to deal a hand.")
    val adviceMessage: State<String> = _adviceMessage

    private val _runningCount = mutableStateOf(0)
    val runningCount: State<Int> = _runningCount

    private val _trueCount = mutableStateOf(0.0)
    val trueCount: State<Double> = _trueCount

    var isDealerSecondCardHidden = mutableStateOf(true)
    var roundResultText = mutableStateOf("")

    private val _moneyAnimation = mutableStateOf<Pair<String, Boolean>?>(null)
    val moneyAnimation: State<Pair<String, Boolean>?> = _moneyAnimation

    private fun triggerMoneyAnimation(text: String, isPositive: Boolean) {
        _moneyAnimation.value = Pair(text, isPositive)
        viewModelScope.launch {
            delay(1800)
            if (_moneyAnimation.value?.first == text) {
                _moneyAnimation.value = null
            }
        }
    }

    // List of cards played in this shoe to track Hi-Lo counting
    private var cardsSeenInShoe = 0

    init {
        loadDataFromDatabase()
        resetCounts()
    }

    private fun loadDataFromDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val balance = dbHelper.getBalance()
            val gameStats = dbHelper.getStats()
            val activeSettings = dbHelper.getSettings()

            viewModelScope.launch(Dispatchers.Main) {
                _walletBalance.value = balance
                _stats.value = gameStats
                _settings.value = activeSettings
            }
        }
    }

    private fun resetCounts() {
        _runningCount.value = 0
        _trueCount.value = 0.0
        cardsSeenInShoe = 0
        cardShoe.resetAndShuffle()
    }

    fun updateSettings(newSettings: BlackjackSettings) {
        _settings.value = newSettings
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateSettings(newSettings)
        }
        updateAdvice()
    }

    fun resetLifetimeStats() {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.resetStats()
            val gameStats = dbHelper.getStats()
            viewModelScope.launch(Dispatchers.Main) {
                _stats.value = gameStats
            }
        }
    }

    fun addBet(amount: Int) {
        if (_gameState.value != GameState.BETTING) return
        val current = _activeBet.value
        if (current + amount <= _walletBalance.value) {
            _activeBet.value = current + amount
        }
    }

    fun clearBet() {
        if (_gameState.value != GameState.BETTING) return
        _activeBet.value = 0
    }

    fun rebet() {
        if (_gameState.value != GameState.BETTING) return
        val prev = _previousBet.value
        if (prev > 0 && prev <= _walletBalance.value) {
            _activeBet.value = prev
        }
    }

    fun reloadWalletAccount() {
        if (_walletBalance.value == 0 && _gameState.value == GameState.BETTING) {
            viewModelScope.launch(Dispatchers.IO) {
                dbHelper.updateBalance(500)
                viewModelScope.launch(Dispatchers.Main) {
                    _walletBalance.value = 500
                    _adviceMessage.value = "Wallet reloaded with $500 loan. Play responsibly!"
                }
            }
        }
    }

    fun startGameDeal() {
        if (_gameState.value != GameState.BETTING) return
        val bet = _activeBet.value
        if (bet < 5) {
            _adviceMessage.value = "Minimum bet is $5."
            return
        }

        _gameState.value = GameState.PLAYER_TURN
        _previousBet.value = bet
        
        // Deduct bet from balance
        val nextBalance = _walletBalance.value - bet
        _walletBalance.value = nextBalance
        triggerMoneyAnimation("-$bet", false)
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateBalance(nextBalance)
        }

        // Clean hands
        dealerCards.clear()
        playerHands.clear()
        _currentHandIndex.value = 0
        isDealerSecondCardHidden.value = true
        roundResultText.value = ""

        // Check cards shoe depletion to trigger auto-shuffle at 75%
        if (cardShoe.getPenetrationPercent() > 75f) {
            resetCounts()
            _adviceMessage.value = "Card Shoe depleted. Shuffling 8 decks..."
        }

        // Deal Cards
        val pHand = BlackjackHand(bet = bet)
        playerHands.add(pHand)

        // Deal: Player 1, Dealer 1, Player 2, Dealer 2
        dealCardToPlayer(0)
        dealCardToDealer()
        dealCardToPlayer(0)
        dealCardToDealer()

        // Evaluatings
        evaluateInitialNaturalBJ()
    }

    private fun dealCardToPlayer(handIdx: Int) {
        if (handIdx >= playerHands.size) return
        val card = cardShoe.draw()
        playerHands[handIdx].cards.add(card)
        trackCardForCounting(card)
    }

    private fun dealCardToDealer() {
        val card = cardShoe.draw()
        // The second card dealt to dealer is hidden face down initially
        if (dealerCards.size == 1) {
            card.isFaceUp = false
        }
        dealerCards.add(card)
        if (card.isFaceUp) {
            trackCardForCounting(card)
        }
    }

    private fun trackCardForCounting(card: Card) {
        cardsSeenInShoe++
        val score = card.rank.value
        var value = 0
        if (score in 2..6) {
            value = 1
        } else if (score == 10 || card.rank == Rank.ACE) {
            value = -1
        }
        _runningCount.value += value

        // Calculate True Count = Running Count / Decks Remaining
        val totalCards = cardShoe.totalCount()
        val remainingCards = totalCards - cardsSeenInShoe
        val decksRemaining = remainingCards.toDouble() / 52.0
        val trueVal = if (decksRemaining > 0.0) {
            _runningCount.value / decksRemaining
        } else {
            _runningCount.value.toDouble()
        }
        // Round to 1 decimal place
        _trueCount.value = (trueVal * 10.0).roundToInt() / 10.0
    }

    private fun evaluateInitialNaturalBJ() {
        val pHand = playerHands[0]
        val isPlayerBJ = pHand.isBlackjack()
        val dealerScoreWithHidden = dealerCards[0].rank.value + dealerCards[1].rank.value

        // Standard Vegas Aces Check
        val isDealerBJ = (dealerCards[0].rank == Rank.ACE && dealerCards[1].rank.value == 10) ||
                         (dealerCards[0].rank.value == 10 && dealerCards[1].rank == Rank.ACE)

        if (isPlayerBJ || isDealerBJ) {
            // End Round Immediately
            if (dealerCards.size >= 2) {
                dealerCards[1] = dealerCards[1].copy(isFaceUp = true)
            }
            isDealerSecondCardHidden.value = false
            trackCardForCounting(dealerCards[1]) // Unhide second card for accurate count
            _gameState.value = GameState.PAYS_OUT
            resolvePaysOut()
        } else {
            updateAdvice()
        }
    }

    fun hit() {
        if (_gameState.value != GameState.PLAYER_TURN) return
        val idx = _currentHandIndex.value
        if (idx >= playerHands.size) return

        dealCardToPlayer(idx)
        val hand = playerHands[idx]

        if (hand.calculateScore() > 21) {
            hand.isBust = true
            hand.isDone = true
            moveToNextHand()
        } else {
            updateAdvice()
        }
    }

    fun stand() {
        if (_gameState.value != GameState.PLAYER_TURN) return
        val idx = _currentHandIndex.value
        if (idx >= playerHands.size) return

        playerHands[idx].isDone = true
        moveToNextHand()
    }

    fun doubleDown() {
        if (_gameState.value != GameState.PLAYER_TURN) return
        val idx = _currentHandIndex.value
        if (idx >= playerHands.size) return

        val hand = playerHands[idx]
        if (hand.cards.size != 2) return // Double down only available on first 2 cards

        // Check restrictions if enabled
        if (!_settings.value.doubleAnyTwo) {
            val score = hand.calculateScore()
            if (score !in 9..11) {
                _adviceMessage.value = "Active rules permit doubling on scores 9, 10, or 11 only."
                return
            }
        }

        if (_walletBalance.value >= hand.bet) {
            // Deduct addition bet matching original chip bet
            val doublingCost = hand.bet
            val nextBalance = _walletBalance.value - doublingCost
            _walletBalance.value = nextBalance
            triggerMoneyAnimation("-$doublingCost", false)
            viewModelScope.launch(Dispatchers.IO) {
                dbHelper.updateBalance(nextBalance)
            }

            hand.bet += doublingCost
            dealCardToPlayer(idx)

            if (hand.calculateScore() > 21) {
                hand.isBust = true
            }
            hand.isDone = true
            moveToNextHand()
        } else {
            _adviceMessage.value = "Insufficient balance to Double Down."
        }
    }

    fun split() {
        if (_gameState.value != GameState.PLAYER_TURN) return
        val idx = _currentHandIndex.value
        if (idx >= playerHands.size) return

        val hand = playerHands[idx]
        if (hand.cards.size != 2) return
        if (playerHands.size >= _settings.value.maxSplits + 1) {
            _adviceMessage.value = "Reached maximal allowable splits limit of ${_settings.value.maxSplits}."
            return
        }

        val val1 = hand.cards[0].rank.value
        val val2 = hand.cards[1].rank.value

        if (val1 != val2) {
            _adviceMessage.value = "Splitting requires initial pair of equal rank."
            return
        }

        if (_walletBalance.value >= hand.bet) {
            // Deduct secondary split bet from wallet
            val nextBalance = _walletBalance.value - hand.bet
            _walletBalance.value = nextBalance
            triggerMoneyAnimation("-${hand.bet}", false)
            viewModelScope.launch(Dispatchers.IO) {
                dbHelper.updateBalance(nextBalance)
            }

            // Perform structural split
            val cardToMove = hand.cards.removeAt(1)
            
            val splitHand = BlackjackHand(bet = hand.bet)
            splitHand.cards.add(cardToMove)
            
            // Deal a new card to original hand, and split hand
            playerHands.add(idx + 1, splitHand)
            dealCardToPlayer(idx)
            dealCardToPlayer(idx + 1)

            _adviceMessage.value = "Hand Split successfully. Play your active hands."
            updateAdvice()
        } else {
            _adviceMessage.value = "Insufficient balance to perform Split."
        }
    }

    private fun moveToNextHand() {
        val nextIdx = _currentHandIndex.value + 1
        if (nextIdx < playerHands.size) {
            _currentHandIndex.value = nextIdx
            updateAdvice()
        } else {
            // Dealer plays
            _gameState.value = GameState.DEALER_TURN
            playDealerTurn()
        }
    }

    private fun playDealerTurn() {
        viewModelScope.launch {
            if (dealerCards.size >= 2) {
                dealerCards[1] = dealerCards[1].copy(isFaceUp = true)
            }
            isDealerSecondCardHidden.value = false
            // Unhide dealer's second card for accuracy on screen and Hi-Lo calculation
            trackCardForCounting(dealerCards[1])

            // If all player hands are busted, dealer stands
            val allBust = playerHands.all { it.isBust }
            if (allBust) {
                _gameState.value = GameState.PAYS_OUT
                resolvePaysOut()
                return@launch
            }

            // Dealer hits until hitting hard/soft 17 requirements
            var dealerTotal = calculateHandScore(dealerCards)
            val shouldHitSoft17 = _settings.value.dealerSoft17Hit

            while (dealerTotal < 17 || (dealerTotal == 17 && isSoft17() && shouldHitSoft17)) {
                delay(800) // Delay to add visual suspense
                val card = cardShoe.draw()
                dealerCards.add(card)
                trackCardForCounting(card)
                dealerTotal = calculateHandScore(dealerCards)
            }

            delay(600)
            _gameState.value = GameState.PAYS_OUT
            resolvePaysOut()
        }
    }

    private fun isSoft17(): Boolean {
        var total = dealerCards.sumOf { it.rank.value }
        val aces = dealerCards.count { it.rank == Rank.ACE }
        return total == 17 && aces > 0
    }

    private fun calculateHandScore(cards: List<Card>): Int {
        var total = cards.sumOf { it.rank.value }
        var acesCount = cards.count { it.rank == Rank.ACE }
        while (total > 21 && acesCount > 0) {
            total -= 10
            acesCount--
        }
        return total
    }

    private fun resolvePaysOut() {
        val dealerScore = calculateHandScore(dealerCards)
        var totalWinnings = 0
        var totalWinStats = 0
        var outcomeSummary = ""

        val isDealerBJ = isHandBJ(dealerCards)

        playerHands.forEachIndexed { index, hand ->
            val playerScore = hand.calculateScore()
            val isPlayerBJ = hand.isBlackjack()

            if (hand.isBust) {
                viewModelScope.launch(Dispatchers.IO) {
                    dbHelper.recordGameRound("BUST", 0)
                }
                outcomeSummary += "H${index + 1}: BUST (-$${hand.bet}). "
            } else if (dealerScore > 21) {
                // Dealer busted
                val win = if (isPlayerBJ) (hand.bet * 2.5).toInt() else hand.bet * 2
                totalWinnings += win
                totalWinStats += win - hand.bet
                viewModelScope.launch(Dispatchers.IO) {
                    dbHelper.recordGameRound(if (isPlayerBJ) "BLACKJACK" else "WIN", win)
                }
                outcomeSummary += "H${index + 1}: WIN (+$${win - hand.bet}). "
            } else if (isPlayerBJ && !isDealerBJ) {
                // 3:2 standard casino blackjack payout
                val win = (hand.bet * 2.5).toInt()
                totalWinnings += win
                totalWinStats += win - hand.bet
                viewModelScope.launch(Dispatchers.IO) {
                    dbHelper.recordGameRound("BLACKJACK", win)
                }
                outcomeSummary += "H${index + 1}: BLACKJACK (+$${win - hand.bet})! "
            } else if (!isPlayerBJ && isDealerBJ) {
                viewModelScope.launch(Dispatchers.IO) {
                    dbHelper.recordGameRound("LOSS", 0)
                }
                outcomeSummary += "H${index + 1}: LOSS against Dealer BJ (-$${hand.bet}). "
            } else if (playerScore > dealerScore) {
                val win = hand.bet * 2
                totalWinnings += win
                totalWinStats += win - hand.bet
                viewModelScope.launch(Dispatchers.IO) {
                    dbHelper.recordGameRound("WIN", win)
                }
                outcomeSummary += "H${index + 1}: WIN (+$${win - hand.bet}). "
            } else if (playerScore < dealerScore) {
                viewModelScope.launch(Dispatchers.IO) {
                    dbHelper.recordGameRound("LOSS", 0)
                }
                outcomeSummary += "H${index + 1}: LOSS (-$${hand.bet}). "
            } else {
                // Push
                totalWinnings += hand.bet
                viewModelScope.launch(Dispatchers.IO) {
                    dbHelper.recordGameRound("PUSH", 0)
                }
                outcomeSummary += "H${index + 1}: PUSH (Bet Returned). "
            }
        }

        // Add back standard winnings
        if (totalWinnings > 0) {
            val netBalance = _walletBalance.value + totalWinnings
            _walletBalance.value = netBalance
            triggerMoneyAnimation("+$totalWinnings", true)
            viewModelScope.launch(Dispatchers.IO) {
                dbHelper.updateBalance(netBalance)
            }
        } else {
            val totalBet = playerHands.sumOf { it.bet }
            triggerMoneyAnimation("-$totalBet", false)
        }

        roundResultText.value = outcomeSummary.trim()
        _adviceMessage.value = "Payouts settled. Set chip stakes for the next round."
        
        // Refresh analytical telemetry
        loadDataFromDatabase()
        _gameState.value = GameState.PAYS_OUT
    }

    private fun isHandBJ(cards: List<Card>): Boolean {
        if (cards.size != 2) return false
        val total = cards.sumOf { it.rank.value }
        var aces = cards.count { it.rank == Rank.ACE }
        val finalScore = if (total > 21 && aces > 0) total - 10 else total
        return finalScore == 21
    }

    fun nextRound() {
        if (_gameState.value != GameState.PAYS_OUT) return
        _gameState.value = GameState.BETTING
        _activeBet.value = 0
        dealerCards.clear()
        playerHands.clear()
        roundResultText.value = ""
        _adviceMessage.value = "Stamps ready. Place standard chip wagers."
    }

    // Dynamic Basic Strategy Expert HUD advisor logic
    private fun updateAdvice() {
        val idx = _currentHandIndex.value
        if (_gameState.value != GameState.PLAYER_TURN || idx >= playerHands.size) {
            _adviceMessage.value = "Deal a hand first."
            return
        }

        val hand = playerHands[idx]
        val dealerUpCard = dealerCards.firstOrNull { it.isFaceUp }
        val dUnion = dealerUpCard?.rank?.value ?: 10

        val score = hand.calculateScore()
        val cards = hand.cards
        val isPair = cards.size == 2 && cards[0].rank.value == cards[1].rank.value

        _adviceMessage.value = getBasicStrategyAdvice(score, dUnion, isPair)
    }

    private fun getBasicStrategyAdvice(playerScore: Int, dealerUpCardValue: Int, isPair: Boolean): String {
        if (isPair) {
            return when (playerScore) {
                22 -> "Rule recommends: Always SPLIT Aces. Prompts optimal double hands."
                16 -> "Rule recommends: Always SPLIT 8s. Prevents hazardous 16 totals."
                20 -> "Rule recommends: Always STAND on 10s. Split is mathematically inferior."
                18 -> if (dealerUpCardValue in 2..9 && dealerUpCardValue != 7) "Rule recommends: SPLIT 9s against Dealer $dealerUpCardValue." else "Rule recommends: STAND."
                14 -> if (dealerUpCardValue in 2..7) "Rule recommends: SPLIT 7s against Dealer $dealerUpCardValue." else "Rule recommends: HIT."
                12 -> if (dealerUpCardValue in 2..6) "Rule recommends: SPLIT 6s against Dealer $dealerUpCardValue." else "Rule recommends: HIT."
                10 -> "Rule recommends: DOUBLE DOWN on pair of 5s if permitted, else HIT."
                else -> "Rule recommends: HIT low score pairs to build totals."
            }
        }

        if (playerScore >= 17) return "Rule recommends: STAND immediately. Bust probability is too high."
        if (playerScore <= 8) return "Rule recommends: HIT to construct card balance."

        return when (playerScore) {
            11 -> "Rule recommends: DOUBLE DOWN. Prompts high chance of pulling a 10-value."
            10 -> if (dealerUpCardValue in 2..9) "Rule recommends: DOUBLE DOWN against Dealer $dealerUpCardValue, otherwise HIT." else "Rule recommends: HIT."
            9 -> if (dealerUpCardValue in 3..6) "Rule recommends: DOUBLE DOWN against Dealer $dealerUpCardValue, otherwise HIT." else "Rule recommends: HIT."
            12 -> if (dealerUpCardValue in 4..6) "Rule recommends: STAND. Let Dealer absorb bust probabilities." else "Rule recommends: HIT."
            in 13..16 -> if (dealerUpCardValue in 2..6) "Rule recommends: STAND. Dealer is in bust zone (card index value 2-6)." else "Rule recommends: HIT."
            else -> "Rule recommends: HIT."
        }
    }
}
