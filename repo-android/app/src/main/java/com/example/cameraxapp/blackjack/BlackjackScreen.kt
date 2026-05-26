package com.example.cameraxapp.blackjack

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

// Visual felt colors mapping
fun getFeltColor(styleId: Int): Color {
    return when (styleId) {
        0 -> Color(0xFF0F5A35) // Elegant Vegas Emerald
        1 -> Color(0xFF6B1B29) // High-Roller Burgundy
        2 -> Color(0xFF1E2D4A) // Royal Indigo Lounge
        else -> Color(0xFF0F5A35)
    }
}

// Reactive dealer emoji provider
@Composable
fun getDealerEmoji(gameState: GameState, dealerScore: Int): String {
    return when (gameState) {
        GameState.BETTING -> "🤵" // Croupier awaiting bets
        GameState.PLAYER_TURN -> "🤔" // Croupier observing player turn
        GameState.DEALER_TURN -> "🃏" // Croupier drawing cards
        GameState.PAYS_OUT -> {
            if (dealerScore > 21) "😮" // Dealer busted!
            else if (dealerScore >= 20) "😎" // Dealer got strong hand!
            else "🤝" // Standard payout face
        }
    }
}

@Composable
fun DealerAvatar(gameState: GameState, score: Int) {
    val emoji = getDealerEmoji(gameState = gameState, dealerScore = score)
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(Color.White.copy(alpha = 0.12f), CircleShape)
            .border(1.5.dp, Color(0xFFFFD700), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}

@Composable
fun getPlayerRankAndEmoji(balance: Int): Pair<String, String> {
    return when {
        balance >= 5000 -> Pair("HIGH ROLLER", "👑")
        balance >= 2500 -> Pair("DIAMOND VIP", "💎")
        balance >= 1500 -> Pair("HOT HAND", "🔥")
        else -> Pair("GUEST SEAT", "👤")
    }
}

@Composable
fun PlayerAvatar(balance: Int) {
    val (rank, emoji) = getPlayerRankAndEmoji(balance = balance)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                .border(1.2.dp, Color(0xFFE0B0FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Column {
            Text(
                text = rank,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFE0B0FF),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 10.sp
                )
            )
            Text(
                text = "Premium Seat",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Light
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackjackScreen(
    onBack: () -> Unit,
    viewModel: BlackjackViewModel
) {
    val gameState by viewModel.gameState
    val walletBalance by viewModel.walletBalance
    val activeBet by viewModel.activeBet
    val currentHandIndex by viewModel.currentHandIndex
    val runningCount by viewModel.runningCount
    val trueCount by viewModel.trueCount
    val adviceMessage by viewModel.adviceMessage
    val stats by viewModel.stats
    val settings by viewModel.settings
    val isSecondHidden by viewModel.isDealerSecondCardHidden
    val roundResultText by viewModel.roundResultText

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    val feltColor = getFeltColor(settings.tableColor)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "♠ BLACKJACK PRO",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return to Hub")
                    }
                },
                actions = {
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Lifetime Stats")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Table Configuration")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ActionFabMenu(
                gameState = gameState,
                activeBet = activeBet,
                canDouble = viewModel.playerHands.getOrNull(currentHandIndex)?.cards?.size == 2 && walletBalance >= (viewModel.playerHands.getOrNull(currentHandIndex)?.bet ?: 0),
                canSplit = viewModel.playerHands.getOrNull(currentHandIndex)?.cards?.size == 2 &&
                        viewModel.playerHands.getOrNull(currentHandIndex)?.cards?.get(0)?.rank?.value == viewModel.playerHands.getOrNull(currentHandIndex)?.cards?.get(1)?.rank?.value &&
                        walletBalance >= (viewModel.playerHands.getOrNull(currentHandIndex)?.bet ?: 0) &&
                        viewModel.playerHands.size < settings.maxSplits + 1,
                onDeal = { viewModel.startGameDeal() },
                onHit = { viewModel.hit() },
                onStand = { viewModel.stand() },
                onDouble = { viewModel.doubleDown() },
                onSplit = { viewModel.split() },
                onNextRound = { viewModel.nextRound() }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(feltColor.copy(alpha = 1.0f), feltColor.copy(alpha = 0.85f)),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // Row A: Optional Coaching & Cards Counting HUD Overlays
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (settings.showCardCountingHud) {
                        CardCountingHud(runningCount = runningCount, trueCount = trueCount)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    if (settings.showStrategyHud) {
                        StrategyAdviceHud(adviceText = adviceMessage)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Row B: Dealer Section
                DealerSection(
                    cards = viewModel.dealerCards,
                    isSecondCardHidden = isSecondHidden,
                    score = if (isSecondHidden && viewModel.dealerCards.size >= 2) {
                        viewModel.dealerCards.firstOrNull()?.rank?.value ?: 0
                    } else {
                        calculateHandScore(viewModel.dealerCards)
                    },
                    gameState = gameState
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Golden Felt Inscription Label
                CasinoFeltInscription(dealerRuleText = if (settings.dealerSoft17Hit) "Dealer Hits Soft 17" else "Dealer Stands on All 17s")

                Spacer(modifier = Modifier.height(24.dp))

                // Row C: Player Hands Section (Support Splits!)
                PlayerSection(
                    hands = viewModel.playerHands,
                    activeHandIndex = currentHandIndex,
                    gameState = gameState,
                    walletBalance = walletBalance
                )

                // Row D: Round Outcome Overlays
                if (roundResultText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.85f),
                            contentColor = Color.Yellow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Yellow.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = roundResultText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Row E: Wallet State & Betting Module
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        BankrollBar(
                            balance = walletBalance,
                            activeBet = activeBet,
                            onReloadWallet = { viewModel.reloadWalletAccount() }
                        )

                        // Floating transaction animation
                        val moneyAnim by viewModel.moneyAnimation
                        androidx.compose.animation.AnimatedVisibility(
                            visible = moneyAnim != null,
                            enter = fadeIn() + expandVertically() + slideInVertically(initialOffsetY = { 40 }),
                            exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { -60 }),
                            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-36).dp)
                        ) {
                            moneyAnim?.let { (text, isPositive) ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.9f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336))
                                ) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336),
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (gameState == GameState.BETTING) {
                        ChipBettingRail(
                            balance = walletBalance,
                            onAddBet = { viewModel.addBet(it) },
                            onClear = { viewModel.clearBet() },
                            onRebet = { viewModel.rebet() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reserve spacing so the scrolling content clears the overlaid bottom FABs
                Spacer(modifier = Modifier.height(115.dp))
            }

            // Slide out Overlay Dialogs configurations
            if (showSettingsDialog) {
                SettingsPanelDialog(
                    settings = settings,
                    onDismiss = { showSettingsDialog = false },
                    onSave = { updated ->
                        viewModel.updateSettings(updated)
                        showSettingsDialog = false
                    }
                )
            }

            if (showStatsDialog) {
                StatisticsPanelDialog(
                    stats = stats,
                    onDismiss = { showStatsDialog = false },
                    onReset = { viewModel.resetLifetimeStats() }
                )
            }

            if (gameState == GameState.PAYS_OUT) {
                val netGainLoss by viewModel.netGainLoss
                val netGainValue = netGainLoss
                if (netGainValue != null) {
                    val isGain = netGainValue > 0
                    val isLoss = netGainValue < 0
                    val isPush = netGainValue == 0

                    val color = when {
                        isGain -> Color(0xFF4CAF50) // Green
                        isLoss -> Color(0xFFF44336) // Red
                        else -> Color(0xFFFFD700) // Gold for push
                    }

                    val badgeIcon = when {
                        isGain -> "🏆" // Win/gain trophy
                        isLoss -> "💸" // Loss fly-away money
                        else -> "🤝" // Push shake hands
                    }

                    val badgeTitle = when {
                        isGain -> "ROUND WON!"
                        isLoss -> "ROUND LOST"
                        else -> "PUSH (EVEN)"
                    }

                    val amountText = when {
                        isGain -> "+$${netGainValue}"
                        isLoss -> "-$${kotlin.math.abs(netGainValue)}"
                        else -> "$0"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { viewModel.nextRound() },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.85f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(2.dp, color.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .padding(32.dp)
                                .shadow(12.dp, RoundedCornerShape(24.dp))
                                .clickable(enabled = false) {}
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 28.dp, horizontal = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Centered badge icon with transparent 0.6
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .graphicsLayer { alpha = 0.6f }
                                        .background(color.copy(alpha = 0.15f), CircleShape)
                                        .border(2.dp, color, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = badgeIcon,
                                        fontSize = 42.sp
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = badgeTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray,
                                            letterSpacing = 2.sp
                                        )
                                    )
                                    Text(
                                        text = amountText,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 36.sp,
                                            color = color
                                        )
                                    )
                                }

                                if (roundResultText.isNotEmpty()) {
                                    Text(
                                        text = roundResultText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center,
                                            color = Color.LightGray.copy(alpha = 0.8f)
                                        ),
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                Button(
                                    onClick = { viewModel.nextRound() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = color,
                                        contentColor = if (isLoss) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Text(
                                        "NEXT ROUND",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CHILD UI COMPONENTS
// -------------------------------------------------------------

@Composable
fun CardCountingHud(runningCount: Int, trueCount: Double) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Shoe Counting (Hi-Lo):",
                style = MaterialTheme.typography.labelMedium.copy(color = Color.LightGray)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Running: ${if (runningCount > 0) "+$runningCount" else runningCount}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (runningCount > 0) Color(0xFF4EEEEA) else if (runningCount < 0) Color(0xFFFF5252) else Color.White
                    )
                )
                Text(
                    "True Count: ${if (trueCount > 0) "+$trueCount" else trueCount}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (trueCount > 0) Color(0xFF4EEEEA) else if (trueCount < 0) Color(0xFFFF5252) else Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun StrategyAdviceHud(adviceText: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFDF7E7),
            contentColor = Color(0xFF8A6D3B)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFAEBCC), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Coach Strategy Hint",
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF8A6D3B)
            )
            Text(
                text = adviceText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun CasinoFeltInscription(dealerRuleText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "BLACKJACK PAYS 3 TO 2",
            style = MaterialTheme.typography.labelLarge.copy(
                color = Color(0xFFFFD700).copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "$dealerRuleText • Insurance Pays 2 to 1",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.35f),
                letterSpacing = 0.5.sp,
                fontSize = 9.sp
            )
        )
    }
}

fun calculateHandScore(cards: List<Card>): Int {
    var total = cards.sumOf { it.rank.value }
    var acesCount = cards.count { it.rank == Rank.ACE }
    while (total > 21 && acesCount > 0) {
        total -= 10
        acesCount--
    }
    return total
}

@Composable
fun DealerSection(
    cards: List<Card>,
    isSecondCardHidden: Boolean,
    score: Int,
    gameState: GameState
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (cards.isNotEmpty()) {
                        val animatedDealerScore by animateIntAsState(
                            targetValue = score,
                            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
                        )
                        Text(
                            text = animatedDealerScore.toString(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Color.Yellow,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            )
                        )
                    }
                    DealerAvatar(gameState = gameState, score = score)
                    Text(
                        "Dealer Hand",
                        style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }

            Spacer(modifier = Modifier.height(10.dp))

            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Dealer awaiting player bets...",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f))
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(cards) { index, card ->
                        PlayingCardView(card = card, index = index)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerSection(
    hands: List<BlackjackHand>,
    activeHandIndex: Int,
    gameState: GameState,
    walletBalance: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Render Player Avatar Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeHand = hands.getOrNull(activeHandIndex) ?: hands.firstOrNull()
                    if (activeHand != null && activeHand.cards.isNotEmpty()) {
                        val playerScore = activeHand.calculateScore()
                        val animatedPlayerScore by animateIntAsState(
                            targetValue = playerScore,
                            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
                        )
                        Text(
                            text = animatedPlayerScore.toString(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Color.Yellow,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            )
                        )
                    }
                    PlayerAvatar(balance = walletBalance)
                }
                Text(
                    text = "Seat #2",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        hands.forEachIndexed { idx, hand ->
            val isActive = gameState == GameState.PLAYER_TURN && idx == activeHandIndex

            val borderModifier = if (isActive) {
                Modifier.border(2.dp, Color(0xFFE0B0FF), RoundedCornerShape(12.dp))
            } else {
                Modifier
            }

            val alphaModifier = if (gameState == GameState.PLAYER_TURN && !isActive) {
                Modifier.alpha(0.3f)
            } else {
                Modifier
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(borderModifier)
                    .then(alphaModifier)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Your Hand ${if (hands.size > 1) "#${idx + 1}" else ""}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (isActive) Color(0xFFE0B0FF) else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "[ACTIVE]",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFE0B0FF),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Bet: $${hand.bet}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            val animatedHandScore by animateIntAsState(
                                targetValue = hand.calculateScore(),
                                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
                            )
                            Text(
                                "Score: $animatedHandScore",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(hand.cards) { index, card ->
                            PlayingCardView(card = card, index = index)
                        }
                    }
                }
            }
        }
    }
}

// Aspect ratio 3:2 card component supporting 3D flip flips and diagonal dealing entries
@Composable
fun CardBackView() {
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 108.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFFC0C0C0), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF8B2535), Color(0xFF5A121D))
                    ),
                    RoundedCornerShape(6.dp)
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♠ ♣\n♥ ♦",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            )
        }
    }
}

