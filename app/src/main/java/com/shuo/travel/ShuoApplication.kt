package com.shuo.travel

import android.app.Application
import androidx.room.Room
import com.shuo.travel.data.*

class ShuoApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, ShuoDatabase::class.java, "shuo.db").addMigrations(ShuoDatabase.MIGRATION_1_2).build() }
    val library by lazy { LibraryRepository(database) }
    val preferences by lazy { PreferencesRepository(this) }
}
