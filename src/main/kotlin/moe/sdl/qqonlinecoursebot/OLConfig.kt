@file:UseSerializers(DurationSerializer::class)

package moe.sdl.qqonlinecoursebot

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class OLConfig(
    val qq: Long = 114514L,
    val password: String = "123456",

    val notificationByAppleScript: Boolean = true,
    val whiteGroupList: List<Long> = listOf(),

    val autoRepeat: Boolean = true,
    val triggerRepeatCount: Int = 5, // How many times will trigger the auto reply?
    val repeatTimeIdentification: Duration = 5.toDuration(DurationUnit.SECONDS),
    val diffPeopleAsTrigger: Boolean = false, // Only repeat when different people are repeating
    val replyRepeatCount: Int = 1, // If bot has been triggered, how many times would you like to repeat also?
    val repeatCDTime: Duration = 120.toDuration(DurationUnit.SECONDS), // repeat reply cd time

    val tencentMeetingDetect: Boolean = true,
    val tencentMeetingAutoJoin: Boolean = true,
    val tencentMeetingInstantJoin: Boolean = false,
    val tencentMeetingAheadMinutes: Int = 5,

    val mentionReply: Boolean = true,
    val mentionQQWhiteList: List<Long> = listOf(),
    val mentionTexts: List<String> = listOf(
            "My internet is poor.",
            "I am restarting my router.",
            "My device has no battery now."
    ),
    val mentionCDTime: Duration = 60.toDuration(DurationUnit.SECONDS),

    val autoReply: Boolean = true,
    val rules: Map<String, String> = mapOf(
        "Please send me a (.{1,4}) to mark you present" to "$1",
        "Please send me (.{1,4}) to mark you present" to "$1"
    )




)

object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DurationSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Duration = Duration.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString(value.toString())
    }
}