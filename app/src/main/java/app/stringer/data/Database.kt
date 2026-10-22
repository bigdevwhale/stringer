package app.stringer.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * One frame sent in to the paper. [assignment] is the catalogue id it answers ("" for a free frame
 * or an investigation). [fallback] means the exact thing wasn't there and the "or…" was shot.
 * [case] and [slot] file it into an investigation instead of the weekly issue ("alphabet", "Ж").
 * [front] puts it on the front page of its week.
 */
@Entity(tableName = "shots")
data class Shot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assignment: String = "",
    val rubric: String = Rubric.READERS.name,
    val at: Long = System.currentTimeMillis(),
    /** A photo in the app's photos folder. */
    val photo: String,
    val caption: String = "",
    val fallback: Boolean = false,
    val case: String = "",
    val slot: String = "",
    val front: Boolean = false,
    /** The weather reported when the telegram came, for the issue's weather box. */
    val weather: String = "",
) {
    val rubricOf: Rubric get() = Rubric.of(rubric)
    val inCase: Boolean get() = case.isNotEmpty()
}

@Dao
interface ShotDao {
    @Query("SELECT * FROM shots ORDER BY at")
    fun observe(): Flow<List<Shot>>

    @Query("SELECT * FROM shots ORDER BY at")
    suspend fun all(): List<Shot>

    @Query("SELECT * FROM shots WHERE id = :id")
    suspend fun get(id: Long): Shot?

    @Insert
    suspend fun insert(s: Shot): Long

    @Update
    suspend fun update(s: Shot)

    @Query("DELETE FROM shots WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE shots SET front = 0 WHERE at >= :from AND at < :to")
    suspend fun clearFront(from: Long, to: Long)
}

@Database(entities = [Shot::class], version = 1, exportSchema = true)
abstract class StringerDatabase : RoomDatabase() {
    abstract fun shots(): ShotDao

    companion object {
        fun create(context: Context): StringerDatabase =
            Room.databaseBuilder(context.applicationContext, StringerDatabase::class.java, "stringer.db").build()
    }
}
