package chat.revolt.api.routes.microservices.january

import chat.revolt.api.REVOLT_JANUARY

fun asJanuaryProxyUrl(url: String): String {
    return "$REVOLT_JANUARY/proxy?url=$url"
}
