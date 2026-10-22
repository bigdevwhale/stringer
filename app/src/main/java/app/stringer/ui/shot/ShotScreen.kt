package app.stringer.ui.shot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stringer.R
import app.stringer.container
import app.stringer.domain.Case
import app.stringer.domain.Editor
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.T
import app.stringer.ui.art.BoxStamp
import app.stringer.ui.art.Original
import app.stringer.ui.art.Printed
import app.stringer.ui.components.Field
import app.stringer.ui.components.PrimaryButton
import app.stringer.ui.components.Segmented
import app.stringer.ui.components.SoftButton
import app.stringer.ui.components.TextLink
import app.stringer.ui.components.TopBar
import app.stringer.ui.ctx
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType

/** One filed frame on the proof: printed, captioned, with the editor's mark in the margin. */
@Composable
fun ShotScreen(id: Long, home: Home, go: Go) {
    val c = ctx()
    val s = home.shot(id)
    if (s == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) { TopBar("", go::back) }
        return
    }
    var editing by rememberSaveable { mutableStateOf(false) }
    var caption by rememberSaveable(id) { mutableStateOf(s.caption) }
    var binning by remember { mutableStateOf(false) }
    var printed by rememberSaveable { mutableStateOf(true) }
    val k = Case.of(s.case)
    val count = home.shots.count { it.at <= s.at }
    val remark = remember(s.id) { T.remark(c, s, 0, count, home.feminine) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(if (k != null) T.case(c, k) else T.rubric(c, s.rubricOf), go::back) {
            IconButton(onClick = { editing = !editing }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit_caption), tint = S.c.ink) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            // Tap the frame, or the switch under it, to see the photo as it was shot.
            Box(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(S.RSm).clickable { printed = !printed }) {
                if (printed) {
                    Printed(s.photo, Modifier.fillMaxSize(), width = 1000, cells = 120)
                    BoxStamp(stringResource(if (s.front) R.string.stamp_front else R.string.stamp_set), Modifier.align(Alignment.BottomEnd).offset(x = (-10).dp, y = (-12).dp).rotate(-6f), color = S.c.red)
                } else {
                    Original(s.photo, Modifier.fillMaxSize().background(S.c.print), crop = false)
                }
            }
            Spacer(Modifier.height(10.dp))
            Segmented(listOf(true, false), printed, { stringResource(if (it) R.string.as_printed else R.string.as_shot) }, { printed = it })
            Spacer(Modifier.height(12.dp))
            if (editing) {
                Field(caption, { caption = it.take(140) }, stringResource(R.string.caption), singleLine = false, minLines = 2, style = SType.Caption)
                Spacer(Modifier.height(8.dp))
                PrimaryButton(stringResource(R.string.save), {
                    val text = caption
                    c.container.write { newsroom.caption(id, text) }
                    editing = false
                }, Modifier.fillMaxWidth(), enabled = caption.isNotBlank())
            } else {
                Text(s.caption, style = SType.Caption.copy(fontSize = 19.sp, lineHeight = 25.sp))
            }
            Spacer(Modifier.height(8.dp))
            val task = home.task(s)
            if (task.isNotEmpty()) Text(stringResource(if (s.fallback) R.string.task_or else R.string.task_line, task), style = SType.Muted)
            if (k != null) Text("${T.case(c, k)} · ${s.slot}", style = SType.Muted)
            Text("${T.date(c, s.at)}, ${T.time(s.at)}", style = SType.Small)
            Spacer(Modifier.height(14.dp))
            Text(remark, style = SType.Pencil, modifier = Modifier.rotate(-1.5f))
            Spacer(Modifier.height(20.dp))
            if (!s.inCase) {
                val week = Editor.week(s.at)
                SoftButton(stringResource(R.string.open_issue, home.issueNumber(week)), { go.issue(week) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Newspaper)
                if (!s.front) {
                    Spacer(Modifier.height(10.dp))
                    SoftButton(stringResource(R.string.to_front), { c.container.write { newsroom.front(id) } }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Star)
                }
            } else if (k != null) {
                SoftButton(stringResource(R.string.open_case), { go.case(k) }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(6.dp))
            Row {
                TextLink(stringResource(R.string.back_to_desk), go::desk)
                Spacer(Modifier.weight(1f))
                TextLink(stringResource(R.string.to_bin), { binning = true }, color = S.c.red)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (binning) {
        AlertDialog(
            onDismissRequest = { binning = false },
            containerColor = S.c.surface,
            title = { Text(stringResource(R.string.bin_title), style = SType.Title) },
            text = { Text(stringResource(R.string.bin_text), style = SType.Body) },
            confirmButton = {
                TextButton({
                    binning = false
                    c.container.write { newsroom.bin(id) }
                    go.back()
                }) { Text(stringResource(R.string.to_bin), color = S.c.red) }
            },
            dismissButton = { TextButton({ binning = false }) { Text(stringResource(R.string.cancel), color = S.c.muted) } },
        )
    }
}
