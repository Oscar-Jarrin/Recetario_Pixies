package com.pixies.recetario.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun getDatabaseBuilder(context: Any?): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(name = DB_NAME)
        .setDriver(BundledSQLiteDriver())
