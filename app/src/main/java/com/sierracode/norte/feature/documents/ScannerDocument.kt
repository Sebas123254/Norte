package com.sierracode.norte.feature.documents

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.data.local.entity.Document
import com.sierracode.norte.data.local.entity.DocumentType
import com.sierracode.norte.data.local.repository.DocumentRepository
import com.sierracode.norte.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repo: DocumentRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    fun saveScanned(fileUri: String?, onDone: () -> Unit) = viewModelScope.launch {
        repo.upsert(
            Document(
                ownerName = "Yo",
                category = "Otros",
                name = if (fileUri != null) "Documento escaneado" else "Documento sin imagen",
                documentType = DocumentType.OTRO,
                issueDateEpochDay = LocalDate.now().toEpochDay(),
                expiryDateEpochDay = null,
                reminderLeadDays = settingsStore.settings.first().defaultReminderDays,
                fileUri = fileUri
            )
        )
        onDone()
    }
}

@Composable
fun ScannerDocumentScreen(nav: NavController, vm: ScannerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("La cámara se necesita para escanear.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) { Text("Dar permiso") }
            OutlinedButton(onClick = { vm.saveScanned(null) { nav.popBackStack() } }) { Text("Crear sin foto") }
        }
    } else {
        val imageCapture = remember { ImageCapture.Builder().build() }

        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("Scanner", "Error uniendo cámara", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { vm.saveScanned(null) { nav.popBackStack() } }) {
                    Text("Crear sin foto")
                }
                Button(onClick = {
                    val dir = File(context.filesDir, "attachments").apply { mkdirs() }
                    val file = File(dir, "captura_${System.currentTimeMillis()}.jpg")
                    imageCapture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(file).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                vm.saveScanned(Uri.fromFile(file).toString()) { nav.popBackStack() }
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.e("Scanner", "Error al capturar", exc)
                                Toast.makeText(context, "Error: ${exc.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }) { Text("Capturar foto") }
            }
        }
    }
}