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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalConfiguration
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
fun PlayerAvatar(
    balance: Int,
    activeBet: Int,
    onReloadWallet: () -> Unit
) {
    val (rank, emoji) = getPlayerRankAndEmoji(balance = balance)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar (circle with emoji symbol)
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                .border(1.2.dp, Color(0xFFE0B0FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }

        // Stakes and Wallet (Middle section)
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Wallet:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "$${balance}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (balance > 0) Color(0xFFB9F6CA) else Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Stakes:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "$${activeBet}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // Name / Rank on the right
        Column {
            Text(
                text = rank,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFE0B0FF),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 11.sp
                )
            )
            Text(
                text = "Premium Seat",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 9.sp,
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
    val previousBet by viewModel.previousBet
    val isGameOver by viewModel.isGameOver
    val sessionMaxWallet by viewModel.sessionMaxWallet

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    val feltColor = getFeltColor(settings.tableColor)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isCompact = screenWidth < 600
    val isCompactHeight = screenHeight < 480

    val tableSpacing = if (isCompactHeight) 8.dp else if (isCompact) 12.dp else 24.dp
    val outerPadding = if (isCompact) 8.dp else 16.dp
    val reserveSpacing = if (isCompactHeight) 60.dp else if (isCompact) 85.dp else 115.dp

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to Hub")
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
                onNextRound = { viewModel.nextRound() },
                balance = walletBalance,
                onAddBet = { viewModel.addBet(it) },
                isCompact = isCompact
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
                    .padding(outerPadding),
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
                    gameState = gameState,
                    isCompact = isCompact
                )

                Spacer(modifier = Modifier.height(tableSpacing))

                // Golden Felt Inscription Label
                CasinoFeltInscription(
                    dealerRuleText = if (settings.dealerSoft17Hit) "Dealer Hits Soft 17" else "Dealer Stands on All 17s",
                    isCompact = isCompact
                )

                Spacer(modifier = Modifier.height(tableSpacing))

                // Row C: Player Hands Section (Support Splits!)
                PlayerSection(
                    hands = viewModel.playerHands,
                    activeHandIndex = currentHandIndex,
                    gameState = gameState,
                    walletBalance = walletBalance,
                    activeBet = activeBet,
                    onReloadWallet = { viewModel.reloadWalletAccount() },
                    showStrategyHud = settings.showStrategyHud,
                    adviceMessage = adviceMessage,
                    isCompact = isCompact
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reserve spacing so the scrolling content clears the overlaid bottom FABs
                Spacer(modifier = Modifier.height(reserveSpacing))
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color(0xFFFF5252)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("LEAVE GAME", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (gameState == GameState.BETTING) {
                    if (walletBalance == 0) {
                        Button(
                            onClick = { viewModel.reloadWalletAccount() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC62828),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("RELOAD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.clearBet() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("CLEAR BET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.rebet() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("SAME BET ($$previousBet)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
                    highScores = viewModel.highScores,
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

                    val configuration = LocalConfiguration.current
                    val vmin = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
                    val circleSize = (vmin * 0.30f).dp

                    val alphaState = remember { Animatable(0f) }
                    LaunchedEffect(netGainValue) {
                        alphaState.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .graphicsLayer(alpha = alphaState.value)
                            .clickable { viewModel.nextRound() },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E),
                                contentColor = Color.White
                            ),
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .size(circleSize)
                                .shadow(8.dp, CircleShape)
                                .clickable(enabled = false) {}
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Centered badge icon
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color.copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, color.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = badgeIcon,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = badgeTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray,
                                            fontSize = 7.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = amountText,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = color
                                        )
                                    )
                                }

                                if (roundResultText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = roundResultText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center,
                                            fontSize = 6.sp,
                                            color = Color.LightGray.copy(alpha = 0.8f)
                                        ),
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Button(
                                    onClick = { viewModel.nextRound() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = color,
                                        contentColor = if (isLoss) Color.White else Color.Black
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(18.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "NEXT",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 7.sp,
                                            letterSpacing = 0.2.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isGameOver) {
                var playerNameInput by remember { mutableStateOf("") }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f))
                        .clickable(enabled = false) {}, // Intercept clicks and block content under it
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFF5252)),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "💸 GAME OVER 💸",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF5252),
                                    letterSpacing = 1.sp
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Your wallet is empty (balance below $5). You cannot make any more bets.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Peak Wallet Balance Achieved:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "$$sessionMaxWallet",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                text = "Enter your name for the Hall of Fame:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            OutlinedTextField(
                                value = playerNameInput,
                                onValueChange = { if (it.length <= 15) playerNameInput = it },
                                placeholder = { Text("Player Name", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFE0B0FF),
                                    unfocusedBorderColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.startNewGame(playerNameInput)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "START NEW GAME ($1000)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
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
fun CompactStrategyAdvice(adviceText: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFDF7E7), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFFAEBCC), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Coach Strategy Hint",
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF8A6D3B)
            )
            Text(
                text = adviceText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF8A6D3B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
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
fun CasinoFeltInscription(dealerRuleText: String, isCompact: Boolean = false) {
    val titleSize = if (isCompact) 9.sp else 11.sp
    val subTextSize = if (isCompact) 7.sp else 9.sp
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
                fontSize = titleSize
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "$dealerRuleText • Insurance Pays 2 to 1",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.35f),
                letterSpacing = 0.5.sp,
                fontSize = subTextSize
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
fun GameAvatarCircle(
    emoji: String,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .size(108.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 44.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StakesWalletCard(
    walletBalance: Int,
    activeBet: Int,
    onReloadWallet: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.2.dp, Color(0xFFE0B0FF).copy(alpha = 0.4f)),
        modifier = Modifier.size(width = 110.dp, height = 108.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stakes (the stacks) at top
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Stakes",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "$${activeBet}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Yellow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                )
            }

            // Wallet at bottom
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Wallet",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "$${walletBalance}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (walletBalance > 0) Color(0xFFB9F6CA) else Color(0xFFFF5252),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

@Composable
fun DealerSection(
    cards: List<Card>,
    isSecondCardHidden: Boolean,
    score: Int,
    gameState: GameState,
    isCompact: Boolean = false
) {
    val animatedDealerScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
    )

    val isBust = score > 21
    val isBlackjack = !isSecondCardHidden && cards.size == 2 && score == 21

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 10.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCompact) {
                // Compact Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val emoji = getDealerEmoji(gameState = gameState, dealerScore = score)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                .border(1.5.dp, Color(0xFFFFD700), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 16.sp)
                        }
                        Text(
                            text = "DEALER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        )
                    }

                    // Score pill
                    val scoreBg = when {
                        isBust -> Color(0xFF421515)
                        isBlackjack -> Color(0xFF4C3B12)
                        else -> Color.Black.copy(alpha = 0.6f)
                    }
                    val scoreColor = when {
                        isBust -> Color(0xFFFF5252)
                        isBlackjack -> Color(0xFFFFD700)
                        else -> Color.Yellow
                    }
                    Box(
                        modifier = Modifier
                            .background(scoreBg, RoundedCornerShape(6.dp))
                            .border(1.5.dp, scoreColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                isBust -> "BUSTED ($score)"
                                isBlackjack -> "BLACKJACK"
                                else -> "TOTAL: $score"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable/centered row of cards underneath
                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Awaiting Bets",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(cards) { index, card ->
                            PlayingCardView(
                                card = card,
                                index = index,
                                width = 56.dp,
                                height = 84.dp
                            )
                            if (index < cards.size - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "DEALER",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val emoji = getDealerEmoji(gameState = gameState, dealerScore = score)
                        GameAvatarCircle(emoji = emoji, borderColor = Color(0xFFFFD700))
                    }
                    item {
                        Spacer(modifier = Modifier.width(110.dp))
                    }
                    item {
                        TotalCard(
                            scoreText = if (cards.isEmpty()) "0" else animatedDealerScore.toString(),
                            isBust = isBust,
                            isBlackjack = isBlackjack
                        )
                    }
                    if (cards.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(width = 72.dp, height = 108.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Awaiting Bets",
                                    fontSize = 11.sp,
                                    color = Color.LightGray.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(cards) { index, card ->
                            PlayingCardView(card = card, index = index)
                        }
                    }
                }
            }
        }
    }
}

