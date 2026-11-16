package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ConversionMode
import com.example.model.FootnotePlacement
import com.example.model.K2Settings
import com.example.model.ProtectPolicy
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val settingsJson: String,
    val isBuiltIn: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toShareableJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("k2pdfoptVersion", "2.55")
            put("settings", JSONObject(settingsJson))
        }.toString(2)
    }
}

@Entity(tableName = "page_overrides", primaryKeys = ["documentPath", "pageNumber"])
data class PageOverrideEntity(
    val documentPath: String,
    val pageNumber: Int,
    val blocksJson: String,
    val ocrLangOverride: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reader_bookmarks")
data class ReaderBookmarkEntity(
    @PrimaryKey val documentPath: String,
    val title: String,
    val currentPage: Int,
    val totalPages: Int,
    val nightMode: Boolean,
    val detectedLangTag: String,
    val extractedCleanText: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "batch_reports")
data class BatchReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val pagesProcessed: Int,
    val avgOcrConfidence: Int,
    val lowQualityPageCount: Int,
    val elapsedMs: Long,
    val outputFormat: String,
    val outputPath: String,
    val outputSizeBytes: Long,
    val warningsSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface K2Dao {
    @Query("SELECT * FROM profiles ORDER BY isBuiltIn DESC, updatedAt DESC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfilesImmediate(): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomProfile(id: String)

    @Query("SELECT * FROM page_overrides WHERE documentPath = :docPath ORDER BY pageNumber ASC")
    suspend fun getOverridesForDocument(docPath: String): List<PageOverrideEntity>

    @Query("SELECT * FROM page_overrides WHERE documentPath = :docPath AND pageNumber = :page")
    suspend fun getPageOverride(docPath: String, page: Int): PageOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPageOverride(overrideEntity: PageOverrideEntity)

    @Query("DELETE FROM page_overrides WHERE documentPath = :docPath AND pageNumber = :page")
    suspend fun deletePageOverride(docPath: String, page: Int)

    @Query("SELECT * FROM reader_bookmarks ORDER BY updatedAt DESC")
    fun observeBookmarks(): Flow<List<ReaderBookmarkEntity>>

    @Query("SELECT * FROM reader_bookmarks WHERE documentPath = :path")
    suspend fun getBookmark(path: String): ReaderBookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: ReaderBookmarkEntity)

    @Query("SELECT * FROM batch_reports ORDER BY timestamp DESC")
    fun observeBatchReports(): Flow<List<BatchReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchReport(report: BatchReportEntity)
}

@Database(
    entities = [
        ProfileEntity::class,
        PageOverrideEntity::class,
        ReaderBookmarkEntity::class,
        BatchReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class K2Database : RoomDatabase() {
    abstract fun dao(): K2Dao

    companion object {
        @Volatile
        private var INSTANCE: K2Database? = null

        fun getInstance(context: Context): K2Database {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    K2Database::class.java,
                    "k2pdfopt_android.db"
                ).build().also { INSTANCE = it }
            }
        }

        fun builtInProfiles(): List<ProfileEntity> {
            val defaultReflow = K2Settings()
            val scientificMedical = K2Settings(
                mode = ConversionMode.TWO_COLUMN,
                maxColumns = 2,
                tableProtection = ProtectPolicy.SCALE_TO_FIT,
                equationInlineImage = true,
                footnotePlacement = FootnotePlacement.END_OF_PARAGRAPH,
                useNeuralLayoutDetector = true,
                ocrEnabled = true,
                ocrLanguages = "eng",
                sharpen = true
            )
            val scanCleanup = K2Settings(
                mode = ConversionMode.DEFAULT_REFLOW,
                autoStraightenDeskew = true,
                despeckle = true,
                whitenBackground = true,
                removeShadows = true,
                contrast = 1.45f,
                gamma = 0.42f,
                sharpen = true,
                ocrEnabled = true
            )
            val rtlMultilingual = K2Settings(
                mode = ConversionMode.DEFAULT_REFLOW,
                rtlMode = true,
                ocrEnabled = true,
                ocrLanguages = "ara+heb+fas+urd+eng",
                justification = 2
            )
            val cjkVertical = K2Settings(
                mode = ConversionMode.DEFAULT_REFLOW,
                verticalCjk = true,
                rtlMode = true,
                ocrEnabled = true,
                ocrLanguages = "chi_sim_vert+jpn_vert+kor",
                wordSpacing = -0.05f
            )
            val einkContrast = K2Settings(
                mode = ConversionMode.DEFAULT_REFLOW,
                bitsPerPixel = 2,
                dither = true,
                contrast = 1.60f,
                gamma = 0.38f,
                negateColors = 0
            )
            val slidesFitWidth = K2Settings(
                mode = ConversionMode.FIT_WIDTH,
                maxColumns = 1,
                wrapText = false,
                rotateDeg = 90
            )
            return listOf(
                ProfileEntity(
                    id = "preset_default_reflow",
                    name = "Standard Book Reflow",
                    description = "Single-column book text reflow (-mode default -wrap+ -hy+) with OCR layer.",
                    settingsJson = defaultReflow.toJson().toString(),
                    isBuiltIn = true
                ),
                ProfileEntity(
                    id = "preset_scientific_medical",
                    name = "Scientific & Medical Journal (2-Column)",
                    description = "Tuned for clinical trial papers, IEEE/Nature/NEJM articles: 2-column unwrap, protected tables & inline equations, smart footnotes.",
                    settingsJson = scientificMedical.toJson().toString(),
                    isBuiltIn = true
                ),
                ProfileEntity(
                    id = "preset_scan_cleanup",
                    name = "Archival Scan Cleanup + OCR",
                    description = "Applies Leptonica deskew (-as+), morphological despeckle, background whitening, and Sauvola shadow removal before reflow.",
                    settingsJson = scanCleanup.toJson().toString(),
                    isBuiltIn = true
                ),
                ProfileEntity(
                    id = "preset_rtl_multilingual",
                    name = "RTL Bidirectional (Arabic / Persian / Urdu / Hebrew)",
                    description = "Right-to-left column & word order (-r2l+) with HarfBuzz ligature/diacritic preservation and mixed LTR English support.",
                    settingsJson = rtlMultilingual.toJson().toString(),
                    isBuiltIn = true
                ),
                ProfileEntity(
                    id = "preset_cjk_vertical",
                    name = "CJK Vertical & Horizontal (Chinese / Japanese / Korean)",
                    description = "Supports right-to-left vertical column reading order and CJK character grid segmentation without Latin word gaps.",
                    settingsJson = cjkVertical.toJson().toString(),
                    isBuiltIn = true
                ),
                ProfileEntity(
                    id = "preset_eink_contrast",
                    name = "E-Ink Panel Optimizer (2-bit Dithered)",
                    description = "High-contrast gamma (-g 0.38 -c 1.60 -bpc 2 -d+) tuned for Kindle, Kobo, and Nook e-ink screens.",
                    settingsJson = einkContrast.toJson().toString(),
                    isBuiltIn = true
                ),
                ProfileEntity(
                    id = "preset_slides_fitwidth",
                    name = "Presentation Slides (-mode fw)",
                    description = "Landscape fit-width mode preserving slide diagrams and bullet geometry.",
                    settingsJson = slidesFitWidth.toJson().toString(),
                    isBuiltIn = true
                )
            )
        }
    }
}
