package app.stringer.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.stringer.R
import app.stringer.data.Photos
import app.stringer.ui.ctx
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The phone's own camera, for one frame. Returns a function that opens it; [onShot] gets the file
 * name in the photos folder once a frame was actually taken. A cancelled shot leaves nothing behind.
 */
@Composable
fun rememberCamera(onShot: (String) -> Unit): () -> Unit {
    val c = ctx()
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val name = pending
        pending = null
        if (ok && name != null && Photos.file(c, name).length() > 0) onShot(name) else Photos.delete(c, name)
    }
    return {
        val f = Photos.fresh(c)
        pending = f.name
        val uri = FileProvider.getUriForFile(c, "${c.packageName}.files", f)
        runCatching { camera.launch(uri) }.onFailure {
            pending = null
            Toast.makeText(c, R.string.no_camera, Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * A photo already on the phone, through the system's photo picker: no storage permission, and the
 * app sees only the one photo chosen. [onPicked] gets its copy's name in the photos folder.
 */
@Composable
fun rememberGallery(onPicked: (String) -> Unit): () -> Unit {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val name = withContext(Dispatchers.IO) { Photos.import(c, uri) }
            if (name != null) picked(name) else Toast.makeText(c, R.string.no_photo, Toast.LENGTH_LONG).show()
        }
    }
    return {
        runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            .onFailure { Toast.makeText(c, R.string.no_gallery, Toast.LENGTH_LONG).show() }
    }
}

/** Where the next frame comes from: straight from the [camera], from the [gallery], or [ask] which. */
class PhotoSource(val camera: () -> Unit, val gallery: () -> Unit, val ask: () -> Unit)

/** The camera and the gallery behind one button: [PhotoSource.ask] puts the choice to the correspondent. */
@Composable
fun rememberPhoto(onPhoto: (String) -> Unit): PhotoSource {
    var asking by rememberSaveable { mutableStateOf(false) }
    val camera = rememberCamera(onPhoto)
    val gallery = rememberGallery(onPhoto)
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            containerColor = S.c.surface,
            title = { Text(stringResource(R.string.photo_from), style = SType.Title) },
            text = {
                Column {
                    SoftButton(stringResource(R.string.photo_from_camera), { asking = false; camera() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
                    Spacer(Modifier.height(10.dp))
                    SoftButton(stringResource(R.string.photo_from_gallery), { asking = false; gallery() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoLibrary)
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ asking = false }) { Text(stringResource(R.string.cancel), color = S.c.muted) } },
        )
    }
    return PhotoSource(camera, gallery) { asking = true }
}
