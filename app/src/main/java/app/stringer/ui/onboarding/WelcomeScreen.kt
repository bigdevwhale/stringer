package app.stringer.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stringer.R
import app.stringer.container
import app.stringer.ru
import app.stringer.ui.T
import app.stringer.ui.art.TelegramForm
import app.stringer.ui.art.tape
import app.stringer.ui.art.tapeLines
import app.stringer.ui.components.Field
import app.stringer.ui.components.PrimaryButton
import app.stringer.ui.components.SectionTitle
import app.stringer.ui.components.Segmented
import app.stringer.ui.ctx
import app.stringer.ui.desk.Rules
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType

/** The job offer: what the paper wants, the paper's name, the byline, and how hard the assignments come. */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val c = ctx()
    var paper by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var feminine by rememberSaveable { mutableStateOf(false) }
    var daily by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Rules()
        Spacer(Modifier.height(8.dp))
        Text(T.paper(c, paper), style = SType.Mast.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Rules()
        Spacer(Modifier.height(20.dp))
        TelegramForm(
            head = stringResource(R.string.telegram).uppercase(),
            fields = listOf("#" to "1", stringResource(R.string.tg_to) to stringResource(R.string.tg_to_v, name.ifBlank { stringResource(R.string.correspondent) }).uppercase()),
            lines = tapeLines(tape(stringResource(R.string.welcome_tape), stringResource(R.string.stop))),
            sign = stringResource(R.string.tg_sign).uppercase(),
        )
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.welcome_title), style = SType.Display)
        Spacer(Modifier.height(8.dp))
        Step("1", stringResource(R.string.welcome_1))
        Step("2", stringResource(R.string.welcome_2))
        Step("3", stringResource(R.string.welcome_3))
        Step("4", stringResource(R.string.welcome_4))

        SectionTitle(stringResource(R.string.paper_name))
        Field(paper, { paper = it.take(40) }, stringResource(R.string.paper_default))
        SectionTitle(stringResource(R.string.your_name))
        Field(name, { name = it.take(40) }, stringResource(R.string.your_name_hint))
        if (c.ru) {
            SectionTitle(stringResource(R.string.address))
            Segmented(listOf(false, true), feminine, { stringResource(if (it) R.string.address_f else R.string.address_m) }, { feminine = it })
        }
        SectionTitle(stringResource(R.string.paper_kind))
        PaperChoice(daily) { daily = it }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.welcome_go), {
            val p = paper.trim()
            val n = name.trim()
            c.container.write { settings.update { it.copy(onboarded = true, paper = p, name = n, feminine = feminine, daily = daily) } }
            onDone()
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.privacy), style = SType.Small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Step(n: String, text: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Text(n, style = SType.Pencil.copy(fontSize = 24.sp), modifier = Modifier.width(26.dp))
        Text(text, style = SType.Body)
    }
}

/** The district weekly (plain twists) or the big-city daily (trickier ones mixed in). */
@Composable
fun PaperChoice(daily: Boolean, onChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(false, true).forEach { d ->
            val on = d == daily
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(S.RMd)
                    .background(if (on) S.c.redSoft else S.c.surface)
                    .border(if (on) 1.5.dp else 1.dp, if (on) S.c.red.copy(alpha = 0.6f) else S.c.line, S.RMd)
                    .clickable { onChange(d) }
                    .padding(14.dp),
            ) {
                Text(stringResource(if (d) R.string.kind_daily else R.string.kind_weekly), style = SType.Strong.copy(color = if (on) S.c.red else S.c.ink))
                Text(stringResource(if (d) R.string.kind_daily_sub else R.string.kind_weekly_sub), style = SType.Muted)
            }
        }
    }
}
