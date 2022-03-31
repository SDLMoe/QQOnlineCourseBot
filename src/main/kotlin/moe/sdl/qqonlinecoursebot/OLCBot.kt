package moe.sdl.qqonlinecoursebot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.utils.BotConfiguration
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JOptionPane
import kotlin.concurrent.schedule


suspend fun main() {
    val olcbot = OLCBot()
    olcbot.init()
}

class OLCBot {

    private val log = BotLogger()
    private lateinit var config: OLConfig
    private lateinit var bot: Bot

    private val lastRepeat: AtomicInteger = AtomicInteger(0)
    private val repeatMap: ConcurrentHashMap<Long, CopyOnWriteArrayList<GroupMessageEvent>> = ConcurrentHashMap()

    private var mentionReplySwitch: Boolean = false
    private val lastMention: AtomicInteger = AtomicInteger(0)
    private val lastMentionedMessage: AtomicInteger = AtomicInteger(0)

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    suspend fun init() {
        loadConfig()
        bootQQBot()
        initBotListener()
        bot.join()
    }

    private fun loadConfig() {
        val file = File(getWorkDir(), "config.json")
        if (file.exists()) {
            config = json.decodeFromString(file.readText())
        } else {
            file.parentFile?.mkdirs()
            file.createNewFile()
            config = OLConfig()
            file.writeText(json.encodeToString(config))
        }

        log.notificationSwitch = config.notificationByAppleScript
        mentionReplySwitch = config.mentionReply
        if (config.mentionTexts.isEmpty()) mentionReplySwitch = false

//        Currently, we don't need to save any config, since config would not be changed during running.
//        Runtime.getRuntime().addShutdownHook(Thread {
//            file.writeText(json.encodeToString(config))
//        })

    }

    private suspend fun bootQQBot() {
        val miraiConfig = BotConfiguration.Default.apply {
            fileBasedDeviceInfo()
            protocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE
            heartbeatStrategy = BotConfiguration.HeartbeatStrategy.REGISTER
            cacheDir = getCacheFolder()
        }
        bot = BotFactory.newBot(config.qq, config.password, configuration = miraiConfig)
        bot.login()
    }

