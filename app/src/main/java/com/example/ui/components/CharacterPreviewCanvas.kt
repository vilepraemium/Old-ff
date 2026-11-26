package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.GameCatalog
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DiamondCyan

@Composable
fun CharacterPreviewCanvas(
    hatId: String,
    maskId: String,
    topId: String,
    bottomId: String,
    shoesId: String,
    weaponSkinId: String,
    modifier: Modifier = Modifier,
) {
    val hat = GameCatalog.findItem(hatId)
    val mask = GameCatalog.findItem(maskId)
    val top = GameCatalog.findItem(topId)
    val bottom = GameCatalog.findItem(bottomId)
    val shoes = GameCatalog.findItem(shoesId)
    val weaponSkin = GameCatalog.findItem(weaponSkinId)

    val transition = rememberInfiniteTransition(label = "characterIdle")
    val breathe by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheOffset",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val scale = minOf(w / 220f, h / 300f)
        val topY = (h - 260f * scale) / 2f

        // Glowing tactical pedestal at feet
        val pedestalY = topY + 242f * scale
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    AmberGold.copy(alpha = 0.42f),
                    DiamondCyan.copy(alpha = 0.14f),
                    Color.Transparent,
                ),
                center = Offset(cx, pedestalY),
                radius = 78f * scale,
            ),
            topLeft = Offset(cx - 78f * scale, pedestalY - 22f * scale),
            size = Size(156f * scale, 44f * scale),
        )
        drawOval(
            color = AmberGold.copy(alpha = 0.7f),
            topLeft = Offset(cx - 56f * scale, pedestalY - 12f * scale),
            size = Size(112f * scale, 24f * scale),
            style = Stroke(width = 2f * scale),
        )

        val bob = breathe * scale

        val pantsPrimary = bottom?.primaryColor ?: Color(0xFF78716C)
        val pantsAccent = bottom?.accentColor ?: Color(0xFF44403C)
        val topPrimary = top?.primaryColor ?: Color(0xFFE2E8F0)
        val topAccent = top?.accentColor ?: Color(0xFF334155)
        val shoePrimary = shoes?.primaryColor ?: Color(0xFF292524)
        val shoeAccent = shoes?.accentColor ?: Color(0xFF57534E)

        // Legs / Pants
        val hipY = topY + 144f * scale + bob * 0.5f
        // Left leg
        drawRoundRect(
            color = pantsPrimary,
            topLeft = Offset(cx - 24f * scale, hipY),
            size = Size(20f * scale, 74f * scale),
            cornerRadius = CornerRadius(6f * scale),
        )
        // Right leg
        drawRoundRect(
            color = pantsPrimary,
            topLeft = Offset(cx + 4f * scale, hipY),
            size = Size(20f * scale, 74f * scale),
            cornerRadius = CornerRadius(6f * scale),
        )
        // Pant stripes / style accent
        if (bottom?.styleTag == "JAZZ" || bottom?.styleTag == "JOGGER" || bottom?.styleTag == "ANGELIC") {
            drawLine(
                color = pantsAccent,
                start = Offset(cx - 21f * scale, hipY + 6f * scale),
                end = Offset(cx - 21f * scale, hipY + 68f * scale),
                strokeWidth = 3f * scale,
            )
            drawLine(
                color = pantsAccent,
                start = Offset(cx + 21f * scale, hipY + 6f * scale),
                end = Offset(cx + 21f * scale, hipY + 68f * scale),
                strokeWidth = 3f * scale,
            )
        }
        // Waist belt
        drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(cx - 25f * scale, hipY - 4f * scale),
            size = Size(50f * scale, 9f * scale),
        )
        drawRect(
            color = AmberGold,
            topLeft = Offset(cx - 5f * scale, hipY - 4f * scale),
            size = Size(10f * scale, 9f * scale),
        )

        // Shoes
        val shoeY = topY + 215f * scale
        drawRoundRect(
            color = shoePrimary,
            topLeft = Offset(cx - 28f * scale, shoeY),
            size = Size(24f * scale, 18f * scale),
            cornerRadius = CornerRadius(5f * scale),
        )
        drawRoundRect(
            color = shoePrimary,
            topLeft = Offset(cx + 4f * scale, shoeY),
            size = Size(24f * scale, 18f * scale),
            cornerRadius = CornerRadius(5f * scale),
        )
        drawRect(
            color = shoeAccent,
            topLeft = Offset(cx - 28f * scale, shoeY + 13f * scale),
            size = Size(24f * scale, 5f * scale),
        )
        drawRect(
            color = shoeAccent,
            topLeft = Offset(cx + 4f * scale, shoeY + 13f * scale),
            size = Size(24f * scale, 5f * scale),
        )

        // Arms (behind/beside torso)
        val shoulderY = topY + 78f * scale + bob
        val skinTone = Color(0xFFE0AC84)
        val sleeveColor = if (top?.styleTag == "TANK") skinTone else topPrimary
        drawRoundRect(
            color = sleeveColor,
            topLeft = Offset(cx - 42f * scale, shoulderY + 4f * scale),
            size = Size(15f * scale, 56f * scale),
            cornerRadius = CornerRadius(7f * scale),
        )
        drawRoundRect(
            color = sleeveColor,
            topLeft = Offset(cx + 27f * scale, shoulderY + 4f * scale),
            size = Size(15f * scale, 56f * scale),
            cornerRadius = CornerRadius(7f * scale),
        )
        // Hands
        drawCircle(
            color = skinTone,
            radius = 7f * scale,
            center = Offset(cx - 34f * scale, shoulderY + 62f * scale),
        )
        drawCircle(
            color = skinTone,
            radius = 7f * scale,
            center = Offset(cx + 34f * scale, shoulderY + 62f * scale),
        )

        // Torso / Jacket
        drawRoundRect(
            color = topPrimary,
            topLeft = Offset(cx - 28f * scale, shoulderY),
            size = Size(56f * scale, 68f * scale),
            cornerRadius = CornerRadius(10f * scale),
        )
        // Jacket collar & center zip / tactical details
        drawLine(
            color = topAccent,
            start = Offset(cx, shoulderY + 4f * scale),
            end = Offset(cx, shoulderY + 64f * scale),
            strokeWidth = 3f * scale,
        )
        if (top?.styleTag == "HEROIC" || top?.styleTag == "HOODIE" || top?.styleTag == "KIMONO" || top?.styleTag == "TRENCH") {
            drawRoundRect(
                color = topAccent,
                topLeft = Offset(cx - 28f * scale, shoulderY),
                size = Size(56f * scale, 14f * scale),
                cornerRadius = CornerRadius(6f * scale),
            )
        }
        if (top?.styleTag == "CRIMINAL") {
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 7f * scale,
                center = Offset(cx + 12f * scale, shoulderY + 24f * scale),
                style = Stroke(width = 2.5f * scale),
            )
        }

        // Equipped Weapon held across chest
        val wPrimary = weaponSkin?.primaryColor ?: Color(0xFF475569)
        val wAccent = weaponSkin?.accentColor ?: Color(0xFF94A3B8)
        rotate(degrees = -22f, pivot = Offset(cx, shoulderY + 38f * scale)) {
            // Rifle body
            drawRoundRect(
                color = wPrimary,
                topLeft = Offset(cx - 46f * scale, shoulderY + 32f * scale),
                size = Size(84f * scale, 11f * scale),
                cornerRadius = CornerRadius(3f * scale),
            )
            // Barrel
            drawRect(
                color = wAccent,
                topLeft = Offset(cx + 36f * scale, shoulderY + 35f * scale),
                size = Size(24f * scale, 4f * scale),
            )
            // Magazine
            drawRect(
                color = wAccent,
                topLeft = Offset(cx + 4f * scale, shoulderY + 42f * scale),
                size = Size(8f * scale, 16f * scale),
            )
        }

        // Neck & Head
        val headCy = topY + 50f * scale + bob
        drawRect(
            color = skinTone,
            topLeft = Offset(cx - 8f * scale, headCy + 16f * scale),
            size = Size(16f * scale, 14f * scale),
        )
        drawCircle(
            color = skinTone,
            radius = 22f * scale,
            center = Offset(cx, headCy),
        )

        // Eyes
        drawCircle(color = Color(0xFF0F172A), radius = 2.6f * scale, center = Offset(cx - 8f * scale, headCy - 2f * scale))
        drawCircle(color = Color(0xFF0F172A), radius = 2.6f * scale, center = Offset(cx + 8f * scale, headCy - 2f * scale))

        // Mask / Glasses
        when (mask?.styleTag) {
            "SKULL" -> {
                drawRoundRect(
                    color = mask.primaryColor,
                    topLeft = Offset(cx - 20f * scale, headCy + 2f * scale),
                    size = Size(40f * scale, 20f * scale),
                    cornerRadius = CornerRadius(8f * scale),
                )
                for (i in -2..2) {
                    drawLine(
                        color = mask.accentColor,
                        start = Offset(cx + i * 5f * scale, headCy + 8f * scale),
                        end = Offset(cx + i * 5f * scale, headCy + 17f * scale),
                        strokeWidth = 2f * scale,
                    )
                }
            }
            "SHADES" -> {
                drawRoundRect(
                    color = mask.primaryColor,
                    topLeft = Offset(cx - 17f * scale, headCy - 7f * scale),
                    size = Size(14f * scale, 9f * scale),
                    cornerRadius = CornerRadius(2f * scale),
                )
                drawRoundRect(
                    color = mask.primaryColor,
                    topLeft = Offset(cx + 3f * scale, headCy - 7f * scale),
                    size = Size(14f * scale, 9f * scale),
                    cornerRadius = CornerRadius(2f * scale),
                )
                drawLine(
                    color = mask.accentColor,
                    start = Offset(cx - 3f * scale, headCy - 4f * scale),
                    end = Offset(cx + 3f * scale, headCy - 4f * scale),
                    strokeWidth = 2f * scale,
                )
            }
            "VISOR" -> {
                drawRoundRect(
                    color = mask.accentColor.copy(alpha = 0.85f),
                    topLeft = Offset(cx - 19f * scale, headCy - 8f * scale),
                    size = Size(38f * scale, 11f * scale),
                    cornerRadius = CornerRadius(4f * scale),
                )
            }
        }

        // Hair / Hat
        val hatColor = hat?.primaryColor ?: Color(0xFFDC2626)
        val hatAccent = hat?.accentColor ?: Color(0xFFFCA5A5)
        when (hat?.styleTag) {
            "CROWN" -> {
                val crown = Path().apply {
                    moveTo(cx - 20f * scale, headCy - 14f * scale)
                    lineTo(cx - 16f * scale, headCy - 34f * scale)
                    lineTo(cx - 6f * scale, headCy - 22f * scale)
                    lineTo(cx, headCy - 38f * scale)
                    lineTo(cx + 6f * scale, headCy - 22f * scale)
                    lineTo(cx + 16f * scale, headCy - 34f * scale)
                    lineTo(cx + 20f * scale, headCy - 14f * scale)
                    close()
                }
                drawPath(crown, color = hatColor)
                drawPath(crown, color = hatAccent, style = Stroke(width = 2f * scale))
            }
            "CAP", "HIPHOP", "SAKURA" -> {
                drawArc(
                    color = hatColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - 23f * scale, headCy - 28f * scale),
                    size = Size(46f * scale, 34f * scale),
                )
                drawRoundRect(
                    color = hatAccent,
                    topLeft = Offset(cx - 24f * scale, headCy - 14f * scale),
                    size = Size(54f * scale, 6f * scale),
                    cornerRadius = CornerRadius(3f * scale),
                )
            }
            "CRIMINAL", "ARCTIC", "BEANIE" -> {
                drawArc(
                    color = hatColor,
                    startAngle = 170f,
                    sweepAngle = 200f,
                    useCenter = true,
                    topLeft = Offset(cx - 24f * scale, headCy - 28f * scale),
                    size = Size(48f * scale, 40f * scale),
                )
                if (hat.styleTag == "CRIMINAL") {
                    // Iconic clown-grin criminal emblem
                    drawRoundRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(cx - 17f * scale, headCy + 2f * scale),
                        size = Size(34f * scale, 16f * scale),
                        cornerRadius = CornerRadius(6f * scale),
                    )
                    drawLine(
                        color = Color(0xFF111827),
                        start = Offset(cx - 10f * scale, headCy + 10f * scale),
                        end = Offset(cx + 10f * scale, headCy + 10f * scale),
                        strokeWidth = 2.5f * scale,
                    )
                }
            }
            else -> {
                // Classic hair + OG Red Bandana
                drawArc(
                    color = Color(0xFF1E1B18),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - 22f * scale, headCy - 24f * scale),
                    size = Size(44f * scale, 26f * scale),
                )
                drawRect(
                    color = hatColor,
                    topLeft = Offset(cx - 22f * scale, headCy - 14f * scale),
                    size = Size(44f * scale, 7f * scale),
                )
                // Bandana trailing ribbon
                drawLine(
                    color = hatColor,
                    start = Offset(cx + 21f * scale, headCy - 10f * scale),
                    end = Offset(cx + 34f * scale, headCy + 2f * scale),
                    strokeWidth = 5f * scale,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
