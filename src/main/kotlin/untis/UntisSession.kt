package untis

import config.Untis
import kotlinx.io.IOException
import org.bytedream.untis4j.LoginException
import org.bytedream.untis4j.Session
import utils.w
import java.time.LocalDate

inline fun closingUntisSession(config: Untis, block: (session: Session) -> Unit) =
    try {
        Session.login(config.username, config.password, config.server, config.school ?: "").apply(block).logout()
    } catch(e: LoginException) {
        w("failed to login to untis")
        e.printStackTrace()
    } catch(e: IOException) {
        w("unknown exception occurred")
        e.printStackTrace()
    }


fun Session.todaysTimetable(daysInAdvance: Int) = getTimetableFromPersonId(LocalDate.now(), LocalDate.now().plusDays(
    daysInAdvance.toLong()
), infos.personId)