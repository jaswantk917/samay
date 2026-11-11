package app.materialclock

import android.app.Application
import app.materialclock.widget.ClockWidgetRenderer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ClockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.Default) { ClockWidgetRenderer.publishPreview(this@ClockApplication) }
    }
}
