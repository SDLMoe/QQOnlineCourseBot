# QQOnlineCourseBot

 基于 Mirai 的简单网课机器人

由于 Mirai Console 过于毒瘤所以直接使用 [mirai-core](https://github.com/mamoe/mirai)
 
## 功能

- 自动复读
- 自动规则匹配（Regex）与回复
- At 回复
- 白名单机制
- 自动进入腾讯会议
  - 支持定时会议的提前 n 分钟进入，和结束时提醒 n 分钟提醒
- 基于 AppleScript / JFXPanel 的提醒功能


## 使用

在 Release 页面直接下载已打包版本，请使用 Java 17 运行本项目

```bash
java -jar QQOnlineCourseBot-1.0.0.jar
```

运行后请先不要进行登录操作，因为首次运行会生成默认配置，请修改配置后再重启项目。

## 配置

- 示例

```json
{
    "qq": 1141514, // QQ 帐号
    "password": "123123", // QQ 密码
    "notificationByAppleScript": true, // 是否启用提醒
    "whiteGroupList": [ // 监听的群号
        114514,
        1145141,
        11451419,
        114514191,
        1145141919
    ],
    "autoRepeat": true, // 启用自动复读
    "triggerRepeatCount": 5, // 需要 n 次群友复读触发自动复读
    "repeatTimeIdentification": "5s", // 每一个消息在多少时间内视为一次复读
    "diffPeopleAsTrigger": false, // 每次复读必须是不同的人
    "replyRepeatCount": 1, // 复读次数
    "repeatCDTime": "2m", // 复读冷却时间
    "tencentMeetingDetect": true, // 检测腾讯会议
    "tencentMeetingAutoJoin": true, // 自动加入腾讯会议
    "tencentMeetingInstantJoin": false, // 是否启用瞬时加入（不启用会议规划功能）
    "tencentMeetingAheadMinutes": 5, // 会议规划提前多少分钟进入会议 / 结束提醒
    "mentionReply": true, // At 回复
    "mentionQQWhiteList": [ // 只有在里面的人 At 才回复
      114514,
      1145141,
      11451419,
      114514191,
      1145141919
    ],
    "mentionTexts": [ // 回复内容，按顺序循环发送
        "My internet is poor.",
        "I am restarting my router.",
        "My device has no battery now."
    ],
    "mentionCDTime": "2m", // At 回复冷却时间
    "autoReply": true, // 自动匹配回复（正则表达式）
    "rules": {
        "Please send me a (.{1,4}) to mark you present": "$1",
        "Please send me (.{1,4}) to mark you present": "$1"
    }
}
```