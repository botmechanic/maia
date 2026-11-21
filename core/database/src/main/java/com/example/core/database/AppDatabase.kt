package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Database(
    entities = [
        EpisodicMemoryEntity::class,
        SemanticMemoryEntity::class,
        ProfileMemoryEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun episodicDao(): EpisodicDao
    abstract fun semanticDao(): SemanticDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val DB_NAME = "maia_secure_database.db"

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE semantic_memories ADD COLUMN embeddingBlob BLOB")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var INIT_ERROR: Exception? = null

        fun getInitError(): Exception? = INIT_ERROR

        /**
         * Destroys an unreadable/orphaned encrypted vault (for example, after a hardware
         * Keystore reset or unsupported device transfer) and clears its stale wrapped key
         * so Maia can cleanly initialize a brand-new encrypted vault.
         */
        @Synchronized
        fun resetVault(context: Context) {
            val appCtx = context.applicationContext
            try {
                INSTANCE?.close()
            } catch (_: Exception) {
            }
            INSTANCE = null
            INIT_ERROR = null
            appCtx.deleteDatabase(DB_NAME)
            appCtx.getSharedPreferences("maia_secure_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }

                val passphrase = try {
                    val keyManager = DatabaseKeyManager(context.applicationContext)
                    keyManager.getDatabasePassphrase()
                } catch (e: Exception) {
                    INIT_ERROR = e
                    throw e
                }

                val factory: SupportSQLiteOpenHelper.Factory
                try {
                    System.loadLibrary("sqlcipher")
                    factory = try {
                        val modernClass = Class.forName("net.zetetic.database.sqlcipher.SupportOpenHelperFactory")
                        val constructor = modernClass.getConstructor(ByteArray::class.java)
                        constructor.newInstance(passphrase) as SupportSQLiteOpenHelper.Factory
                    } catch (e: ClassNotFoundException) {
                        val legacyClass = Class.forName("net.sqlcipher.database.SupportFactory")
                        val constructor = legacyClass.getConstructor(ByteArray::class.java)
                        constructor.newInstance(passphrase) as SupportSQLiteOpenHelper.Factory
                    }
                    android.util.Log.d("AppDatabase", "SQLCipher loaded successfully.")
                } catch (e: Exception) {
                    INIT_ERROR = e
                    throw IllegalStateException(
                        "SQLCipher encryption is required but failed to initialize. " +
                            "Maia cannot operate without encrypted storage.", e,
                    )
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_5_6)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
