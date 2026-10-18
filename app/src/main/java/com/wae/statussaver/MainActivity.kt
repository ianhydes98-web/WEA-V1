package com.wae.statussaver

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wae.statussaver.ui.WaeRoot
import com.wae.statussaver.ui.WaeTheme
import com.wae.statussaver.util.Perms
import com.wae.statussaver.vm.StatusViewModel

class MainActivity : ComponentActivity() {

    private val vm: StatusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            WaeTheme {
                val state by vm.state.collectAsStateWithLifecycle()

                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { vm.refresh() }

                WaeRoot(
                    state = state,
                    onRequestPermissions = { permissionLauncher.launch(Perms.runtime()) },
                    onRefresh = vm::refresh,
                    onSave = vm::save,
                    onSaveAll = vm::saveAll,
                    onSelectTab = vm::selectTab,
                    onToggleAutoSave = vm::toggleAutoSave,
                    onOpenBatterySettings = {
                        runCatching { startActivity(Perms.batteryIntent(this@MainActivity)) }
                    },
                    onOpenAllFilesSettings = {
                        runCatching { startActivity(Perms.allFilesIntent(this@MainActivity)) }
                    },
                    onResetIndex = vm::resetIndex,
                    onOpenWhatsApp = ::openWhatsApp,
                    onMessageShown = vm::consumeMessage
                )

                LaunchedEffect(Unit) { vm.refresh() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshHealth()
    }

    private fun openWhatsApp() {
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                runCatching { startActivity(intent) }
                return
            }
        }
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.whatsapp")))
        }
    }
}