    private fun initBotListener() {
        bot.eventChannel.subscribeAlways<GroupMessageEvent> { event ->
            if (config.whiteGroupList.contains(event.group.id)) {
                if (config.autoRepeat && event.time - lastRepeat.get() > config.repeatCDTime.inWholeSeconds) {
                    repeatMap[event.group.id]?.lastOrNull()?.also { lastEvent ->
                        if (event.time - lastEvent.time < config.repeatTimeIdentification.inWholeSeconds &&
                            lastEvent.message.contentToString() == event.message.contentToString() &&
                            (!config.diffPeopleAsTrigger || lastEvent.sender.id != event.sender.id)
                        ) {
                            repeatMap.getOrPut(event.group.id) { CopyOnWriteArrayList() }.add(event)
                        } else {
                            repeatMap[event.group.id] = CopyOnWriteArrayList()
                        }
                    } ?: repeatMap.getOrPut(event.group.id) { CopyOnWriteArrayList() }.add(event)


                    if ((repeatMap[event.group.id]?.size ?: 0) > config.triggerRepeatCount) {
                        val msg = event.message.contentToString()
                        repeatMap[event.group.id] = CopyOnWriteArrayList()
                        lastRepeat.set(event.time)
                        repeat(config.replyRepeatCount) {
                            bot.getGroup(event.group.id)?.sendMessage(msg)
                            log.notice("复读「$msg」，群「${event.group.name}」", "复读检测")
                        }
                    }

                }

                if (mentionReplySwitch && config.mentionQQWhiteList.contains(event.sender.id) &&
                    event.message.filterIsInstance<At>().any { it.target == config.qq } &&
                    event.time - lastMention.get() > config.mentionCDTime.inWholeSeconds
                ) {
                    var lmi = lastMentionedMessage.get()
                    val msg = config.mentionTexts[lmi]
                    lmi++
                    lastMentionedMessage.set(lmi)
                    if (lmi >= config.mentionTexts.size) {
                        lastMentionedMessage.set(0)
                    }
                    bot.getGroup(event.group.id)?.sendMessage(msg)
                    lastMention.set(event.time)
                    log.notice("自动回复「$msg」，群「${event.group.name}」", "有人 At 你")
                }

                if (config.autoReply) {
                    config.rules.map { Regex(it.key) to it.value }.forEach { (reg, placeHolder) ->
                        reg.find(event.message.contentToString())?.also {
                            val msg = it.groups.foldIndexed(placeHolder) { index, acc, i ->
                                acc.replace("$$index", i?.value ?: "")
                            }
                            bot.getGroup(event.group.id)?.sendMessage(msg)
                            log.notice("自动回复「$msg」，群「${event.group.name}」", "匹配到规则")
                        }
                    }
                }

                if (config.tencentMeetingDetect) {
                    val msg = event.message.contentToString()

                    regexForMeetingCode.find(msg)?.value?.also { code ->
                        val meetingCode = code.replace("-", "")
                        val meetingTopic = regexForTopic.find(msg)?.value ?: "无主题"
                        val meetingPassword = regexForPassword.find(msg)?.value
                        log.notice("会议号「$meetingCode」${meetingPassword?.let { "密码「$it」" } ?: ""}", "腾讯会议「${meetingTopic}」")
                        val joinMeeting = {
                            meetingPassword?.let {
                                log.notice("请输入密码「$it」", "已入会", true)
                            }
                            joinTencentMeeting(meetingCode.toLong())
                        }

                        if (config.tencentMeetingAutoJoin && config.tencentMeetingInstantJoin) {
                            joinMeeting()
                            return@also
                        }

                        regexForMeetingStartTime.find(msg)?.value?.also { meetingStartTime ->
                            val meetingStartDateTime = LocalDateTime.parse(meetingStartTime, meetingTimeFormatter)
                            log.info("会议开始时间：$meetingStartTime")
                            val meetingAheadStartDateTime =
                                meetingStartDateTime.minusMinutes(config.tencentMeetingAheadMinutes.toLong())
                            if (LocalDateTime.now() < meetingAheadStartDateTime) {
                                Timer().schedule(
                                    Date.from(
                                        meetingAheadStartDateTime.atZone(ZoneId.systemDefault()).toInstant()
                                    )
                                ) {
                                    log.notice(
                                        "会议将在 ${config.tencentMeetingAheadMinutes} 分钟后开始",
                                        "腾讯会议「${meetingTopic}」"
                                    )
                                    if (config.tencentMeetingAutoJoin) joinMeeting()
                                }
                            } else {
                                log.notice(
                                    "会议已经或即将开始",
                                    "腾讯会议「${meetingTopic}」"
                                )
                                if (config.tencentMeetingAutoJoin) joinMeeting()
                            }

                            regexForMeetingEndTime.find(msg)?.value?.also { endTime ->
                                val meetingEndTime = meetingStartTime.substring(0,meetingStartTime.length - 5) + endTime
                                val meetingEndDateTime = LocalDateTime.parse(meetingEndTime, meetingTimeFormatter)
                                log.info("会议结束时间：$meetingEndTime")
                                val meetingAheadEndDateTime =
                                    meetingEndDateTime.minusMinutes(config.tencentMeetingAheadMinutes.toLong())
                                if (LocalDateTime.now() < meetingAheadEndDateTime) {
                                    Timer().schedule(
                                        Date.from(
                                            meetingAheadEndDateTime.atZone(ZoneId.systemDefault()).toInstant()
                                        )
                                    ) {
                                        log.notice(
                                            "会议预定将在 ${config.tencentMeetingAheadMinutes} 分钟后结束",
                                            "腾讯会议「${meetingTopic}」"
                                        )
                                    }
                                } else {
                                    log.notice(
                                        "会议已经或即将结束",
                                        "腾讯会议「${meetingTopic}」"
                                    )
                                }
                            }
                        } ?: run{
                            if (config.tencentMeetingAutoJoin) {
                                joinMeeting()
                            }
                        }
                    }
                }

            }
        }


    }


}

class BotLogger {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val logger = KotlinLogging.logger {}
    private var noticedUnsupportedSystem: Boolean = false
    var notificationSwitch = true


    fun info(msg: String) {
        logger.info(msg)
    }


    fun notice(msg: String, title: String = "OLCBot", forceJFXPane: Boolean = false) {
        logger.info("Notification >> $title >> $msg")
        if (notificationSwitch) {
            if (forceJFXPane) {
                scope.launch {
                    JOptionPane.showConfirmDialog(
                        null,
                        msg,
                        title,
                        JOptionPane.DEFAULT_OPTION
                    )
                }
            } else {
                if (osType == OsType.MAC) {
                    Runtime.getRuntime().exec(
                        arrayOf(
                            "osascript",
                            "-e",
                            "display notification \"${msg}\" with title \"${title}\" sound name \"Funky\""
                        )
                    )
                } else if (noticedUnsupportedSystem) {
                    repeat(3) {
                        logger.warn(
                            "**** Do not support the notification pushing on ${
                                System.getProperty("os.name").lowercase()
                            } ****"
                        )
                    }
                    noticedUnsupportedSystem = true
                } else {
                    scope.launch {
                        JOptionPane.showConfirmDialog(
                            null,
                            msg,
                            title,
                            JOptionPane.DEFAULT_OPTION
                        )
                    }
                }
            }
        }
    }


}

