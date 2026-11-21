package com.sukhoramen.cs

import android.app.Application
import com.sukhoramen.cs.data.DemoInboxRepository
import com.sukhoramen.cs.data.PreferenceDemoStorage
import com.sukhoramen.cs.data.DeviceVault
import com.sukhoramen.cs.data.LiveApi
import com.sukhoramen.cs.data.LiveInboxRepository
import com.sukhoramen.cs.data.LiveSession

class SukhoApplication : Application() {
    val demoRepository by lazy { DemoInboxRepository(PreferenceDemoStorage(this)) }
    val vault by lazy { DeviceVault(this) }
    val session by lazy { LiveSession() }
    val liveApi by lazy { LiveApi(BuildConfig.API_BASE_URL, session) }
    val liveRepository by lazy { LiveInboxRepository(liveApi, session, this) }
}

