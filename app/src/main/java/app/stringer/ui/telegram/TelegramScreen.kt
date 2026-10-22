package app.stringer.ui.telegram

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stringer.R
import app.stringer.container
import app.stringer.data.Newsroom
import app.stringer.data.inflect
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.T
import app.stringer.ui.art.TelegramForm
import app.stringer.ui.art.tape
import app.stringer.ui.art.tapeLines
import app.stringer.ui.components.PrimaryButton
import app.stringer.ui.components.SoftButton
import app.stringer.ui.components.TextLink
import app.stringer.ui.components.TopBar
import app.stringer.ui.components.rememberCamera
import app.stringer.ui.ctx
import app.stringer.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The telegram itself: the task on tape, the fallback, and the way out to the camera. */
@Composable
fun TelegramScreen(home: Home, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val a = home.telegram
    // Closed elsewhere (filed or put away): nothing to show here.
    LaunchedEffect(a == null) { if (a == null) go.desk() }
    if (a == null) return
    val t = home.settings.telegram
    var grumble by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberCamera { go.develop(it, "tg") }
    val stop = stringResource(R.string.stop)
    val fallback = home.catalogue.fallback(a.thing)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(T.rubric(c, a.rubric), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            val tapeText = tape(a.text(home.ru, home.feminine), stop)
            val orText = if (fallback.blank) stringResource(R.string.or_any_frame) else stringResource(R.string.or_fallback, fallback.text(home.ru))
            TelegramForm(
                head = stringResource(R.string.telegram).uppercase(),
                fields = listOf(
                    "#" to (home.shots.size + 1).toString(),
                    stringResource(R.string.tg_from) to stringResource(R.string.tg_from_v, T.paper(c, home.settings.paper)).uppercase(),
                    stringResource(R.string.tg_to) to stringResource(R.string.tg_to_v, home.settings.name.ifBlank { stringResource(R.string.correspondent) }).uppercase(),
                    stringResource(R.string.tg_time) to "${T.date(c, t.at)} ${T.time(t.at)}",
                ),
                lines = tapeLines(tapeText) + tapeLines(tape(orText, stop)),
                sign = stringResource(R.string.tg_sign).uppercase(),
                stamp = if (t.flash) stringResource(R.string.flash) else if (a.level == 2) stringResource(R.string.stamp_daily) else null,
            )
            grumble?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, style = SType.Pencil)
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton(stringResource(R.string.shoot), camera, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
            Spacer(Modifier.height(10.dp))
            Row {
                SoftButton(
                    stringResource(R.string.swap), {
                        scope.launch {
                            val ok = withContext(Dispatchers.Default) { c.container.newsroom.swap() } != null
                            if (ok) grumble = inflect(c.resources.getStringArray(R.array.remark_swap).random(), home.feminine)
                        }
                    },
                    Modifier.weight(1f), icon = Icons.Rounded.SwapHoriz, enabled = t.swaps < Newsroom.SWAPS,
                )
                Spacer(Modifier.width(10.dp))
                SoftButton(stringResource(R.string.put_away), { c.container.write { newsroom.dismiss() }; go.desk() }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.tg_hint), style = SType.Muted)
            Spacer(Modifier.height(4.dp))
            TextLink(stringResource(R.string.back_to_desk), go::desk, Modifier.padding(start = 0.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}
