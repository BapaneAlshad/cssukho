package com.sukhoramen.cs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukhoramen.cs.ui.SukhoApp
import com.sukhoramen.cs.ui.EntryScreen
import com.sukhoramen.cs.ui.SukhoTheme
import com.sukhoramen.cs.data.ThemeMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    private val entryModel by viewModels<EntryViewModel> { EntryViewModel.Factory(application as SukhoApplication) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as SukhoApplication
            val entry by entryModel.state.collectAsStateWithLifecycle()
            when (entry.page) {
                EntryPage.SETUP, EntryPage.LOCKED -> SukhoTheme(ThemeMode.LIGHT) {
                    EntryScreen(entry, entryModel::activate, entryModel::unlock, entryModel::enterDemo, entryModel::forgetPin)
                }
                EntryPage.DEMO, EntryPage.LIVE -> {
                    val repository = if (entry.page == EntryPage.DEMO) app.demoRepository else app.liveRepository
                    val model: MainViewModel = viewModel(key = "inbox_${entry.page.name}", factory = MainViewModel.Factory(repository))
                    SukhoApp(model, onExit = entryModel::showGate)
                }
            }
        }
    }

    override fun onStart() { super.onStart(); entryModel.onForeground() }
    override fun onStop() { entryModel.onBackground(); super.onStop() }
}