fun getPlayerAvatarEmoji(advice: String, walletBalance: Int): String {
    val upperAdvice = advice.uppercase()
    return when {
        upperAdvice.contains("RELOAD") -> "🤕"
        upperAdvice.contains("MINIMUM") -> "🪙"
        upperAdvice.contains("DOUBLE") -> "🚀"
        upperAdvice.contains("SPLIT") -> "✂️"
        upperAdvice.contains("STAND") -> "✋"
        upperAdvice.contains("HIT") -> "👊"
        upperAdvice.contains("PLACE") && upperAdvice.contains("BET") -> "🪙"
        upperAdvice.contains("PAYOUT") || upperAdvice.contains("WON") -> "🥳"
        upperAdvice.contains("LOST") -> "😭"
        else -> {
            when {
                walletBalance >= 5000 -> "👑"
                walletBalance >= 2500 -> "💎"
                walletBalance >= 1500 -> "🔥"
                else -> "👤"
            }
        }
    }
}

@Composable
fun AvatarCard(
    gameState: GameState,
    adviceMessage: String,
    walletBalance: Int,
    activeBet: Int
) {
    val emoji = getPlayerAvatarEmoji(adviceMessage, walletBalance)
    
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 108.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFFE0B0FF), RoundedCornerShape(8.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "PLAYER",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFE0B0FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
            )
            
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.animateContentSize()
            )
            
            Text(
                text = "$${walletBalance}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFB9F6CA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun TotalCard(
    scoreText: String,
    isBust: Boolean,
    isBlackjack: Boolean
) {
    val borderColor = when {
        isBust -> Color(0xFFFF5252)
        isBlackjack -> Color(0xFFFFD700)
        else -> Color.Yellow
    }
    
    val bgColor = when {
        isBust -> Color(0xFF421515)
        isBlackjack -> Color(0xFF4C3B12)
        else -> Color.Black.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(width = 112.dp, height = 108.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
            )
            
            Text(
                text = scoreText,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = if (isBust) Color(0xFFFF5252) else if (isBlackjack) Color(0xFFFFD700) else Color.Yellow,
                    fontWeight = FontWeight.Black,
                    fontSize = if (scoreText.length > 3) 18.sp else 26.sp
                )
            )
            
            Text(
                text = if (isBust) "BUSTED" else if (isBlackjack) "NATURAL" else "POINTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isBust) Color(0xFFFF5252) else if (isBlackjack) Color(0xFFFFD700) else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PlayerSection(
    hands: List<BlackjackHand>,
    activeHandIndex: Int,
    gameState: GameState,
    walletBalance: Int,
    activeBet: Int,
    onReloadWallet: () -> Unit,
    showStrategyHud: Boolean,
    adviceMessage: String,
    isCompact: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 14.dp)
    ) {
        val (rank, rankEmoji) = getPlayerRankAndEmoji(balance = walletBalance)

        if (hands.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(if (isCompact) 10.dp else 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PLAYER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isCompact) 18.sp else 28.sp,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "($rankEmoji)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color(0xFFE0B0FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isCompact) 12.sp else 14.sp
                                )
                            )
                        }
                        if (showStrategyHud && adviceMessage.isNotEmpty()) {
                            CompactStrategyAdvice(adviceText = adviceMessage)
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

                    if (isCompact) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Wallet: $${walletBalance}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFB9F6CA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = "Bet: $${activeBet}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.Yellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Text(
                                text = "Awaiting Bet",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                val emoji = getPlayerAvatarEmoji(adviceMessage, walletBalance)
                                GameAvatarCircle(emoji = emoji, borderColor = Color(0xFFE0B0FF))
                            }

                            item {
                                StakesWalletCard(
                                    walletBalance = walletBalance,
                                    activeBet = activeBet,
                                    onReloadWallet = onReloadWallet
                                )
                            }

                            item {
                                TotalCard(
                                    scoreText = "0",
                                    isBust = false,
                                    isBlackjack = false
                                )
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 108.dp)
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No Cards",
                                        fontSize = 11.sp,
                                        color = Color.LightGray.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
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
                         modifier = Modifier.padding(if (isCompact) 10.dp else 14.dp),
                         horizontalAlignment = Alignment.CenterHorizontally
                     ) {
                         if (isCompact) {
                             // Compact Header Row for Player Hands
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Row(
                                     verticalAlignment = Alignment.CenterVertically,
                                     horizontalArrangement = Arrangement.spacedBy(6.dp)
                                 ) {
                                     val emoji = getPlayerAvatarEmoji(adviceMessage, walletBalance)
                                     Box(
                                         modifier = Modifier
                                             .size(32.dp)
                                             .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                             .border(1.5.dp, Color(0xFFE0B0FF), CircleShape),
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Text(emoji, fontSize = 16.sp)
                                     }
                                     Text(
                                         text = "PLAYER" + if (hands.size > 1) " #${idx + 1}" else "",
                                         style = MaterialTheme.typography.titleMedium.copy(
                                             fontWeight = FontWeight.Black,
                                             fontSize = 18.sp,
                                             color = if (isActive) Color(0xFFE0B0FF) else Color.White
                                         )
                                     )
                                     if (isActive) {
                                         Box(
                                             modifier = Modifier
                                                 .background(Color(0xFFE0B0FF).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                 .padding(horizontal = 6.dp, vertical = 2.dp)
                                         ) {
                                             Text(
                                                 text = "ACTIVE",
                                                 style = MaterialTheme.typography.labelSmall.copy(
                                                     color = Color(0xFFE0B0FF),
                                                     fontWeight = FontWeight.Bold,
                                                     fontSize = 8.sp
                                                 )
                                             )
                                         }
                                     }
                                 }
                                 if (isActive && showStrategyHud && adviceMessage.isNotEmpty()) {
                                     CompactStrategyAdvice(adviceText = adviceMessage)
                                 }
                             }

                             Spacer(modifier = Modifier.height(6.dp))

                             // Stats Row
                             Row(
                                 modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Row(
                                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                                 ) {
                                     Text(
                                         text = "Wallet: $${walletBalance}",
                                         style = MaterialTheme.typography.labelSmall.copy(
                                             color = Color(0xFFB9F6CA),
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 11.sp
                                         )
                                     )
                                     Text(
                                         text = "Bet: $${hand.bet}",
                                         style = MaterialTheme.typography.labelSmall.copy(
                                             color = Color.Yellow,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 11.sp
                                         )
                                     )
                                 }

                                 // Score Badge
                                 val scoreDisplay = hand.getScoreDisplay()
                                 val isHandBust = hand.isBust
                                 val isHandBJ = hand.isBlackjack()
                                 val scoreBg = when {
                                     isHandBust -> Color(0xFF421515)
                                     isHandBJ -> Color(0xFF4C3B12)
                                     else -> Color.Black.copy(alpha = 0.6f)
                                 }
                                 val scoreColor = when {
                                     isHandBust -> Color(0xFFFF5252)
                                     isHandBJ -> Color(0xFFFFD700)
                                     else -> Color.Yellow
                                 }
                                 Box(
                                     modifier = Modifier
                                         .background(scoreBg, RoundedCornerShape(6.dp))
                                         .border(1.dp, scoreColor, RoundedCornerShape(6.dp))
                                         .padding(horizontal = 8.dp, vertical = 3.dp)
                                 ) {
                                     Text(
                                         text = when {
                                             isHandBust -> "BUST ($scoreDisplay)"
                                             isHandBJ -> "BJ"
                                             else -> "$scoreDisplay PTS"
                                         },
                                         style = MaterialTheme.typography.labelSmall.copy(
                                             fontWeight = FontWeight.Bold,
                                             color = scoreColor,
                                             fontSize = 10.sp
                                         )
                                     )
                                 }
                             }

                             Spacer(modifier = Modifier.height(8.dp))

                             // Responsive Card list
                             LazyRow(
                                 horizontalArrangement = Arrangement.Center,
                                 modifier = Modifier.fillMaxWidth(),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 itemsIndexed(hand.cards) { index, card ->
                                     PlayingCardView(
                                         card = card,
                                         index = index,
                                         width = 56.dp,
                                         height = 84.dp
                                     )
                                     if (index < hand.cards.size - 1) {
                                         Spacer(modifier = Modifier.width(4.dp))
                                     }
                                 }
                             }
                         } else {
                         Row(
                             modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Row(
                                 verticalAlignment = Alignment.CenterVertically,
                                 horizontalArrangement = Arrangement.spacedBy(12.dp)
                             ) {
                                 Text(
                                     text = "PLAYER" + if (hands.size > 1) " #${idx + 1}" else "",
                                     style = MaterialTheme.typography.headlineMedium.copy(
                                         fontWeight = FontWeight.Black,
                                         fontSize = 28.sp,
                                         color = if (isActive) Color(0xFFE0B0FF) else Color.White
                                     )
                                 )
                                 Text(
                                     text = "($rank $rankEmoji)",
                                     style = MaterialTheme.typography.titleSmall.copy(
                                         color = Color(0xFFE0B0FF),
                                         fontWeight = FontWeight.Bold,
                                         fontSize = 14.sp
                                     )
                                 )
                                 if (isActive) {
                                     Box(
                                         modifier = Modifier
                                             .background(Color(0xFFE0B0FF).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                             .padding(horizontal = 8.dp, vertical = 2.dp)
                                     ) {
                                         Text(
                                             text = "ACTIVE",
                                             style = MaterialTheme.typography.labelSmall.copy(
                                                 color = Color(0xFFE0B0FF),
                                                 fontWeight = FontWeight.Bold,
                                                 fontSize = 10.sp
                                             )
                                         )
                                    }
                                }
                            }
                            if (isActive && showStrategyHud && adviceMessage.isNotEmpty()) {
                                CompactStrategyAdvice(adviceText = adviceMessage)
                            }
                        }

                         Spacer(modifier = Modifier.height(10.dp))

                         LazyRow(
                             horizontalArrangement = Arrangement.spacedBy(8.dp),
                             modifier = Modifier.fillMaxWidth(),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             item {
                                 val emoji = getPlayerAvatarEmoji(adviceMessage, walletBalance)
                                 GameAvatarCircle(
                                     emoji = emoji,
                                     borderColor = if (isActive) Color(0xFFE0B0FF) else Color.White.copy(alpha = 0.4f)
                                 )
                             }

                             item {
                                 StakesWalletCard(
                                     walletBalance = walletBalance,
                                     activeBet = hand.bet,
                                     onReloadWallet = onReloadWallet
                                 )
                             }

                             item {
                                 TotalCard(
                                     scoreText = hand.getScoreDisplay(),
                                     isBust = hand.isBust,
                                     isBlackjack = hand.isBlackjack()
                                 )
                             }

                             itemsIndexed(hand.cards) { index, card ->
                                 PlayingCardView(card = card, index = index)
                             }
                         }
                         }
                     }
                }
            }
        }
    }
}

