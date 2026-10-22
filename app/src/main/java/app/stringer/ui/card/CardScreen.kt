package app.stringer.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.stringer.R
import app.stringer.data.Rubric
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.T
import app.stringer.ui.art.Stamp
import app.stringer.ui.art.tilt
import app.stringer.ui.components.SCard
import app.stringer.ui.components.SectionTitle
import app.stringer.ui.components.TopBar
import app.stringer.ui.ctx
import app.stringer.ui.theme.Mono
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import app.stringer.ui.theme.Sans
import app.stringer.ui.theme.Serif

/** The press card: rank, the stamp of every issue so far, and what the correspondent has shot. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardScreen(home: Home, go: Go) {
    val c = ctx()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.press_card), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().shadow(4.dp, S.RMd).background(S.c.card, S.RMd).border(1.dp, S.c.print.copy(alpha = 0.25f), S.RMd).padding(18.dp)) {
                Column {
                    Text(stringResource(R.string.card_head).uppercase(), style = androidx.compose.ui.text.TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.3.em, color = S.c.print))
                    Text(T.paper(c, home.settings.paper), style = SType.Mast.copy(fontSize = 22.sp, lineHeight = 26.sp))
                    Spacer(Modifier.height(14.dp))
                    Line(stringResource(R.string.card_name), home.settings.name.ifBlank { stringResource(R.string.correspondent) })
                    Line(stringResource(R.string.card_rank), T.rank(c, home.rank, home.feminine))
                    Line(stringResource(R.string.card_since), home.shots.minOfOrNull { it.at }?.let { T.date(c, it) } ?: T.date(c, home.now))
                    Line(stringResource(R.string.card_no), "%04d".format(home.shots.size.coerceAtLeast(1) * 7 + 1000))
                }
                Stamp(stringResource(R.string.card_stamp), Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 8.dp).rotate(-16f), size = 84.dp)
            }

            SectionTitle(stringResource(R.string.card_progress))
            SCard {
                Row {
                    Stat(home.shots.size.toString(), stringResource(R.string.stat_frames), Modifier.weight(1f))
                    Stat(home.issues.size.toString(), stringResource(R.string.stat_issues), Modifier.weight(1f))
                    Stat(home.streak.toString(), stringResource(R.string.stat_streak), Modifier.weight(1f))
                }
                home.nextRank?.let { next ->
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.next_rank, T.rank(c, next, home.feminine), next.from - home.shots.size), style = SType.Muted)
                    Spacer(Modifier.height(6.dp))
                    val from = home.rank.from
                    LinearProgressIndicator(
                        progress = { (home.shots.size - from) / (next.from - from).toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(S.RSm),
                        color = S.c.red, trackColor = S.c.surface2, drawStopIndicator = {},
                    )
                }
            }

            if (home.issues.isNotEmpty()) {
                SectionTitle(stringResource(R.string.card_stamps))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    home.issues.sorted().forEach { w ->
                        Box(Modifier.size(46.dp).rotate(tilt(w, 8f)), contentAlignment = Alignment.Center) {
                            Stamp("№${home.issueNumber(w)}", size = 46.dp, color = if (home.issueNumber(w) % 2 == 0) S.c.formInk else S.c.red)
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.card_rubrics))
            SCard {
                val counts = home.published.groupingBy { it.rubricOf }.eachCount()
                val max = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                Rubric.entries.forEach { r ->
                    val n = counts[r] ?: 0
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(T.rubric(c, r), style = SType.Muted, modifier = Modifier.width(132.dp), maxLines = 1)
                        Box(Modifier.weight(1f).height(10.dp)) {
                            Box(Modifier.fillMaxWidth(n / max.toFloat()).height(10.dp).background(S.c.print))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(n.toString(), style = SType.Small, modifier = Modifier.width(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Line(k: String, v: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Bottom) {
        Text(k, style = androidx.compose.ui.text.TextStyle(fontFamily = Mono, fontSize = 12.sp, color = S.c.muted), modifier = Modifier.width(96.dp))
        Text(v, style = androidx.compose.ui.text.TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = S.c.print))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = SType.Display)
        Text(label, style = SType.Small)
    }
}
