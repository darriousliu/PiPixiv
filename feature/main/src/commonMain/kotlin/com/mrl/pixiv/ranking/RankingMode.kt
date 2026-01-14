package com.mrl.pixiv.ranking

import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.ai_generate
import com.mrl.pixiv.strings.every_day
import com.mrl.pixiv.strings.every_month
import com.mrl.pixiv.strings.every_week
import com.mrl.pixiv.strings.every_week_r18g
import com.mrl.pixiv.strings.original
import com.mrl.pixiv.strings.past
import com.mrl.pixiv.strings.popular_female
import com.mrl.pixiv.strings.popular_male
import com.mrl.pixiv.strings.rookie
import org.jetbrains.compose.resources.StringResource

enum class RankingType {
    GENERAL, R18, R18G
}

enum class RankingMode(val value: String, val type: RankingType, val title: StringResource) {
    DAY("day", RankingType.GENERAL, RStrings.every_day),
    DAY_MALE("day_male", RankingType.GENERAL, RStrings.popular_male),
    DAY_FEMALE("day_female", RankingType.GENERAL, RStrings.popular_female),
    WEEK_ORIGINAL("week_original", RankingType.GENERAL, RStrings.original),
    WEEK_ROOKIE("week_rookie", RankingType.GENERAL, RStrings.rookie),
    WEEK("week", RankingType.GENERAL, RStrings.every_week),
    MONTH("month", RankingType.GENERAL, RStrings.every_month),
    DAY_AI("day_ai", RankingType.GENERAL, (RStrings.ai_generate)),
    PAST("day", RankingType.GENERAL, (RStrings.past)),
    DAY_R18("day_r18", RankingType.R18, RStrings.every_day),
    DAY_MALE_R18("day_male_r18", RankingType.R18, RStrings.popular_male),
    DAY_FEMALE_R18("day_female_r18", RankingType.R18, RStrings.popular_female),
    DAY_R18_AI("day_r18_ai", RankingType.R18, RStrings.ai_generate),
    WEEK_R18("week_r18", RankingType.R18, RStrings.every_week),
    WEEK_R18G("week_r18g", RankingType.R18G, RStrings.every_week_r18g);
}
