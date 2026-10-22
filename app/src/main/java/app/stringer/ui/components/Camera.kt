package app.stringer.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import app.stringer.R
import app.stringer.data.Photos
import app.stringer.ui.ctx

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
