package com.example.forcefullscreen

import android.content.pm.ActivityInfo
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookMain : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android") return

        try {
            val activityRecord = XposedHelpers.findClassIfExists(
                "com.android.server.wm.ActivityRecord",
                lpparam.classLoader
            ) ?: return

            // Ignore manifest portrait locks
            XposedBridge.hookAllMethods(
                activityRecord,
                "getRequestedOrientation",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            )

            // Ignore runtime portrait lock calls
            XposedBridge.hookAllMethods(
                activityRecord,
                "setRequestedOrientation",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty() && param.args[0] is Int) {
                            param.args[0] = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                }
            )

            // Prevent letterbox compat display insets
            XposedBridge.hookAllMethods(
                activityRecord,
                "shouldCreateCompatDisplayInsets",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }
}
