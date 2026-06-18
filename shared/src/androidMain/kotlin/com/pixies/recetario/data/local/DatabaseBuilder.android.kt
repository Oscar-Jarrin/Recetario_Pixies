package com.pixies.recetario.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDatabaseBuilder(context: Any?): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(
        context = context as Context,
        name = DB_NAME
    )
