package chat.revolt.api.internals

import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.RevoltHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

object WebChallenge {
    suspend fun needsCloudflare(): Boolean {
        RevoltHttp.get(REVOLT_BASE).let {
            val text = it.bodyAsText()
            return text.contains("window._cf_chl_opt") // FIXME Naive, prone to captcha page changing
        }
    }
}