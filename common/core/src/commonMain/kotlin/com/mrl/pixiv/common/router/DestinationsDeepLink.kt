package com.mrl.pixiv.common.router

sealed interface PixivLinkTarget {
    val id: Long
    val url: String

    data class Illust(override val id: Long) : PixivLinkTarget {
        override val url: String = "https://www.pixiv.net/artworks/$id"
    }

    data class Novel(override val id: Long) : PixivLinkTarget {
        override val url: String = "https://www.pixiv.net/novel/show.php?id=$id"
    }

    data class User(override val id: Long) : PixivLinkTarget {
        override val url: String = "https://www.pixiv.net/users/$id"
    }

    fun toDestination(): Destination = when (this) {
        is Illust -> Destination.PictureDeeplink(id)
        is Novel -> Destination.NovelDetail(id)
        is User -> Destination.ProfileDetail(id)
    }
}

object DestinationsDeepLink {
    private const val PIXIV_HOST = "https?://(?:www\\.)?pixiv\\.(?:net|me)"
    private const val OPTIONAL_PATH_PREFIX = "(?:/[^/\\s?#]+)*"

    private val illustFinder = Regex(
        pattern = "$PIXIV_HOST$OPTIONAL_PATH_PREFIX/artworks/([1-9]\\d*)",
        option = RegexOption.IGNORE_CASE,
    )
    private val userFinder = Regex(
        pattern = "$PIXIV_HOST$OPTIONAL_PATH_PREFIX/users/([1-9]\\d*)",
        option = RegexOption.IGNORE_CASE,
    )
    private val novelFinder = Regex(
        pattern = "$PIXIV_HOST$OPTIONAL_PATH_PREFIX/novel/show\\.php\\?(?:[^\\s#&]+&)*id=([1-9]\\d*)",
        option = RegexOption.IGNORE_CASE,
    )

    fun findLinks(text: String): List<PixivLinkTarget> {
        if (text.isBlank()) return emptyList()

        return buildList {
            addMatches(text, illustFinder, PixivLinkTarget::Illust)
            addMatches(text, novelFinder, PixivLinkTarget::Novel)
            addMatches(text, userFinder, PixivLinkTarget::User)
        }
            .sortedBy(FoundLink::index)
            .map(FoundLink::target)
            .distinct()
    }

    private fun MutableList<FoundLink>.addMatches(
        text: String,
        regex: Regex,
        createTarget: (Long) -> PixivLinkTarget,
    ) {
        regex.findAll(text).forEach { match ->
            val id = match.groupValues[1].toLongOrNull() ?: return@forEach
            add(FoundLink(index = match.range.first, target = createTarget(id)))
        }
    }

    private data class FoundLink(
        val index: Int,
        val target: PixivLinkTarget,
    )
}
