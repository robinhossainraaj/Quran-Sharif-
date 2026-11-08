package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/** Closes the app and opens it again from the start. */
fun restartApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    var c: Context? = context
    var activity: Activity? = null
    while (c != null) {
        if (c is Activity) { activity = c; break }
        c = (c as? ContextWrapper)?.baseContext
    }
    context.startActivity(launch)
    activity?.finishAffinity()
}