// Aspect ratio 3:2 card component supporting 3D flip flips and diagonal dealing entries
@Composable
fun CardBackView(width: Dp = 72.dp, height: Dp = 108.dp) {
    val padding = if (width < 60.dp) 2.dp else 4.dp
    val fontSize = if (width < 60.dp) 8.sp else 11.sp
    val lineHeight = if (width < 60.dp) 10.sp else 14.sp
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFFC0C0C0), RoundedCornerShape(8.dp))
            .padding(padding)
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
                    fontSize = fontSize,
                    lineHeight = lineHeight
                )
            )
        }
    }
}

@Composable
fun CardFrontView(card: Card, width: Dp = 72.dp, height: Dp = 108.dp) {
    val contentColor = if (card.suit.isRed) Color(0xFFD32F2F) else Color(0xFF1F1F1F)
    val padding = if (width < 60.dp) 3.dp else 5.dp
    val topLeftFontSize = if (width < 60.dp) 9.sp else 13.sp
    val topLeftLineHeight = if (width < 60.dp) 10.sp else 14.sp
    val centerRankFontSize = if (width < 60.dp) 22.sp else 32.sp
    val centerSuitFontSize = if (width < 60.dp) 12.sp else 18.sp

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.2.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(padding)
    ) {
        // Top-Left corner suit-rank index - scaled up for high readability by kids
        Text(
            text = "${card.rank.representation}\n${card.suit.symbol}",
            style = MaterialTheme.typography.titleSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = topLeftFontSize,
                lineHeight = topLeftLineHeight
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
                    fontSize = centerRankFontSize,
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                text = card.suit.symbol,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = contentColor,
                    fontSize = centerSuitFontSize,
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
                fontSize = topLeftFontSize,
                lineHeight = topLeftLineHeight
            ),
            modifier = Modifier.align(Alignment.BottomEnd),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun PlayingCardView(card: Card, index: Int = 0, width: Dp = 72.dp, height: Dp = 108.dp) {
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
                CardBackView(width = width, height = height)
            }
        } else {
            CardFrontView(card = card, width = width, height = height)
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
    highScores: List<Pair<String, Int>>,
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
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
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

                if (highScores.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "🏆 HALL OF FAME RANKINGS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            highScores.forEachIndexed { rank, score ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${rank + 1}. ${score.first}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rank == 0) Color(0xFFFFD700) else Color.White
                                    )
                                    Text(
                                        text = "$${score.second}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB9F6CA)
                                    )
                                }
                            }
                        }
                    }
                }

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
    onNextRound: () -> Unit,
    balance: Int,
    onAddBet: (Int) -> Unit,
    isCompact: Boolean = false
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(gameState) {
        isVisible = gameState == GameState.BETTING
    }
    val fanOutFraction by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    when (gameState) {
        GameState.BETTING -> {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.wrapContentSize()
            ) {
                // Semi-circular Chips
                if (fanOutFraction > 0f) {
                    val radius = 125.dp * fanOutFraction
                    // Chips properties: Value, Color, Border Color
                    val chips = listOf(
                        Triple(5, Color(0xFFE0E0E0), Color.DarkGray),
                        Triple(10, Color(0xFF1E88E5), Color.Blue),
                        Triple(25, Color(0xFF43A047), Color.Green),
                        Triple(100, Color(0xFF000000), Color.White),
                        Triple(500, Color(0xFFE53935), Color.DarkGray)
                    )

                    chips.forEachIndexed { index, chip ->
                        val angleInRad = (index * 22.5) * Math.PI / 180.0
                        val xOffset = (-5.dp.value - (radius.value * Math.cos(angleInRad))).dp
                        val yOffset = (-5.dp.value - (radius.value * Math.sin(angleInRad))).dp

                        Box(
                            modifier = Modifier
                                .offset(x = xOffset, y = yOffset)
                                .graphicsLayer {
                                    alpha = fanOutFraction
                                    scaleX = fanOutFraction
                                    scaleY = fanOutFraction
                                }
                        ) {
                            val isDark = chip.first == 100
                            CasinoChip(
                                value = chip.first,
                                color = chip.second,
                                border = chip.third,
                                labelColor = if (isDark) Color.White else Color.Black,
                                onClick = { onAddBet(chip.first) }
                            )
                        }
                    }
                }

                // Main circular Deal button with horizontal label Row matching player actions structure
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (activeBet >= 5) "DEAL HAND ($activeBet)" else "PLACE BET (Min $5)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    FloatingActionButton(
                        onClick = onDeal,
                        containerColor = if (activeBet >= 5) Color(0xFFFFD700) else Color(0xFF424242),
                        contentColor = if (activeBet >= 5) Color.Black else Color.Gray,
                        shape = CircleShape
                    ) {
                        Text("🃏", fontSize = 24.sp)
                    }
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
