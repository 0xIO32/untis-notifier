package store

import com.toddway.shelf.FileStorage
import com.toddway.shelf.KotlinxSerializer
import com.toddway.shelf.Shelf
import com.toddway.shelf.get
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import utils.ifTrue
import java.io.File
import java.time.LocalDate

object LessonNotificationStore {
    @OptIn(InternalSerializationApi::class)
    private val shelf = Shelf(FileStorage(File("cache").apply { mkdirs() }), KotlinxSerializer().apply {
        this.register<Set<Int>>(SetSerializer(Int.serializer()))
        this.register<LinkedHashSet<Int>>(SetSerializer(Int.serializer()) as KSerializer<LinkedHashSet<Int>>)
    })

    fun merge(lessonTime: Int, lessonDate: LocalDate, reminded: Set<Int>): Boolean {
        val old = get(lessonTime, lessonDate) ?: listOf()
        old.containsAll(reminded)
            .and(old.isNotEmpty() || reminded.isEmpty())
            .ifTrue { return false }
        val new = old.toMutableSet()
        new.addAll(reminded.toList())
        shelf.item("$lessonDate.$lessonTime").put<Set<Int>>(new)
        return true
    }

    fun get(lessonTime: Int, lessonDate: LocalDate = LocalDate.now()): Set<Int>? = shelf.item("$lessonDate.$lessonTime").get<Set<Int>>()

    fun clear() = shelf.clear()
}