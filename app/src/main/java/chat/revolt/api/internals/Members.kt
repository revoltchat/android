package chat.revolt.api.internals

import chat.revolt.api.schemas.Member

@RequiresOptIn("Dummy API, does nothing or returns null.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RvxDummyMemberAPI

object Members {
    @RvxDummyMemberAPI
    fun getMember(serverId: String, userId: String): Member? {
        return null
    }
}