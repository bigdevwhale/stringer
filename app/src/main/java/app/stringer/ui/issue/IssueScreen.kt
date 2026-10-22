package app.stringer.ui.issue

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.stringer.R
import app.stringer.data.Rubric
import app.stringer.data.Shot
import app.stringer.data.Weather
import app.stringer.domain.Editor
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.T
import app.stringer.ui.art.Printed
import app.stringer.ui.components.TopBar
import app.stringer.ui.ctx
import app.stringer.ui.theme.Mono
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import app.stringer.ui.theme.Sans
import app.stringer.ui.theme.Serif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** A week's issue, laid out as a newspaper page: the front, the rubrics, small ads in the gaps. */
@Composable
fun IssueScreen(week: Long, home: Home, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val number = home.issueNumber(week)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.issue_n, number), go::back) {
            IconButton(onClick = {
                scope.launch {
                    val bitmap = layer.toImageBitmap().asAndroidBitmap()
                    val f = withContext(Dispatchers.IO) { save(c, bitmap, number) }
                    share(c, f)
                }
            }) { Icon(Icons.Rounded.Share, stringResource(R.string.share), tint = S.c.ink) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 10.dp)) {
            Box(
                Modifier.drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            ) { Page(week, home, go) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The page itself, also what gets shared. */
@Composable
fun Page(week: Long, home: Home, go: Go?) {
    val c = ctx()
    val frames = home.inWeek(week)
    val lead = frames.firstOrNull { it.front } ?: frames.firstOrNull { it.rubricOf != Rubric.READERS } ?: frames.firstOrNull()
    val rest = frames.filter { it.id != lead?.id }
    val sunday = Editor.monday(week).plusDays(6)

    Column(Modifier.fillMaxWidth().background(S.c.newsprint).border(1.dp, S.c.line).padding(horizontal = 14.dp, vertical = 12.dp)) {
        // The ears above the masthead.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.issue_ear_n, home.issueNumber(week)), style = Ear, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.issue_ear_price), style = Ear, textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(6.dp))
        Text(T.paper(c, home.settings.paper), style = SType.Mast.copy(fontSize = 36.sp, lineHeight = 40.sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Thick()
        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(T.dateline(c, sunday), style = Ear, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.issue_weekly), style = Ear, textAlign = TextAlign.End)
        }
        Thin()

        if (lead == null) {
            Spacer(Modifier.height(40.dp))
            Text(stringResource(R.string.issue_blank), style = SType.Caption.copy(fontSize = 16.sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(40.dp))
        } else {
            Spacer(Modifier.height(10.dp))
            Text(lead.caption, style = Headline, modifier = Modifier.fillMaxWidth().then(open(go, lead)))
            Spacer(Modifier.height(8.dp))
            Printed(lead.photo, Modifier.fillMaxWidth().aspectRatio(1.25f).then(open(go, lead)), width = 1000, cells = 120)
            Spacer(Modifier.height(4.dp))
            Byline(home, lead)
            WeatherBox(home, frames)
        }

        // The rubrics, in the paper's order, two frames to a row.
        Rubric.entries.forEach { r ->
            val inR = rest.filter { it.rubricOf == r }
            if (inR.isEmpty()) return@forEach
            RubricHead(T.rubric(c, r))
            inR.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { s -> Item(home, s, go, Modifier.weight(1f)) }
                    if (row.size == 1) Ad(T.ads(c), (week + row[0].id).toInt(), Modifier.weight(1f))
                }
            }
        }

        // Rubrics still empty this week get small ads, as a district paper would.
        val missing = Editor.missing(home.shots, week)
        if (lead != null && missing.size >= 3) {
            RubricHead(stringResource(R.string.r_ads))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Ad(T.ads(c), week.toInt(), Modifier.weight(1f))
                Ad(T.ads(c), week.toInt() + 7, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(12.dp))
        Thin()
        Spacer(Modifier.height(4.dp))
        val who = home.settings.name.ifBlank { stringResource(R.string.correspondent) }
        Text(
            stringResource(R.string.issue_footer, who, T.plural(c, R.plurals.frames, frames.size), T.span(c, week)),
            style = Ear.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun open(go: Go?, s: Shot): Modifier = if (go != null) Modifier.clickable { go.shot(s.id) } else Modifier

@Composable
private fun Item(home: Home, s: Shot, go: Go?, modifier: Modifier) {
    Column(modifier.then(open(go, s))) {
        Printed(s.photo, Modifier.fillMaxWidth().aspectRatio(0.85f), width = 520, cells = 70)
        Spacer(Modifier.height(4.dp))
        Text(s.caption, style = SType.Caption.copy(fontSize = 12.5.sp, lineHeight = 16.sp), maxLines = 4)
        val task = home.task(s)
        if (task.isNotEmpty()) Text(task, style = SType.Column.copy(fontSize = 10.sp, lineHeight = 13.sp, color = S.c.muted), maxLines = 2)
    }
}

@Composable
private fun Byline(home: Home, s: Shot) {
    val c = ctx()
    val who = home.settings.name.ifBlank { stringResource(R.string.correspondent) }
    val task = home.task(s)
    Text(
        listOfNotNull(stringResource(R.string.photo_by, who), task.takeIf { it.isNotEmpty() }, T.rubric(c, s.rubricOf)).joinToString(" · "),
        style = SType.Column.copy(fontSize = 10.5.sp, lineHeight = 14.sp, color = S.c.muted),
    )
}

/** The week's weather as reported by the correspondent on the way out. */
@Composable
private fun WeatherBox(home: Home, frames: List<Shot>) {
    val c = ctx()
    val seen = frames.mapNotNull { Weather.code(it.weather) }.distinct()
    if (seen.isEmpty()) return
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().border(1.dp, S.c.print).padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.weather_box).uppercase(), style = Ear.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.width(8.dp))
        Text(seen.joinToString(", ") { T.weather(c, it).lowercase() }, style = SType.Column)
    }
}

@Composable
private fun RubricHead(text: String) {
    Spacer(Modifier.height(10.dp))
    Thick()
    Text(
        text.uppercase(),
        style = androidx.compose.ui.text.TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.2.em, color = S.c.print, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
    Thin()
    Spacer(Modifier.height(8.dp))
}

/** A small ad in a ruled box, from the paper's stock of them. */
@Composable
private fun Ad(ads: List<String>, seed: Int, modifier: Modifier) {
    if (ads.isEmpty()) return
    val text = ads[Math.floorMod(seed * 7 + 3, ads.size)]
    Box(modifier.border(1.dp, S.c.print).padding(8.dp), contentAlignment = Alignment.Center) {
        Text(text, style = SType.Column.copy(fontSize = 11.5.sp, lineHeight = 15.sp, textAlign = TextAlign.Center))
    }
}

@Composable
private fun Thick() = Box(Modifier.fillMaxWidth().height(2.5.dp).background(S.c.print))

@Composable
private fun Thin() = Box(Modifier.fillMaxWidth().height(1.dp).background(S.c.print))

private val Ear @Composable get() = androidx.compose.ui.text.TextStyle(fontFamily = Mono, fontSize = 10.sp, lineHeight = 13.sp, color = S.c.print)
private val Headline @Composable get() = androidx.compose.ui.text.TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 29.sp, color = S.c.print)

private fun save(c: Context, bitmap: Bitmap, number: Int): File {
    val dir = File(c.cacheDir, "issues").apply { mkdirs() }
    val f = File(dir, "issue-$number.png")
    f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return f
}

private fun share(c: Context, f: File) {
    val uri = FileProvider.getUriForFile(c, "${c.packageName}.files", f)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { c.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Toast.makeText(c, R.string.no_share, Toast.LENGTH_LONG).show() }
}
