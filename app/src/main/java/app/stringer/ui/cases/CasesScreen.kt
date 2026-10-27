package app.stringer.ui.cases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stringer.R
import app.stringer.domain.Case
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.T
import app.stringer.ui.art.Printed
import app.stringer.ui.art.press
import app.stringer.ui.art.Stamp
import app.stringer.ui.components.SCard
import app.stringer.ui.components.TopBar
import app.stringer.ui.components.rememberCamera
import app.stringer.ui.ctx
import app.stringer.ui.theme.Masthead
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import app.stringer.ui.theme.Serif

/** The investigations: long assignments that fill up over weeks of walks. */
@Composable
fun CasesScreen(home: Home, go: Go) {
    val c = ctx()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.cases), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.cases_intro), style = SType.Sub)
            Spacer(Modifier.height(16.dp))
            Case.entries.forEach { k ->
                val slots = k.slots(home.ru)
                val filed = home.case(k)
                val n = slots.count { it in filed }
                SCard(Modifier.padding(bottom = 12.dp), onClick = { go.case(k) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(T.case(c, k), style = SType.Title)
                            Text(T.caseBrief(c, k), style = SType.Muted)
                        }
                        if (n == slots.size) Stamp(stringResource(R.string.solved), Modifier.rotate(-12f), size = 70.dp)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { n / slots.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(S.RSm),
                        color = S.c.print, trackColor = S.c.surface2, drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.case_progress, n, slots.size), style = SType.Small)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** One investigation: its slots as a grid, filled ones printed, empty ones waiting. */
@Composable
fun CaseScreen(k: Case, home: Home, go: Go) {
    val c = ctx()
    var slot by rememberSaveable { mutableStateOf("") }
    val camera = rememberCamera { go.develop(it, "case", k.key, slot) }
    val slots = k.slots(home.ru)
    val filed = home.case(k)
    val done = slots.all { it in filed }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(T.case(c, k), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp)) {
            Text(T.caseBrief(c, k), style = SType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                if (done) stringResource(R.string.case_done) else stringResource(R.string.case_hint),
                style = if (done) SType.Pencil else SType.Muted, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
            val cols = if (k == Case.ALPHABET) 5 else 4
            slots.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { s ->
                        val shot = filed[s]
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(0.82f)
                                .clip(S.RSm)
                                .background(if (shot == null) S.c.surface else S.c.surface2)
                                .border(1.dp, S.c.line, S.RSm)
                                .clickable {
                                    if (shot != null) go.shot(shot.id) else { slot = s; camera() }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (shot != null) {
                                Printed(shot.photo, Modifier.fillMaxSize(), width = 260, cells = 44, press = shot.press)
                                Text(
                                    s, style = SType.Strong.copy(fontFamily = Serif, fontSize = 15.sp, color = S.c.onRed),
                                    modifier = Modifier.align(Alignment.BottomStart).offset(4.dp, (-3).dp).background(S.c.print.copy(alpha = 0.78f)).padding(horizontal = 5.dp),
                                )
                            } else {
                                Text(s, style = SType.Mast.copy(fontFamily = if (k == Case.ALPHABET || k == Case.DIGITS) Masthead else Serif, fontSize = 30.sp, color = S.c.faint, textAlign = TextAlign.Center))
                            }
                        }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
