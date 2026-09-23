package untis

import config.TimeTableConfig
import kotlinx.datetime.toKotlinLocalTime
import utils.d
import org.bytedream.untis4j.UntisUtils.LessonCode
import org.bytedream.untis4j.responseObjects.Timetable.Lesson
import utils.ifFalse
import utils.w
import java.time.LocalTime

class LessonParser(val config: TimeTableConfig) {

    fun parseChange(lesson: Lesson): List<LessonChange>? {
        d("parsing lesson (${lesson.subjects[0].longName}) at (${lesson.startTime})")
        val name = lesson.subjects[0].longName
        val date = lesson.date ?: run {
            w("invalid lession date (${lesson.date}")
            return null;
        };
        val time = config[lesson.startTime.toKotlinLocalTime()] ?: run {
            w("invalid lesson time (${lesson.startTime})")
            return null
        }

        if (lesson.code == LessonCode.CANCELLED) return listOf(
            LessonChange(
                LessonChangeType.CANCELLED,
                date,
                time,
                name,
                null
            )
        ).also { d("found cancelled lesson ($time, $date, $name)") }

        val changes = mutableListOf<LessonChange>()

        (lesson.originalTeachers.isEmpty()).ifFalse {
            changes += LessonChange(LessonChangeType.TEACHER, date, time, name, lesson.teachers.firstOrNull()?.longName ?: "---")
            d("found changed teacher ($time, $date, $name)")
        }

        (lesson.originalRooms.isEmpty()).ifFalse {
            changes += LessonChange(LessonChangeType.ROOM, date, time, name, lesson.rooms.firstOrNull()?.name ?: "---")
            d("found changed room ($time, $date, $name)")
        }

        return changes.takeIf { it.isNotEmpty() }
    }
}
