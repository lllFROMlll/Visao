package com.robotarm.visiontracker

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val crashFile = File(context.filesDir, "crash_log.txt")
            crashFile.writeText(sw.toString())
        } catch (e: Exception) {
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun readLastCrash(context: Context): String? {
            val crashFile = File(context.filesDir, "crash_log.txt")
            return if (crashFile.exists()) {
                val content = crashFile.readText()
                crashFile.delete()
                content
            } else null
        }
    }
}