@Composable
fun CardFrontView(card: Card) {
    val contentColor = if (card.suit.isRed) Color(0xFFD32F2F) else Color(0xFF1F1F1F)
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 108.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.2.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(5.dp)
    ) {
        // Top-Left corner suit-rank index - scaled up for high readability by kids
        Text(
            text = "${card.rank.representation}\n${card.suit.symbol}",
            style = MaterialTheme.typography.titleSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                lineHeight = 14.sp
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Center giant card value and suit - extremely clear for kids and easy to read
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = card.rank.representation,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = contentColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                text = card.suit.symbol,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Bottom-Right corner suit-rank index inverted - scaled up
        Text(
            text = "${card.suit.symbol}\n${card.rank.representation}",
            style = MaterialTheme.typography.titleSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                lineHeight = 14.sp
            ),
            modifier = Modifier.align(Alignment.BottomEnd),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun PlayingCardView(card: Card, index: Int = 0) {
    var isDealt by remember { mutableStateOf(false) }
    LaunchedEffect(card) {
        delay(index * 60L) // Staggered dealing feel
        isDealt = true
    }

    val animatedOffset by animateDpAsState(
        targetValue = if (isDealt) 0.dp else (-100).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isDealt) 1f else 0f,
        animationSpec = tween(250)
    )

    // 3D flip angles
    val flipAngle by animateFloatAsState(
        targetValue = if (card.isFaceUp) 0f else 180f,
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = Modifier
            .offset(y = animatedOffset, x = animatedOffset / 2) // Slide diagonally simulating shoe draw
            .alpha(animatedAlpha)
            .graphicsLayer {
                rotationY = flipAngle
                cameraDistance = 12f * density
            }
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(8.dp),
                clip = false
            ),
        contentAlignment = Alignment.Center
    ) {
        if (flipAngle > 90f) {
            Box(
                modifier = Modifier.graphicsLayer { rotationY = 180f }
            ) {
                CardBackView()
            }
        } else {
            CardFrontView(card = card)
        }
    }
}

