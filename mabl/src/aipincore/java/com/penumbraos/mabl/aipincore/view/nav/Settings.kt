import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.penumbraos.mabl.aipincore.view.model.NavViewModel

@Composable
fun Settings(navViewModel: NavViewModel = viewModel<NavViewModel>()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        navViewModel.popView()

        val intent = Intent().apply {
            component = ComponentName(
                "humane.experience.settings",
                "humane.experience.settings.SettingsExperience"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("Settings", "Failed to start settings", e)
        }
    }
}