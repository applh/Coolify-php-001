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
    fun getPenetrationPercent(): Float {
        val total = totalCount().toFloat()
        if (total == 0f) return 0f
        return ((total - remainingCount()) / total) * 100f
    }
}

data class BlackjackHand(
    val cards: MutableList<Card> = mutableListOf(),
    var bet: Int = 0,
    var isDone: Boolean = false,
    var isBust: Boolean = false
) {
    fun calculateScore(): Int {
        var total = cards.sumOf { it.rank.value }
        var acesCount = cards.count { it.rank == Rank.ACE }
        
        while (total > 21 && acesCount > 0) {
            total -= 10
            acesCount--
        }
        return total
    }

    fun hasSoftAce(): Boolean {
        var total = cards.sumOf { it.rank.value }
        var acesCount = cards.count { it.rank == Rank.ACE }
        var adjusted = 0
        while (total > 21 && acesCount > 0) {
            total -= 10
            acesCount--
            adjusted++
        }
        val remainingAces = cards.count { it.rank == Rank.ACE } - adjusted
        return remainingAces > 0 && total <= 21
    }

    fun getScoreDisplay(): String {
        val score = calculateScore()
        if (score <= 21 && hasSoftAce()) {
            val hardScore = score - 10
            return "$hardScore/$score"
        }
        return score.toString()
    }

    fun isBlackjack(): Boolean = cards.size == 2 && calculateScore() == 21
}
