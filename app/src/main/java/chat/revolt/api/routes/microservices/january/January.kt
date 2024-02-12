package chat.revolt.api.routes.microservices.january

import chat.revolt.api.REVOLT_JANUARY
import java.net.URLEncoder

fun asJanuaryProxyUrl(url: String): String {
    return "$REVOLT_JANUARY/proxy?url=${URLEncoder.encode(url, "utf-8")}"
}
