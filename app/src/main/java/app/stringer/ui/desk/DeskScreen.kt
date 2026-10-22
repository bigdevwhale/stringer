package app.stringer.ui.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stringer.R
import app.stringer.container
import app.stringer.data.Rubric
import app.stringer.data.Weather
import app.stringer.data.inflect
import app.stringer.domain.Case
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.T
import app.stringer.ui.art.Printed
import app.stringer.ui.art.Strip
import app.stringer.ui.art.tape
import app.stringer.ui.art.tapeLines
import app.stringer.ui.components.Chip
import app.stringer.ui.components.PrimaryButton
import app.stringer.ui.components.SCard
import app.stringer.ui.components.SectionTitle
import app.stringer.ui.components.rememberCamera
import app.stringer.ui.ctx
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The correspondent's desk: today's telegram, this week's issue, the investigations and the rest. */
@Composable
fun DeskScreen(home: Home, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val letter = rememberCamera { go.develop(it, "letter") }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.desk), style = SType.Eyebrow, modifier = Modifier.weight(1f))
            IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = S.c.muted) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Masthead(home)
            Spacer(Modifier.height(18.dp))

            val tg = home.telegram
            when {
                tg != null -> InHand(home, go)
                home.flashDue -> Flash(home) {
                    scope.launch {
                        val ok = withContext(Dispatchers.Default) { c.container.newsroom.flash() } != null
                        if (ok) go.telegram()
                    }
                }
                else -> GoingOut(home) { w ->
                    scope.launch {
                        val ok = withContext(Dispatchers.Default) { c.container.newsroom.telegram(w) } != null
                        if (ok) go.telegram()
                    }
                }
            }

            ThisIssue(home, go)

            SectionTitle(stringResource(R.string.newsroom))
            SCard(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 12.dp)) {
                val solved = Case.entries.count { it.done(home.shots, home.ru) }
                val filed = Case.entries.sumOf { home.case(it).size }
                DeskRow(Icons.Rounded.Mail, stringResource(R.string.letter), stringResource(R.string.letter_sub), letter)
                DeskRow(Icons.Rounded.Search, stringResource(R.string.cases), stringResource(R.string.cases_sub, filed, solved), go::cases)
                DeskRow(Icons.Rounded.CollectionsBookmark, stringResource(R.string.archive), T.plural(c, R.plurals.issues, home.issues.size), go::archive)
                DeskRow(Icons.Rounded.Badge, stringResource(R.string.press_card), T.rank(c, home.rank, home.feminine), go::card)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** The paper's name, as on its front page, with a rule above and below. */
@Composable
private fun Masthead(home: Home) {
    val c = ctx()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Rules()
        Spacer(Modifier.height(8.dp))
        Text(T.paper(c, home.settings.paper), style = SType.Mast.copy(fontSize = 30.sp, textAlign = TextAlign.Center), maxLines = 2)
        Spacer(Modifier.height(6.dp))
        Rules()
        Spacer(Modifier.height(6.dp))
        val who = home.settings.name.ifBlank { stringResource(R.string.correspondent) }
        Text("$who · ${T.rank(c, home.rank, home.feminine)} · ${T.dateline(c, home.today)}", style = SType.Small.copy(textAlign = TextAlign.Center), maxLines = 2)
    }
}

@Composable
fun Rules() {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(S.c.print))
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(S.c.print))
    }
}

