package app.stringer.ui.write

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stringer.R
import app.stringer.container
import app.stringer.data.Photos
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.art.Printed
import app.stringer.ui.components.Field
import app.stringer.ui.components.PrimaryButton
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
 * The correspondent's own article: a headline, the text, a photo if there is one. [id] is an
 * article to rewrite, 0 for a new one. A photo taken here and not sent to press is thrown away.
 */
@Composable
fun WriteScreen(id: Long, home: Home, go: Go) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val old = home.shot(id)?.takeIf { it.article }
    var headline by rememberSaveable { mutableStateOf(old?.caption.orEmpty()) }
    var body by rememberSaveable { mutableStateOf(old?.body.orEmpty()) }
    var photo by rememberSaveable { mutableStateOf(old?.photo.orEmpty()) }
    var busy by rememberSaveable { mutableStateOf(false) }
    val original = old?.photo.orEmpty()
    // A fresh photo replaces the last one taken here; the article's own photo stays until it's saved.
    val drop = { name: String -> if (name != original) Photos.delete(c, name) }
    val camera = rememberCamera { name -> drop(photo); photo = name }
    val leave = {
        drop(photo)
        go.back()
    }
    BackHandler(onBack = leave)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.write), leave)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
            Field(headline, { headline = it.take(140) }, stringResource(R.string.headline), singleLine = false, style = SType.Title)
            Spacer(Modifier.height(12.dp))
            Field(body, { body = it.take(4000) }, stringResource(R.string.article_text), singleLine = false, minLines = 8, style = SType.Body)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.article_hint), style = SType.Small)
            Spacer(Modifier.height(16.dp))
            if (photo.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().aspectRatio(1.5f).clip(S.RSm)) { Printed(photo, Modifier.fillMaxSize(), width = 1000, cells = 110) }
                Spacer(Modifier.height(10.dp))
                Row {
                    SoftButton(stringResource(R.string.retake), camera, Modifier.weight(1f), icon = Icons.Rounded.PhotoCamera)
                    Spacer(Modifier.width(10.dp))
                    SoftButton(stringResource(R.string.photo_remove), { drop(photo); photo = "" }, Modifier.weight(1f), icon = Icons.Rounded.Delete)
                }
            } else {
                SoftButton(stringResource(R.string.photo_add), camera, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                stringResource(R.string.to_press), {
                    if (busy) return@PrimaryButton
                    busy = true
                    scope.launch {
                        val saved = withContext(Dispatchers.Default) { c.container.newsroom.article(old?.id, headline, body, photo) }
                        if (old != null) go.back() else go.filed(saved)
                    }
                },
                Modifier.fillMaxWidth(), enabled = headline.isNotBlank() && body.isNotBlank() && !busy, icon = Icons.Rounded.Print,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

