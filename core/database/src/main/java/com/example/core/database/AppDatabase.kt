package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Database(
    entities = [
        EpisodicMemoryEntity::class,
        SemanticMemoryEntity::class,
        ProfileMemoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun episodicDao(): EpisodicDao
    abstract fun semanticDao(): SemanticDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var INIT_ERROR: Exception? = null

        fun getInitError(): Exception? = INIT_ERROR

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }

                val keyManager = DatabaseKeyManager(context.applicationContext)
                val passphrase = keyManager.getDatabasePassphrase()

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
                            "Maia cannot operate without encrypted storage.", e
                    )
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "maia_secure_database.db"
                )
                    .openHelperFactory(factory)
                    // Pre-release: sessionId + provenance columns require schema reset
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
