package app.stringer.ui.archive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stringer.R
import app.stringer.data.Rubric
import app.stringer.domain.Editor
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.T
import app.stringer.ui.art.Printed
import app.stringer.ui.components.SCard
import app.stringer.ui.components.SectionTitle
import app.stringer.ui.components.TopBar
import app.stringer.ui.ctx
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType

/** The bound volume: every issue so far, the newest on top, grouped by year. */
@Composable
fun ArchiveScreen(home: Home, go: Go) {
    val c = ctx()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.archive), go::back)
        if (home.issues.isEmpty()) {
            Text(stringResource(R.string.archive_empty), style = SType.Sub, modifier = Modifier.padding(20.dp))
            return
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 20.dp)) {
            home.issues.groupBy { it / 100 }.forEach { (year, weeks) ->
                item(key = "y$year") { SectionTitle(stringResource(R.string.volume, year.toInt(), weeks.size)) }
                items(weeks, key = { it }) { w ->
                    val frames = home.inWeek(w)
                    val lead = frames.firstOrNull { it.front } ?: frames.first()
                    SCard(Modifier.padding(bottom = 10.dp), onClick = { go.issue(w) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Printed(lead.photo, Modifier.size(64.dp).clip(S.RSm), width = 200, cells = 40)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.issue_n, home.issueNumber(w)) + " · " + T.span(c, w), style = SType.Strong)
                                Text(lead.caption, style = SType.Caption.copy(color = S.c.muted), maxLines = 1)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    T.plural(c, R.plurals.frames, frames.size) + " · " +
                                        stringResource(R.string.rubrics_of, Rubric.assigned.size - Editor.missing(home.shots, w).size, Rubric.assigned.size),
                                    style = SType.Small,
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
