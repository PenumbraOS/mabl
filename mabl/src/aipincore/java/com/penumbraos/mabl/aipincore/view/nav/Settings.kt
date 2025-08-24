import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.penumbraos.mabl.aipincore.view.model.NavViewModel

@Composable
fun Settings(navViewModel: NavViewModel = viewModel<NavViewModel>()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val intent = Intent().apply {
            component = ComponentName(
                "humane.experience.settings",
                "humane.experience.settings.SettingsExperience"
            )
        }
        context.startActivity(intent)
        navViewModel.popView()
    }
}