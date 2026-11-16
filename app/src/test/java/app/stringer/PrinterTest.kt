package app.stringer

import app.stringer.ui.issue.Printer
import org.junit.Assert.assertEquals
import org.junit.Test

class PrinterTest {
    @Test fun cutsOnlyBetweenBlocks() {
        // A lead of 1421 px, then blocks of about 830 px, on sheets holding 1800 px.
        val breaks = listOf(1421, 1517, 2346, 3175, 4004, 4825, 5164)
        assertEquals(listOf(0, 1517, 3175, 4825, 5310), Printer.cuts(5310, 1800, 1764, breaks))
    }

    @Test fun aBlockTallerThanASheetIsCutThrough() {
        assertEquals(listOf(0, 1000, 2000, 2500), Printer.cuts(2500, 1000, 1000, listOf(2100)))
    }

    @Test fun aBreakNearTheTopIsNotTaken() {
        // Taking 1100 would leave the second sheet almost empty; it is cut where it falls instead.
        assertEquals(listOf(0, 1000, 2000, 2400), Printer.cuts(2400, 1000, 1000, listOf(1000, 1100)))
    }

    @Test fun aNarrowerPageWinsOnlyIfItSavesASheet() {
        // At full width a sheet holds 1000 px and the page needs three; at 0.9 it holds 1111 px and two do.
        val (width, cuts) = Printer.fit(2200, 1000, 1000f, 1000f, 0, listOf(1100), listOf(1f, 0.9f))
        assertEquals(0.9f, width)
        assertEquals(listOf(0, 1100, 2200), cuts)
        assertEquals(1f, Printer.fit(1800, 1000, 1000f, 1000f, 0, listOf(900), listOf(1f, 0.9f)).first)
    }

    @Test fun forAsManySheetsANarrowerPageThatKeepsBlocksWholeWins() {
        // At full width the 1050 px block is cut through; at 0.9 it fits, still on two sheets.
        val (width, cuts) = Printer.fit(1500, 1000, 1000f, 1000f, 0, listOf(1050), listOf(1f, 0.9f))
        assertEquals(0.9f, width)
        assertEquals(listOf(0, 1050, 1500), cuts)
    }
}
