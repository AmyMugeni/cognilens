package com.cognilens.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cognilens.app.data.local.dao.InterventionLogDao
import com.cognilens.app.data.local.entity.InterventionLogEntity

@Database(entities = [InterventionLogEntity::class], version = 1, exportSchema = false)
abstract class CogniLensDatabase : RoomDatabase() {

    abstract fun interventionLogDao(): InterventionLogDao

    companion object {
        @Volatile
        private var INSTANCE: CogniLensDatabase? = null

        fun getDatabase(context: Context): CogniLensDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CogniLensDatabase::class.java,
                    "cognilens_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}