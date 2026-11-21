package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldChip

@Composable
fun CardVisual(
    nickname: String,
    uid: String,
    manufacturer: String,
    technology: String,
    cardStandard: String,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    statusBadgeText: String? = null,
    statusBadgeColor: Color = Color(0xFF10B981),
    modifier: Modifier = Modifier
) {
    val cardGradient = when {
        technology.contains("iCLASS", ignoreCase = true) || nickname.contains("Science", ignoreCase = true) || nickname.contains("Lab", ignoreCase = true) -> {
            Brush.linearGradient(
                colors = listOf(Color(0xFF0F766E), Color(0xFF0D9488), Color(0xFF0284C7))
            )
        }
        technology.contains("DESFire", ignoreCase = true) || technology.contains("IsoDep", ignoreCase = true) || nickname.contains("Security", ignoreCase = true) -> {
            Brush.linearGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF334155))
            )
        }
        else -> {
            Brush.linearGradient(
                colors = listOf(Color(0xFF312E81), Color(0xFF4338CA), Color(0xFF6366F1))
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .testTag("card_visual_item"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardGradient)
                .drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    val circleAlpha = 0.07f
                    drawCircle(
                        color = Color.White.copy(alpha = circleAlpha),
                        radius = size.width * 0.45f,
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        style = Stroke(width = strokeWidth)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = circleAlpha),
                        radius = size.width * 0.60f,
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        style = Stroke(width = strokeWidth)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.04f),
                        radius = size.width * 0.50f,
                        center = Offset(size.width * 0.10f, size.height * 0.90f),
                        style = Stroke(width = strokeWidth * 2)
                    )
                }
                .padding(18.dp)
        ) {
            // Top Header: Institution Icon, Title, Status Badge & 48dp Favorite Button (zero overlap!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (technology.contains("iCLASS", ignoreCase = true)) Icons.Default.Security else Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = manufacturer,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!statusBadgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusBadgeColor
                        ) {
                            Text(
                                text = statusBadgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (onFavoriteToggle != null) {
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("favorite_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                        tint = if (isFavorite) GoldChip else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Middle: Gold Smart Chip + Contactless Wave Icon
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldChip)
                        .border(1.dp, Color(0xFFB45309), RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .border(0.7.dp, Color(0xFF92400E).copy(alpha = 0.8f))
                    )
                }

                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(30.dp)
                )
            }

            // Bottom Bar: Card UID + Technology Badge + Standard
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "CARD UID",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = uid,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = if (uid.length > 14) 12.sp else 15.sp,
                    letterSpacing = if (uid.length > 14) 0.5.sp else 1.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = technology,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = cardStandard,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
