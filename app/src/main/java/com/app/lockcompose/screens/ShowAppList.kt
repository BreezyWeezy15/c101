
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.app.lockcompose.AppLockService
import com.app.lockcompose.R


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShowAppList() {
    val context = LocalContext.current
    val apps = remember { getInstalledApps(context) }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val timeIntervals = arrayOf("1 min", "15 min", "30 min", "45 min", "60 min", "75 min", "90 min", "120 min")
    var selectedInterval by remember { mutableStateOf(timeIntervals[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Time Interval Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .clickable { expanded = true }
        ) {
            Text(text = selectedInterval, color = MaterialTheme.colorScheme.onSurface)

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                timeIntervals.forEach { interval ->
                    DropdownMenuItem(
                        onClick = {
                            selectedInterval = interval
                            expanded = false
                        },
                        text = {
                            Text(interval, color = MaterialTheme.colorScheme.onSurface)
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(apps) { app ->
                AppListItem(
                    app = app,
                    isSelected = app.packageName == selectedPackage,
                    onClick = {
                        selectedPackage = app.packageName
                        startAppLockService(context)
                        saveSelectedPackageName(context, app.packageName)
                        Log.d("TAG PACKAGE", app.packageName)

                        // Convert selected interval to integer and log
                        val intervalInt = selectedInterval.replace(" min", "").toInt()
                        Log.d("TAG INTERVAL", intervalInt.toString())
                    }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppListItem(app: InstalledApp, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
    ) {
        Icon(
            painter = rememberDrawablePainter(drawable = app.icon),
            contentDescription = app.name,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_check_24),
                contentDescription = "Selected",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun getInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val apps = mutableListOf<InstalledApp>()

    val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
    for (pkg in packages) {
        if (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val appName = pkg.applicationInfo.loadLabel(packageManager).toString()
            val appIcon = try {
                pkg.applicationInfo.loadIcon(packageManager)
            } catch (e: Exception) {
                Log.e("AppList", "Error loading icon for $appName", e)
                ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
            }
            val packageName = pkg.packageName

            // Debugging
            Log.d("AppList", "App: $appName, Icon: $appIcon, Package: $packageName")

            appIcon?.let { InstalledApp(appName, it, packageName) }?.let { apps.add(it) }
        }
    }

    return apps
}

data class InstalledApp(val name: String, val icon: Drawable, val packageName: String)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun rememberDrawablePainter(drawable: Drawable): Painter {
    return remember {
        if (drawable is AdaptiveIconDrawable) {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            BitmapPainter(bitmap.asImageBitmap())
        } else {
            DrawablePainter(drawable)
        }
    }
}

class DrawablePainter(private val drawable: Drawable) : Painter() {
    override val intrinsicSize: Size
        get() = Size(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

fun saveSelectedPackageName(context: Context, packageName: String) {
    val sharedPreferences = context.getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putString("selected_package_name", packageName)
        apply()
    }
}

fun startAppLockService(context: Context) {
    val intent = Intent(context, AppLockService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.startService(intent)
    }
}