@Composable
fun BankrollBar(
    balance: Int,
    activeBet: Int,
    onReloadWallet: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Chip Wallet:",
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.LightGray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "$${balance}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance > 0) Color(0xFFB9F6CA) else Color(0xFFFF5252)
                    )
                )
                if (balance == 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onReloadWallet,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("RELOAD", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Round Stakes:",
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.LightGray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "$${activeBet}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow
                    )
                )
            }
        }
    }
}

// Interactive chips visually styled like casino tokens
@Composable
fun ChipBettingRail(
    balance: Int,
    onAddBet: (Int) -> Unit,
    onClear: () -> Unit,
    onRebet: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CasinoChip(value = 5, color = Color(0xFFE0E0E0), border = Color.DarkGray, onClick = { onAddBet(5) })
                CasinoChip(value = 10, color = Color(0xFF1E88E5), border = Color.Blue, onClick = { onAddBet(10) })
                CasinoChip(value = 25, color = Color(0xFF43A047), border = Color.Green, onClick = { onAddBet(25) })
                CasinoChip(value = 100, color = Color(0xFF000000), border = Color.White, labelColor = Color.White, onClick = { onAddBet(100) })
                CasinoChip(value = 500, color = Color(0xFFE53935), border = Color.DarkGray, onClick = { onAddBet(500) })
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Text("Clear Tables", fontSize = 11.sp)
                }
                Button(
                    onClick = onRebet,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                ) {
                    Text("Repeat Rebet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CasinoChip(
    value: Int,
    color: Color,
    border: Color,
    labelColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(3.dp, CircleShape)
            .background(color, CircleShape)
            .border(2.5.dp, border, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$$value",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = labelColor,
                fontSize = 11.sp
            )
        )
    }
}

