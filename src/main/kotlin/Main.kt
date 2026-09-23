import config.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import notifications.impl.NtfyNotificationProvider
import notifications.impl.PushoverNotificationProvider
import notifications.impl.DiscordNotificationProvider
import store.LessonNotificationStore
import untis.LessonParser
import untis.closingUntisSession
import untis.todaysTimetable
import utils.d
import utils.daysString
import utils.e
import utils.i
import java.time.LocalDate
import java.util.stream.Collectors
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

val ktor by lazy { HttpClient(CIO) }

var debug by Delegates.notNull<Boolean>()
    private set

val json = Json {
    classDiscriminator = "type"
    allowTrailingComma = true
    isLenient = true
}

suspend fun main() = coroutineScope {
    val config = loadConfig() ?: e("cannot read config")
    debug = config.debug
    val notificationProvider = when (config.notifications) {
        is PushoverNotificationConfig -> {
            i("initializing Pushover notification provider")
            PushoverNotificationProvider(config.notifications)
        }
        is NtfyNotificationConfig -> {
            i("initializing Ntfy notification provider")
            NtfyNotificationProvider(config.notifications)
        }
        is DiscordNotificationConfig -> {
            i("initializing Discord notification provider")
            DiscordNotificationProvider(config.notifications)
        }
    }

    val lessonParser = LessonParser(config.timetable)

    launch {
        while(isActive) {
            d("Clearing Lesson store")
            LessonNotificationStore.clear()
            delay(7.days) // I hope this handles errors well enough to ever achieve 7 days uptime
        }
    }

    launch {
        while (isActive) {
            closingUntisSession(config.untis) { session ->
                val timeTable = session.todaysTimetable(config.untis.daysInAdvance).apply { sortByStartTime() }
                for (lesson in timeTable) {
                    val daysInAdvance = (lesson.date.toEpochDay() - LocalDate.now().toEpochDay()).toInt()
                    (lessonParser.parseChange(lesson) ?: continue)
                        .forEach {
                            val new = config.reminder.stream()
                                .filter { reminder -> reminder >= daysInAdvance }
                                .collect(Collectors.toSet())
                            if (LessonNotificationStore.merge(it.lessonTime, it.lessonDate, new)) {
                                notificationProvider.sendChanges(it)
                            } else {
                                d("non-normal lesson (${it.lessonTime}${it.lessonDate.daysString()}, ${it.lessonName}) has already been noticed")
                            }
                        }
                }
            }
            delay(config.untis.refreshDelaySeconds.seconds)
        }
    }

    Unit
}