package com.shuo.travel.data

import androidx.room.*
import com.shuo.travel.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "categories", primaryKeys = ["id"])
data class CategoryEntity(@Embedded val content: Category)

@Entity(tableName = "phrases", primaryKeys = ["id"],
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("categoryId"), Index("lastUsedAt")])
data class PhraseEntity(@Embedded val content: Phrase)

@Entity(tableName = "metadata")
data class Metadata(@PrimaryKey val key: String, val value: String)

class Converters {
    @TypeConverter fun encode(value: List<String>): String = Json.encodeToString(value)
    @TypeConverter fun decode(value: String): List<String> = Json.decodeFromString(value)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM categories ORDER BY `order`, name, id") fun observeCategories(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM phrases ORDER BY createdAt, id") fun observePhrases(): Flow<List<PhraseEntity>>
    @Query("SELECT * FROM categories ORDER BY `order`, name, id") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM phrases ORDER BY createdAt, id") suspend fun phrases(): List<PhraseEntity>
    @Query("SELECT * FROM phrases WHERE id = :id") suspend fun phrase(id: String): PhraseEntity?
    @Query("SELECT * FROM metadata WHERE `key` = :key") suspend fun metadata(key: String): Metadata?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCategories(items: List<CategoryEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPhrases(items: List<PhraseEntity>): List<Long>
    @Upsert suspend fun saveCategory(item: CategoryEntity)
    @Upsert suspend fun savePhrase(item: PhraseEntity)
    @Insert suspend fun insertMetadata(item: Metadata)
    @Query("DELETE FROM phrases WHERE id = :id") suspend fun deletePhrase(id: String)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun deleteCategory(id: String)
    @Query("UPDATE phrases SET favorite = NOT favorite, modifiedAt = MAX(:now, createdAt) WHERE id = :id") suspend fun toggleFavorite(id: String, now: Long)
    @Query("UPDATE phrases SET lastUsedAt = :now WHERE id = :id") suspend fun markUsed(id: String, now: Long)
}

@Database(entities = [CategoryEntity::class, PhraseEntity::class, Metadata::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ShuoDatabase : RoomDatabase() { abstract fun libraryDao(): LibraryDao }
