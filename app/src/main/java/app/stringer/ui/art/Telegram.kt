package app.stringer.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.stringer.ui.theme.Mono
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import app.stringer.ui.theme.Sans
import kotlin.math.abs

/** Words wrapped into telegraph-tape lengths. */
fun tapeLines(text: String, max: Int = 24): List<String> {
    val out = mutableListOf<String>()
    var line = ""
    for (w in text.split(' ').filter { it.isNotEmpty() }) {
        line = when {
            line.isEmpty() -> w
            line.length + 1 + w.length <= max -> "$line $w"
            else -> { out += line; w }
        }
    }
    if (line.isNotEmpty()) out += line
    return out
}

/** The task as the tape prints it: capitals, no punctuation but the "stop". */
fun tape(text: String, stop: String): String =
    text.uppercase().trim().trimEnd('.', '!', '?')
        .replace(Regex("[.!?]\\s+"), " $stop ")
        .replace(Regex("[,:;«»\"—–]"), " ").replace(Regex("\\s+"), " ").trim() + " " + stop

/**
 * A telegraph form with strips of tape pasted on it. [head] is the printed form's title,
 * [fields] the small printed lines (from, to, time), [lines] the pasted tape, [sign] the
 * signature strip, and [stamp] an optional red stamp across the corner.
 */
@Composable
fun TelegramForm(
    head: String,
    fields: List<Pair<String, String>>,
    lines: List<String>,
    sign: String,
    modifier: Modifier = Modifier,
    stamp: String? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(3.dp, S.RSm)
                .background(S.c.form, S.RSm)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(head, style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.32.em, color = S.c.formInk), modifier = Modifier.weight(1f))
                Text("№ ${fields.firstOrNull { it.first == "#" }?.second.orEmpty()}", style = TextStyle(fontFamily = Mono, fontSize = 12.sp, color = S.c.formInk))
            }
            Spacer(Modifier.height(6.dp))
            FormRule()
            fields.filter { it.first != "#" }.forEach { (k, v) ->
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text(k, style = TextStyle(fontFamily = Sans, fontSize = 11.sp, color = S.c.formInk), modifier = Modifier.width(64.dp))
                    Text(v, style = TextStyle(fontFamily = Mono, fontSize = 13.sp, color = S.c.print), maxLines = 1)
                }
                FormRule(dashed = true)
            }
            Spacer(Modifier.height(14.dp))
            lines.forEachIndexed { i, l -> Strip(l, i) }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { Strip(sign, lines.size, small = true) }
            footer?.let {
                Spacer(Modifier.height(10.dp))
                it()
            }
        }
        if (stamp != null) {
            Stamp(stamp, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = 30.dp).rotate(-14f))
        }
    }
}

@Composable
private fun FormRule(dashed: Boolean = false) {
    val ink = S.c.formInk.copy(alpha = if (dashed) 0.35f else 0.7f)
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        drawLine(ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = if (dashed) 1f else 2f, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null)
    }
}

/** One strip of tape, pasted a touch crooked, each one its own way. */
@Composable
fun Strip(text: String, index: Int, small: Boolean = false) {
    val tilt = ((index * 37 + text.length * 11) % 9 - 4) * 0.22f
    val shift = (((index * 53 + text.length * 7) % 7) - 3).dp
    Box(
        Modifier
            .padding(vertical = 3.dp)
            .offset(x = shift)
            .rotate(tilt)
            .shadow(1.dp)
            .background(S.c.strip)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = if (small) SType.Tape.copy(fontSize = 13.sp) else SType.Tape, maxLines = 1)
    }
}

/** A rubber stamp in red ink, a double ring with a word across it. */
@Composable
fun Stamp(text: String, modifier: Modifier = Modifier, color: Color = S.c.red, size: Dp = 92.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val r = this.size.minDimension / 2
            drawCircle(color.copy(alpha = 0.82f), r - 2f, style = Stroke(3.2f))
            drawCircle(color.copy(alpha = 0.82f), r - 9f, style = Stroke(1.6f))
        }
        Text(
            text.uppercase(),
            style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = if (text.length > 8) 10.sp else 13.sp, letterSpacing = 0.08.em, color = color.copy(alpha = 0.85f), textAlign = TextAlign.Center),
            modifier = Modifier.background(S.c.form.copy(alpha = 0.0f)).border(1.6.dp, color.copy(alpha = 0.8f)).padding(horizontal = 4.dp, vertical = 1.dp),
            maxLines = 2,
        )
    }
}

/** A flat rectangular stamp, like «В НАБОР» on a proof. */
@Composable
fun BoxStamp(text: String, modifier: Modifier = Modifier, color: Color = S.c.red) {
    Text(
        text.uppercase(),
        style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.14.em, color = color.copy(alpha = 0.85f)),
        modifier = modifier.border(2.dp, color.copy(alpha = 0.8f), S.RSm).padding(horizontal = 10.dp, vertical = 4.dp),
        maxLines = 1,
    )
}

/** Keeps a seeded tilt in a sane range for anything pinned to the desk. */
fun tilt(seed: Long, max: Float = 2.5f): Float = ((abs(seed * 2654435761L) % 1000) / 1000f - 0.5f) * 2 * max
