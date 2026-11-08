package app.stringer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import app.stringer.data.Photos
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The crop frame (#7): what is inside the frame on screen is what is saved. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CropTest {
    private val c = ApplicationProvider.getApplicationContext<android.content.Context>()

    /** 400×300: red on the left half, blue on the right. */
    private fun photo(): Bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until 400) for (y in 0 until 300) setPixel(x, y, if (x < 200) Color.RED else Color.BLUE)
    }

    private fun saved(name: String?) = BitmapFactory.decodeFile(Photos.file(c, name!!).path)

    @Test
    fun centredSquare() {
        val b = saved(Photos.crop(c, photo(), 1f, 1f, 0f, 0f))
        assertEquals(300, b.width)
        assertEquals(300, b.height)
    }

    @Test
    fun pannedRightShowsTheLeftEdge() {
        // The photo is 4/3 of the frame wide; pushed right by its whole slack, the frame sits on its left edge.
        val b = saved(Photos.crop(c, photo(), 1f, 1f, 1f / 6, 0f))
        assertEquals(300, b.width)
        val px = b.getPixel(b.width - 5, 150)
        assert(Color.blue(px) > 200) { "the right of the crop reaches the blue half" }
        assert(Color.red(b.getPixel(3, 150)) > 200)
        // Only 100 px of blue: the crop starts at x = 0.
        assertEquals(true, Color.red(b.getPixel(195, 150)) > 200)
    }

    @Test
    fun zoomedTwiceHalvesTheCrop() {
        val b = saved(Photos.crop(c, photo(), 0.8f, 2f, 0f, 0f))
        // Frame 1 × 1.25; the photo covers its height at zoom 1 (s = 1.25 / 300), twice that here.
        assertEquals(120, b.width, 1)
        assertEquals(150, b.height, 1)
    }

    private fun assertEquals(expected: Int, actual: Int, tolerance: Int) =
        assert(kotlin.math.abs(expected - actual) <= tolerance) { "expected $expected ± $tolerance, got $actual" }
}
