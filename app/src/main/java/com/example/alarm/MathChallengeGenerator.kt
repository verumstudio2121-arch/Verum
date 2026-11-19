package com.example.alarm

import kotlin.random.Random

data class MathProblem(
    val expression: String,
    val answer: Int
)

enum class MathDifficulty(
    val id: String,
    val label: String,
    val sampleEquation: String
) {
    EASY("Easy", "Easy", "14 + 8 = ?"),
    MEDIUM("Medium", "Medium", "7 × 6 + 9 = ?"),
    HARD("Hard", "Hard", "18 × 7 − 24 = ?");

    companion object {
        fun fromId(id: String): MathDifficulty {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MEDIUM
        }
    }
}

object MathChallengeGenerator {

    fun sampleEquationFor(difficultyId: String): String {
        return MathDifficulty.fromId(difficultyId).sampleEquation
    }

    fun generateProblems(
        difficultyId: String,
        count: Int,
        random: Random = Random.Default
    ): List<MathProblem> {
        val safeCount = count.coerceIn(1, 3)
        val difficulty = MathDifficulty.fromId(difficultyId)
        val result = mutableListOf<MathProblem>()
        val usedExpressions = mutableSetOf<String>()

        var attempts = 0
        while (result.size < safeCount && attempts < 30) {
            attempts++
            val candidate = generateSingle(difficulty, random)
            if (usedExpressions.add(candidate.expression)) {
                result.add(candidate)
            }
        }
        while (result.size < safeCount) {
            result.add(generateSingle(difficulty, random))
        }
        return result
    }

    private fun generateSingle(difficulty: MathDifficulty, random: Random): MathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                if (random.nextBoolean()) {
                    val a = random.nextInt(4, 30)
                    val b = random.nextInt(3, 25)
                    MathProblem("$a + $b", a + b)
                } else {
                    val a = random.nextInt(14, 39)
                    val b = random.nextInt(3, a)
                    MathProblem("$a − $b", a - b)
                }
            }

            MathDifficulty.MEDIUM -> {
                when (random.nextInt(3)) {
                    0 -> {
                        val a = random.nextInt(24, 79)
                        val b = random.nextInt(14, 59)
                        MathProblem("$a + $b", a + b)
                    }
                    1 -> {
                        val a = random.nextInt(42, 95)
                        val b = random.nextInt(15, a - 4)
                        MathProblem("$a − $b", a - b)
                    }
                    else -> {
                        val a = random.nextInt(4, 10)
                        val b = random.nextInt(3, 10)
                        val product = a * b
                        val c = random.nextInt(4, 24)
                        if (random.nextBoolean() || product <= c + 2) {
                            MathProblem("$a × $b + $c", product + c)
                        } else {
                            MathProblem("$a × $b − $c", product - c)
                        }
                    }
                }
            }

            MathDifficulty.HARD -> {
                if (random.nextBoolean()) {
                    val a = random.nextInt(12, 25)
                    val b = random.nextInt(4, 10)
                    val product = a * b
                    val c = random.nextInt(11, minOf(49, product - 5))
                    if (random.nextBoolean()) {
                        MathProblem("$a × $b + $c", product + c)
                    } else {
                        MathProblem("$a × $b − $c", product - c)
                    }
                } else {
                    val a = random.nextInt(6, 13)
                    val b = random.nextInt(4, 10)
                    val c = random.nextInt(3, 9)
                    val d = random.nextInt(3, 8)
                    MathProblem("$a × $b + $c × $d", (a * b) + (c * d))
                }
            }
        }
    }
}
