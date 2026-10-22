package app.stringer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.stringer.R

/**
 * A district newsroom: newsprint, printer's ink, the editor's red pencil and rubber stamp, and the
 * yellowish telegraph form with its pasted paper strips. Only the daylight look.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** The red pencil and the stamp: the one thing to do. */
    val red: Color,
    val redSoft: Color,
    val onRed: Color,
    /** Newsprint and its ink, for the issue itself. */
    val newsprint: Color,
    val print: Color,
    /** The telegraph form, its printed blue lines, and the strips pasted on it. */
    val form: Color,
    val formInk: Color,
    val strip: Color,
    /** The press card's cardboard. */
    val card: Color,
)

val Newsroom = Palette(
    bg = Color(0xFFECE6D8),
    surface = Color(0xFFF7F3EA),
    surface2 = Color(0xFFE4DCCA),
    line = Color(0xFFD3C9B4),
    ink = Color(0xFF1E1C19),
    muted = Color(0xFF5F584C),
    faint = Color(0xFF968D7D),
    red = Color(0xFFAE2F27),
    redSoft = Color(0xFFF0D8D1),
    onRed = Color(0xFFFFF7F2),
    newsprint = Color(0xFFF1ECDF),
    print = Color(0xFF221F1B),
    form = Color(0xFFEDE2C4),
    formInk = Color(0xFF3B5A86),
    strip = Color(0xFFFBF8EF),
    card = Color(0xFFD9CBA8),
)

val LocalPalette = staticCompositionLocalOf { Newsroom }

/** Ruslan Display: the masthead, cut like an old provincial paper's. */
val Masthead = FontFamily(Font(R.font.ruslan, FontWeight.Normal))

/** PT Serif: headlines, captions, the columns. */
val Serif = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
    Font(R.font.pt_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Manrope: plain and clear, for everything around the paper. */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Caveat: the editor's red pencil in the margins. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

/** PT Mono: the telegraph tape. */
val Mono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

object S {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(20.dp)
    val RLg = RoundedCornerShape(14.dp)
    val RMd = RoundedCornerShape(10.dp)
    val RSm = RoundedCornerShape(6.dp)
}

object SType {
    val Mast: TextStyle @Composable get() = TextStyle(fontFamily = Masthead, fontSize = 34.sp, lineHeight = 38.sp, color = S.c.print)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp, color = S.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp, color = S.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.16.em, color = S.c.red)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = S.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, color = S.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = S.c.faint)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val Pencil: TextStyle @Composable get() = TextStyle(fontFamily = Hand, fontSize = 25.sp, lineHeight = 27.sp, color = S.c.red)
    val Tape: TextStyle @Composable get() = TextStyle(fontFamily = Mono, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.04.em, color = S.c.print)
    val Caption: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 13.sp, lineHeight = 17.sp, color = S.c.print)
    val Column: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontSize = 12.5.sp, lineHeight = 16.sp, color = S.c.print)
}

@Composable
fun StringerTheme(content: @Composable () -> Unit) {
    val p = Newsroom
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.red,
                onPrimary = p.onRed,
                primaryContainer = p.redSoft,
                onPrimaryContainer = p.ink,
                secondary = p.formInk,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = SType.Body,
                bodyMedium = SType.Body.copy(fontSize = 15.sp),
                labelLarge = SType.Button,
            ),
            content = content,
        )
    }
}
