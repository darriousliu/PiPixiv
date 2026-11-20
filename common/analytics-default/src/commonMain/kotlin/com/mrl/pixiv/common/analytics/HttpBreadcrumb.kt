package com.mrl.pixiv.common.analytics

internal fun stripHttpBreadcrumbQueryAndFragment(
    category: String?,
    type: String?,
    data: Map<*, *>?,
    setValue: (key: String, value: String?) -> Unit,
) {
    if (data == null || (category != "http" && type != "http")) return

    val url = data["url"] as? String
    val strippedUrl = url?.substringBefore('?')?.substringBefore('#')
    if (strippedUrl != url) {
        setValue("url", strippedUrl)
    }
    if ("http.query" in data) setValue("http.query", null)
    if ("http.fragment" in data) setValue("http.fragment", null)
}
