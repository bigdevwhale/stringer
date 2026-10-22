package app.stringer.ui.develop

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stringer.R
import app.stringer.container
import app.stringer.data.Photos
import app.stringer.domain.Case
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.T
import app.stringer.ui.art.Original
import app.stringer.ui.art.Printed
import app.stringer.ui.components.Chip
import app.stringer.ui.components.Field
import app.stringer.ui.components.PrimaryButton
import app.stringer.ui.components.Segmented
import app.stringer.ui.components.SoftButton
import app.stringer.ui.components.TopBar
import app.stringer.ui.components.rememberCamera
import app.stringer.ui.ctx
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A frame back from the camera, on the proofing table: how it will print, a caption under it, and
 * the editor's choice — to the issue, shoot again, or the bin. [mode] is "tg" for the telegram,
 * "letter" for a letter to the editor, "case" for an investigation's [slot].
 */
@Composable
fun DevelopScreen(photo: String, mode: String, case: String, slot: String, home: Home, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    var current by rememberSaveable { mutableStateOf(photo) }
    var caption by rememberSaveable { mutableStateOf("") }
    var fallback by rememberSaveable { mutableStateOf(false) }
    var printed by rememberSaveable { mutableStateOf(true) }
    var busy by rememberSaveable { mutableStateOf(false) }
    val retake = rememberCamera { name -> Photos.delete(c, current); current = name }
    val a = home.telegram.takeIf { mode == "tg" }
    val k = Case.of(case).takeIf { mode == "case" }
    val task = when {
        a != null -> a.text(home.ru, home.feminine)
        k != null -> "${T.case(c, k)} · $slot"
        else -> stringResource(R.string.letter)
    }
    val leave = {
        Photos.delete(c, current)
        go.back()
    }
    BackHandler(onBack = leave)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.proof), leave)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
            Text(task, style = SType.Title)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(S.RSm).clickable { printed = !printed }) {
                if (printed) Printed(current, Modifier.fillMaxSize(), width = 1000, cells = 120) else Original(current, Modifier.fillMaxSize().background(S.c.print), crop = false)
            }
            Spacer(Modifier.height(10.dp))
            Segmented(listOf(true, false), printed, { stringResource(if (it) R.string.as_printed else R.string.as_shot) }, { printed = it })
            Spacer(Modifier.height(16.dp))
            if (a != null) {
                val fb = home.catalogue.fallback(a.thing)
                Text(stringResource(R.string.found_q), style = SType.Small)
                Spacer(Modifier.height(6.dp))
                Segmented(
                    listOf(false, true), fallback,
                    { stringResource(if (!it) R.string.found_exact else R.string.found_or) }, { fallback = it },
                )
                if (fallback && !fb.blank) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.or_fallback_plain, fb.text(home.ru)), style = SType.Muted)
                }
                Spacer(Modifier.height(16.dp))
            }
            Field(caption, { caption = it.take(140) }, stringResource(R.string.caption), singleLine = false, minLines = 2, style = SType.Caption.copy(fontSize = 17.sp))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.caption_hint), style = SType.Small, modifier = Modifier.weight(1f))
                if (a != null || k != null) Chip(stringResource(R.string.caption_task), false) { caption = task }
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                stringResource(R.string.to_press), {
                    if (busy) return@PrimaryButton
                    busy = true
                    scope.launch {
                        val n = c.container.newsroom
                        val id = withContext(Dispatchers.Default) {
                            when (mode) {
                                "tg" -> n.file(current, caption, fallback)
                                "case" -> n.fileCase(case, slot, current, caption)
                                else -> n.letter(current, caption)
                            }
                        }
                        go.filed(id)
                    }
                },
                Modifier.fillMaxWidth(), enabled = caption.isNotBlank() && !busy, icon = Icons.Rounded.Print,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                SoftButton(stringResource(R.string.retake), retake, Modifier.weight(1f), icon = Icons.Rounded.PhotoCamera)
                Spacer(Modifier.width(10.dp))
                SoftButton(stringResource(R.string.to_bin), leave, Modifier.weight(1f), icon = Icons.Rounded.Delete)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
