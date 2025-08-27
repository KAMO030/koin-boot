package io.github.kamo030.koinboot.component.room

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.util.findAndInstantiateDatabaseImpl
import org.koin.core.scope.Scope
import kotlin.reflect.KClass

actual inline fun <reified T : androidx.room.RoomDatabase> org.koin.core.scope.Scope.createDatabaseBuilder(
    databaseClass: KClass<T>,
    name: String,
    path: String
): androidx.room.RoomDatabase.Builder<T> {
    return Room.databaseBuilder<T>(
        name = "$path/$name.db",
    ) {
        findAndInstantiateDatabaseImpl(databaseClass.java)
    }
}

actual inline fun <reified T : RoomDatabase> Scope.createInMemoryDatabaseBuilder(
    databaseClass: KClass<T>
): RoomDatabase.Builder<T> = Room.inMemoryDatabaseBuilder {
    findAndInstantiateDatabaseImpl(databaseClass.java)
}