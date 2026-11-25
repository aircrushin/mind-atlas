package com.aircrushin.mindatlas

import android.app.Application
import com.aircrushin.mindatlas.ai.AiSettings
import com.aircrushin.mindatlas.data.Workspace
import com.pico.spatial.ui.foundation.dsl.launch

class MindAtlasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AiSettings.init(this)
        Workspace.init(this)
        launch(::mindAtlasApp)
    }
}
