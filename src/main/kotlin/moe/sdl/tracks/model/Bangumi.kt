package moe.sdl.tracks.model

import moe.sdl.tracks.util.color
import moe.sdl.tracks.util.string.secondsToDuration
import moe.sdl.tracks.util.string.toStringOrDefault
import moe.sdl.yabapi.data.bangumi.BangumiDetailed
import moe.sdl.yabapi.data.bangumi.BangumiEpisode
import moe.sdl.yabapi.data.bangumi.BangumiType

fun BangumiDetailed.toAnsi() = """
    @|cyan,bold =================== ${type.toShow()}信息 ===================|@
    @|bold  $title|@
    @|bold  | 💸　会员要求　${if (payment == null) "免费" else payment?.vipPromotion ?: "不明"}|@
    @|bold  | 📆　更新状态　${if (publish?.isFinished == true) "已完结 共" else "已更新"} ${episodes.size} 话|@
    @|bold  | ⌚️　上次更新　${publish?.releaseTime ?: publish?.releaseDate ?: "暂无更新"}|@
    @|bold  | 🌟　评分　　　${rating?.score ?: "暂无"}|@
    """.trimIndent().color

fun BangumiType.toShow(): String = when (this) {
    BangumiType.ANIME -> "番剧"
    BangumiType.MOVIE -> "电影"
    BangumiType.DOCUMENTARY -> "纪录片"
    BangumiType.GUOCHUANG -> "国创"
    BangumiType.SERIES -> "电视剧"
    BangumiType.VARIETY -> "综艺"
    else -> "未知"
}

fun BangumiEpisode.toAnsi(): String {
    val paddedPart = title.toString().padStart(3, '0')
    val duration = this.durationInSecond.toStringOrDefault { it.toInt().secondsToDuration() }
    return """$paddedPart - $longTitle [$duration]"""
}
