package com.pixies.recetario.data.local

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(context: Any?): RoomDatabase.Builder<AppDatabase>
