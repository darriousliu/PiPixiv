package com.mrl.pixiv.ranking

import androidx.annotation.StringRes
import com.mrl.pixiv.common.util.RString

enum class RankingType {
    GENERAL, R18, R18G
}

enum class RankingMode(val value: String, val type: RankingType, @StringRes val title: Int) {
    DAY("day", RankingType.GENERAL, RString.every_day),
    DAY_MALE("day_male", RankingType.GENERAL, RString.popular_male),
    DAY_FEMALE("day_female", RankingType.GENERAL, RString.popular_female),
    WEEK_ORIGINAL("week_original", RankingType.GENERAL, RString.original),
    WEEK_ROOKIE("week_rookie", RankingType.GENERAL, RString.rookie),
    WEEK("week", RankingType.GENERAL, RString.every_week),
    MONTH("month", RankingType.GENERAL, RString.every_month),
    DAY_AI("day_ai", RankingType.GENERAL, (RString.ai_generate)),
    PAST("day", RankingType.GENERAL, (RString.past)),
    DAY_R18("day_r18", RankingType.R18, RString.every_day),
    DAY_MALE_R18("day_male_r18", RankingType.R18, RString.popular_male),
    DAY_FEMALE_R18("day_female_r18", RankingType.R18, RString.popular_female),
    DAY_R18_AI("day_r18_ai", RankingType.R18, RString.ai_generate),
    WEEK_R18("week_r18", RankingType.R18, RString.every_week),
    WEEK_R18G("week_r18g", RankingType.R18G, RString.every_week_r18g);
}
