package io.github.kamo030.koinboot.component.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.scope.Scope
import kotlin.reflect.KClass

actual inline fun <reified T : androidx.room.RoomDatabase> org.koin.core.scope.Scope.createDatabaseBuilder(
    databaseClass: KClass<T>,
    name: String,
    path: String
): androidx.room.RoomDatabase.Builder<T> {
    return Room.databaseBuilder(
        context = get(),
        klass = databaseClass.java,
        name = if (path.isEmpty()) get<Context>().getDatabasePath("${name}.db").absolutePath else "$path/$name.db"
    )
}

actual inline fun <reified T : RoomDatabase> Scope.createInMemoryDatabaseBuilder(
    databaseClass: KClass<T>
): RoomDatabase.Builder<T> = Room.inMemoryDatabaseBuilder(
    context = get(),
    klass = T::class.java,
)