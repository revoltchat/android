package chat.revolt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import chat.revolt.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Inter = FontFamily(
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
    Font(R.font.inter_black, FontWeight.Black),
)

const val ALL_INTER = true

private val OpenSans = if (ALL_INTER) Inter else FontFamily(
    Font(R.font.opensans_light, FontWeight.Light, FontStyle.Normal),
    Font(R.font.opensans_lightitalic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.opensans_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.opensans_regularitalic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.opensans_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.opensans_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.opensans_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.opensans_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.opensans_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.opensans_bolditalic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.opensans_extrabold, FontWeight.ExtraBold, FontStyle.Normal),
    Font(R.font.opensans_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),
)

val RevoltTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),

    titleLarge = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),

    labelLarge = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
)