// Controller buttons suite managing Vegas maneuvers
// Controller buttons suite managing Vegas maneuvers using elevated Floating Action Buttons (FAB)
@Composable
fun ActionControlBar(
    gameState: GameState,
    activeBet: Int,
    canDouble: Boolean,
    canSplit: Boolean,
    onDeal: () -> Unit,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onDouble: () -> Unit,
    onSplit: () -> Unit,
    onNextRound: () -> Unit
) {
    if (gameState == GameState.BETTING) {
        ExtendedFloatingActionButton(
            onClick = onDeal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .height(48.dp),
            containerColor = if (activeBet >= 5) Color(0xFFFFD700) else Color(0xFF555555),
            contentColor = if (activeBet >= 5) Color.Black else Color.LightGray,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🃏", fontSize = 18.sp)
                Text("DEAL HAND", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    } else if (gameState == GameState.PLAYER_TURN) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onHit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("➕", fontSize = 14.sp)
                        Text("HIT", fontWeight = FontWeight.Bold)
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = onStand,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    containerColor = Color(0xFF388E3C),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✋", fontSize = 14.sp)
                        Text("STAND", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val doubleColor = if (canDouble) Color(0xFFF57C00) else Color(0xFF424242)
                val doubleTextColor = if (canDouble) Color.White else Color.Gray
                ExtendedFloatingActionButton(
                    onClick = { if (canDouble) onDouble() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    containerColor = doubleColor,
                    contentColor = doubleTextColor,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = if (canDouble) 6.dp else 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⚡", fontSize = 14.sp)
                        Text("DOUBLE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                val splitColor = if (canSplit) Color(0xFF8E24AA) else Color(0xFF424242)
                val splitTextColor = if (canSplit) Color.White else Color.Gray
                ExtendedFloatingActionButton(
                    onClick = { if (canSplit) onSplit() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    containerColor = splitColor,
                    contentColor = splitTextColor,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = if (canSplit) 6.dp else 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✂", fontSize = 14.sp)
                        Text("SPLIT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    } else if (gameState == GameState.PAYS_OUT) {
        ExtendedFloatingActionButton(
            onClick = onNextRound,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            containerColor = Color(0xFFE0B0FF),
            contentColor = Color.Black,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🪙", fontSize = 16.sp)
                Text("SETTLE NEXT HAND", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    } else {
        // DEALER_TURN loading ticker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

// Custom Sheet to set Table settings and Rules configs online
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanelDialog(
    settings: BlackjackSettings,
    onDismiss: () -> Unit,
    onSave: (BlackjackSettings) -> Unit
) {
    var dealerSoft17 by remember { mutableStateOf(settings.dealerSoft17Hit) }
    var doubleAnyTwo by remember { mutableStateOf(settings.doubleAnyTwo) }
    var insuranceAvail by remember { mutableStateOf(settings.insuranceAvail) }
    var splitLimit by remember { mutableStateOf(settings.maxSplits.toFloat()) }
    var showStrategyHUD by remember { mutableStateOf(settings.showStrategyHud) }
    var showCardCountingHUD by remember { mutableStateOf(settings.showCardCountingHud) }
    var tableFeltChoice by remember { mutableStateOf(settings.tableColor) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Table Configuration Rules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Felt selector
                Text(
                    "Atmospheric Felt Styling",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(Color(0xFF0F5A35), RoundedCornerShape(4.dp))
                            .border(if (tableFeltChoice == 0) 2.dp else 0.dp, Color.White, RoundedCornerShape(4.dp))
                            .clickable { tableFeltChoice = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Emerald", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(Color(0xFF6B1B29), RoundedCornerShape(4.dp))
                            .border(if (tableFeltChoice == 1) 2.dp else 0.dp, Color.White, RoundedCornerShape(4.dp))
                            .clickable { tableFeltChoice = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Burgundy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(Color(0xFF1E2D4A), RoundedCornerShape(4.dp))
                            .border(if (tableFeltChoice == 2) 2.dp else 0.dp, Color.White, RoundedCornerShape(4.dp))
                            .clickable { tableFeltChoice = 2 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Indigo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle elements
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dealer Soft 17 Play", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Dealer Hits on Soft 17 index", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = dealerSoft17, onCheckedChange = { dealerSoft17 = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Double down limitations", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Allow double on any custom ranks", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = doubleAnyTwo, onCheckedChange = { doubleAnyTwo = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Insurance Options", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Prompt insurance when Dealer draws Ace", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = insuranceAvail, onCheckedChange = { insuranceAvail = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Basic Advisor HUD", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Hints optimal betting strategies", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = showStrategyHUD, onCheckedChange = { showStrategyHUD = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Card Counting Coach HUD", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Exposes running and true count statistics", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = showCardCountingHUD, onCheckedChange = { showCardCountingHUD = it })
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Total Allowable splits limit: ${splitLimit.roundToInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = splitLimit,
                        onValueChange = { splitLimit = it },
                        valueRange = 1f..4f,
                        steps = 2
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                BlackjackSettings(
                                    dealerSoft17Hit = dealerSoft17,
                                    doubleAnyTwo = doubleAnyTwo,
                                    insuranceAvail = insuranceAvail,
                                    maxSplits = splitLimit.roundToInt(),
                                    showStrategyHud = showStrategyHUD,
                                    showCardCountingHud = showCardCountingHUD,
                                    tableColor = tableFeltChoice
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

// Stats dialogue tracker
@Composable
fun StatisticsPanelDialog(
    stats: PlayerStats,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Lifetime Analytics Practice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Performance telemetry recorded locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(18.dp))

                StatsAttribute(label = "Hands Played Successfully", value = "${stats.handsPlayed}")
                StatsAttribute(label = "Absolute Wins Record", value = "${stats.wins}")
                StatsAttribute(label = "Absolute Losses Record", value = "${stats.losses}")
                StatsAttribute(label = "Pushes (Returned Stakes)", value = "${stats.pushes}")
                StatsAttribute(label = "Natural Blackjacks Won", value = "${stats.blackjacks}")
                StatsAttribute(label = "Maximum Score Won Payout", value = "$${stats.maxWin}")
                StatsAttribute(label = "Current Wins Streak", value = "${stats.currentStreak}")
                StatsAttribute(label = "Historic Maximum Winning Streak", value = "${stats.maxStreak}")

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Stats Data", tint = Color.Red)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close Panel")
                    }
                }
            }
        }
    }
}

@Composable
fun StatsAttribute(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

// Visual Floating Action Button (FAB) Speed Dial Menu for player turns
@Composable
fun ActionFabMenu(
    gameState: GameState,
    activeBet: Int,
    canDouble: Boolean,
    canSplit: Boolean,
    onDeal: () -> Unit,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onDouble: () -> Unit,
    onSplit: () -> Unit,
    onNextRound: () -> Unit
) {
    when (gameState) {
        GameState.BETTING -> {
            ExtendedFloatingActionButton(
                onClick = onDeal,
                containerColor = if (activeBet >= 5) Color(0xFFFFD700) else Color(0xFF424242),
                contentColor = if (activeBet >= 5) Color.Black else Color.Gray,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = if (activeBet >= 5) 6.dp else 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🃏", fontSize = 18.sp)
                    Text(
                        text = if (activeBet >= 5) "DEAL HAND ($activeBet)" else "PLACE BET (Min $5)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        GameState.PLAYER_TURN -> {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mini-FAB: SPLIT
                AnimatedVisibility(
                    visible = canSplit,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 30 })
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "SPLIT",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = onSplit,
                            containerColor = Color(0xFF8E24AA),
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Text("✂", fontSize = 16.sp)
                        }
                    }
                }

                // Mini-FAB: DOUBLE
                AnimatedVisibility(
                    visible = canDouble,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 30 })
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "DOUBLE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = onDouble,
                            containerColor = Color(0xFFF57C00),
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Text("⚡", fontSize = 16.sp)
                        }
                    }
                }

                // Mini-FAB: STAND
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "STAND",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = onStand,
                        containerColor = Color(0xFF388E3C),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Text("✋", fontSize = 16.sp)
                    }
                }

                // Main/Most usual FAB action = HIT
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "HIT (Most Play)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    FloatingActionButton(
                        onClick = onHit,
                        containerColor = Color(0xFF1976D2),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Text("➕", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        GameState.DEALER_TURN -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Dealer drawing...", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        GameState.PAYS_OUT -> {
            ExtendedFloatingActionButton(
                onClick = onNextRound,
                containerColor = Color(0xFFE0B0FF),
                contentColor = Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🪙", fontSize = 18.sp)
                    Text("SETTLE NEXT HAND", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
