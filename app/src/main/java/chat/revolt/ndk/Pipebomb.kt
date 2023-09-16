package chat.revolt.ndk

@NativeLibrary(NativeLibrary.LIB_NAME_ACCESS_CONTROL)
object Pipebomb {
    init {
        System.loadLibrary(NativeLibrary.LIB_NAME_ACCESS_CONTROL)
    }

    external fun incrementHardCrashCounter()
    external fun checkHardCrash(): Boolean
    external fun doHardCrash()
}