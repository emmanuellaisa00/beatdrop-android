package com.beatdrop.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale ported from the HTML (Manrope, heavy weights, tight tracking).
 * System default font family stands in for Manrope; drop a Manrope .ttf into
 * res/font and swap FontFamily here to match exactly.
 */
private val AppFont = FontFamily.Default

object BeatType {
    // .large-title { 36px / 900 / -0.042em }
    val LargeTitle = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Black,
        fontSize = 36.sp, letterSpacing = (-0.042).em, lineHeight = 36.sp
    )
    // .section h2 { 22px / 900 / -0.032em }
    val SectionTitle = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Black,
        fontSize = 22.sp, letterSpacing = (-0.032).em
    )
    // .compact .t { 17px / 800 }
    val TopBarTitle = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp, letterSpacing = (-0.016).em
    )
    // .card .nm { 14px / 800 }
    val CardTitle = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp, letterSpacing = (-0.014).em
    )
    // .card .ar { 12px / 500 }
    val CardSub = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, letterSpacing = (-0.005).em
    )
    // .quick .name { 13.5px / 800 }
    val QuickName = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 13.5.sp, letterSpacing = (-0.010).em
    )
    // .identity .greet { 13px / 500 }
    val Greet = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, letterSpacing = (-0.005).em
    )
    // .section .see { 11px / 800 / uppercase / +0.10em }
    val SeeAll = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, letterSpacing = 0.10.em
    )
    // .pill { 13px / 700 }
    val Pill = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, letterSpacing = (-0.005).em
    )
    // .track .t { 15px / 600 }
    val TrackTitle = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, letterSpacing = (-0.014).em
    )
    // .track .a { 12.5px / 500 }
    val TrackSub = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp
    )
    // .tab .lbl { 9px / 800 / uppercase / +0.08em }
    val TabLabel = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 9.sp, letterSpacing = 0.08.em
    )
}

val AppTypography = Typography(
    bodyLarge = BeatType.TrackTitle,
    titleLarge = BeatType.LargeTitle,
)