/** Nothing in hand: report the weather and ask for a telegram. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoingOut(home: Home, onAsk: (Weather?) -> Unit) {
    val c = ctx()
    var weather by rememberSaveable { mutableStateOf("") }
    SCard {
        Text(stringResource(R.string.going_out), style = SType.Title)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.going_out_sub), style = SType.Muted)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.weather_q), style = SType.Small)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(T.weather(c, null), weather.isEmpty()) { weather = "" }
            Weather.entries.forEach { w -> Chip(T.weather(c, w), weather == w.code) { weather = w.code } }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(stringResource(R.string.ask_telegram), { onAsk(Weather.code(weather)) }, Modifier.fillMaxWidth())
        if (home.shots.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.first_hint), style = SType.Muted)
        }
    }
}

/** A telegram in hand: its tape, tap to open. */
@Composable
private fun InHand(home: Home, go: Go) {
    val c = ctx()
    val a = home.telegram ?: return
    Box(
        Modifier
            .fillMaxWidth()
            .clip(S.RSm)
            .background(S.c.form)
            .clickable(onClick = go::telegram)
            .padding(16.dp),
    ) {
        Column {
            Text(stringResource(if (home.settings.telegram.flash) R.string.flash_in_hand else R.string.in_hand).uppercase(), style = SType.Eyebrow.copy(color = S.c.formInk))
            Spacer(Modifier.height(8.dp))
            tapeLines(tape(a.text(home.ru, home.feminine), stringResource(R.string.stop)), 22).forEachIndexed { i, l -> Strip(l, i) }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.in_hand_since, T.time(home.settings.telegram.at)) + " · " + T.rubric(c, a.rubric), style = SType.Small)
        }
    }
}

/** Dusk, and the editor wants the evening light before it goes. */
@Composable
private fun Flash(home: Home, onTake: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(S.RMd)
            .background(S.c.redSoft)
            .border(1.5.dp, S.c.red.copy(alpha = 0.6f), S.RMd)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, tint = S.c.red, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.flash).uppercase(), style = SType.Eyebrow)
        }
        Spacer(Modifier.height(6.dp))
        val left = home.toSunset
        Text(if (left != null && left > 0) T.plural(ctx(), R.plurals.flash_sunset, left) else stringResource(R.string.flash_after), style = SType.Title)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.flash_sub), style = SType.Muted)
        Spacer(Modifier.height(12.dp))
        PrimaryButton(stringResource(R.string.flash_take), onTake, Modifier.fillMaxWidth(), icon = Icons.Rounded.Bolt)
    }
}

/** This week's issue: its number, the rubrics filled, the frames so far. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThisIssue(home: Home, go: Go) {
    val c = ctx()
    val frames = home.inWeek(home.week)
    val have = frames.map { it.rubricOf }.toSet()
    SectionTitle(stringResource(R.string.this_issue, home.issueNumber(home.week)))
    SCard(onClick = { go.issue(home.week) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(T.span(c, home.week), style = SType.Strong)
                Text(
                    if (frames.isEmpty()) stringResource(R.string.issue_empty) else T.plural(c, R.plurals.frames, frames.size) + " · " + stringResource(R.string.rubrics_of, Rubric.assigned.count { it in have }, Rubric.assigned.size),
                    style = SType.Muted,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
        }
        if (frames.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                frames.takeLast(5).reversed().forEachIndexed { i, s ->
                    Printed(s.photo, Modifier.weight(1f).aspectRatio(0.8f).rotate(((i * 7) % 5 - 2) * 0.6f).clip(S.RSm), width = 240, cells = 46)
                }
                repeat((5 - frames.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Rubric.assigned.forEach { r ->
                val on = r in have
                Text(
                    T.rubric(c, r),
                    style = SType.Small.copy(fontSize = 11.5.sp, color = if (on) S.c.onRed else S.c.faint),
                    modifier = Modifier.clip(S.RSm).background(if (on) S.c.print else S.c.surface2).padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
        if (home.streak > 1) {
            Spacer(Modifier.height(10.dp))
            Text(inflect(T.plural(c, R.plurals.streak, home.streak), home.feminine), style = SType.Pencil.copy(fontSize = 21.sp))
        }
    }
}

@Composable
private fun DeskRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(S.RSm).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(S.RSm).background(S.c.surface2), contentAlignment = Alignment.Center) { Icon(icon, null, tint = S.c.print, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = SType.Strong)
            Text(sub, style = SType.Muted, maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
    }
}
