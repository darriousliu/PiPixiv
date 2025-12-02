package com.mrl.pixiv.common.data.ranking

enum class RankingType {
    GENERAL, R18, R18G
}

enum class RankingMode(val value: String, val type: RankingType, val title: String) {
    DAY("day", RankingType.GENERAL, "每日"),
    DAY_MALE("day_male", RankingType.GENERAL, "男性热门"),
    DAY_FEMALE("day_female", RankingType.GENERAL, "女性热门"),
    WEEK_ORIGINAL("week_original", RankingType.GENERAL, "原创"),
    WEEK_ROOKIE("week_rookie", RankingType.GENERAL, "新人"),
    WEEK("week", RankingType.GENERAL, "每周"),
    MONTH("month", RankingType.GENERAL, "每月"),
    DAY_AI("day_ai", RankingType.GENERAL, "AI生成"),
    PAST("day", RankingType.GENERAL, "过去"),
    DAY_R18("day_r18", RankingType.R18, "每日"),
    DAY_MALE_R18("day_male_r18", RankingType.R18, "男性热门"),
    DAY_FEMALE_R18("day_female_r18", RankingType.R18, "女性热门"),
    DAY_R18_AI("day_r18_ai", RankingType.R18, "AI生成"),
    WEEK_R18("week_r18", RankingType.R18, "每周"),
    WEEK_R18G("week_r18g", RankingType.R18G, "每周(R18G)");
}
