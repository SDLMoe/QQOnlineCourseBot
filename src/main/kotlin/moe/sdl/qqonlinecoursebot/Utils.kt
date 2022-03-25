package moe.sdl.qqonlinecoursebot

import java.io.File
import java.net.URLDecoder
import java.time.format.DateTimeFormatter

val regexForMeetingCode = Regex("""((?<=[:：]\s?)\d{3}-\d{3}-\d{3,4}|$\d{9,10}^|(?<=\s)\d{9,10}(?=\s))""" )
val regexForTopic = Regex("""(?<=(主题|Topic)(:\s|：\s?)).*""")
val regexForMeetingStartTime = Regex("""(?<=(时间|Time)(:\s|：\s?)).*(?=-\d)""")
val regexForPassword = Regex("""(?<=(密码|Password)(:\s|：\s?)).*""")
val regexForMeetingEndTime = Regex("""(?<=\d{2}:\d{2}-).*(?=\s\()""")


val meetingTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

fun joinTencentMeeting(code: Long) {
    if (code.toString().length in 9..10) {
        val win = osType == OsType.WINDOWS
        val cmd = (if (win) "start " else "open ") + "wemeet://page/inmeeting?meeting_code=$code"
        Runtime.getRuntime().exec(cmd)
    }
}

fun getJarLocation(): File {
    var path: String = OLCBot::class.java.protectionDomain.codeSource.location.path
    if (osType == OsType.WINDOWS) {
        path = path.substring(1)
    }
    if (path.contains("jar")) {
        path = path.substring(0, path.lastIndexOf("/"))
        return File(URLDecoder.decode(path, Charsets.UTF_8))
    }
    return File(URLDecoder.decode(path.replace("target/classes/", ""), Charsets.UTF_8))
}

fun getWorkDir(): File {
    return File(System.getProperty("user.dir"))
}

fun getCacheFolder(): File {
    val file = File(getWorkDir(), "cache")
    if (!file.exists()) {
        file.mkdirs()
    }
    return file
}

val osType: OsType by lazy {
    System.getProperty("os.name")?.lowercase()?.run {
        when {
            contains("win") -> OsType.WINDOWS
            listOf("nix", "nux", "aix").any { contains(it) } -> OsType.LINUX
            contains("mac") -> OsType.MAC
            contains("sunos") -> OsType.SOLARIS
            else -> OsType.OTHER
        }
    } ?: OsType.OTHER
}

enum class OsType { WINDOWS, LINUX, MAC, SOLARIS, OTHER }