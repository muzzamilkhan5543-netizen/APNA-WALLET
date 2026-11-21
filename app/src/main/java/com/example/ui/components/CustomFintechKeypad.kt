package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalWalletPalette

@Composable
fun CustomFintechKeypad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    secondaryLeftLabel: String = "CLR",
    onSecondaryLeftClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalWalletPalette.current
    val haptic = LocalHapticFeedback.current

    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(secondaryLeftLabel, "0", "BACK")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.headerRed.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .border(1.dp, palette.headerRed.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isAction = key == "BACK" || key == secondaryLeftLabel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isAction) palette.headerRed else palette.pageWhite)
                            .border(
                                width = 1.dp,
                                color = if (isAction) palette.headerRed else palette.headerRed.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("keypad_key_$key")
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                when (key) {
                                    "BACK" -> onBackspace()
                                    secondaryLeftLabel -> {
                                        if (onSecondaryLeftClick != null) {
                                            onSecondaryLeftClick()
                                        } else {
                                            onClear()
                                        }
                                    }
                                    else -> onDigitClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "BACK") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = palette.accentYellow
                            )
                        } else {
                            Text(
                                text = key,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isAction) 14.sp else 21.sp,
                                color = if (isAction) palette.accentYellow else palette.headerRed
                            )
                        }
                    }
                }
            }
        }
    }
}
