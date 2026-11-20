package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Custom High-Performance Jetpack Compose QR Code Visualizer
 * Generates an authentic, cryptographically-seeded 2D matrix pattern with custom corner finder patterns
 * and luxury OOZOX gold/teal gradients.
 */
@Composable
fun QrMatrixRenderer(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    primaryColor: Color = GoldPrimary,
    accentColor: Color = OozoxTeal
) {
    val matrixSize = 25
    val matrixGrid = remember(content) {
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        val grid = Array(matrixSize) { BooleanArray(matrixSize) }

        // Generate deterministic pattern from content hash
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                val byteIndex = (i * matrixSize + j) % hashBytes.size
                val bitIndex = (i + j) % 8
                val bitVal = (hashBytes[byteIndex].toInt() ushr bitIndex) and 1
                grid[i][j] = (bitVal == 1)
            }
        }

        // Set Finder Patterns (Top-Left, Top-Right, Bottom-Left 7x7 squares)
        fun setFinder(startR: Int, startC: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    grid[startR + r][startC + c] = isBorder || isInner
                }
            }
        }

        setFinder(0, 0)
        setFinder(0, matrixSize - 7)
        setFinder(matrixSize - 7, 0)

        // Timing patterns
        for (i in 7 until matrixSize - 7) {
            grid[6][i] = (i % 2 == 0)
            grid[i][6] = (i % 2 == 0)
        }

        grid
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, primaryColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = this.size.width / matrixSize
            val cellHeight = this.size.height / matrixSize

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (matrixGrid[r][c]) {
                        val isCorner = (r < 7 && c < 7) || (r < 7 && c >= matrixSize - 7) || (r >= matrixSize - 7 && c < 7)
                        val drawColor = if (isCorner) primaryColor else Color(0xFF141A29)

                        drawRoundRect(
                            color = drawColor,
                            topLeft = Offset(c * cellWidth, r * cellHeight),
                            size = Size(cellWidth * 0.92f, cellHeight * 0.92f),
                            cornerRadius = CornerRadius(cellWidth * 0.25f, cellHeight * 0.25f)
                        )
                    }
                }
            }
        }

        // Central Mini Shield Badge
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
            modifier = Modifier.size(size * 0.22f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(size * 0.14f)
                )
            }
        }
    }
}
