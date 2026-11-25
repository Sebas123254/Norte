package com.sierracode.norte.feature.documents

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
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
import com.sierracode.norte.data.local.entity.*
import com.sierracode.norte.data.local.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repo: DocumentRepository
) : ViewModel() {
    fun createPending(fileUri: String?, onDone: () -> Unit) = viewModelScope.launch {
        repo.upsert(
            Document(
                ownerName = "Yo",
                category = "Otros",
                name = "Documento escaneado",
                documentType = DocumentType.PERSONAL,
                issueDateEpochDay = LocalDate.now().toEpochDay(),
                expiryDateEpochDay = LocalDate.now().plusYears(1).toEpochDay(),
                fileUri = fileUri,
                status = DocumentStatus.PENDIENTE
            )
        )
        onDone()
    }
}

@Composable
fun ScannerDocumentScreen(nav: NavController, vm: ScannerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* se ignora; si deniega, mostramos preview vacío */ }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) permLauncher.launch(Manifest.permission.CAMERA)

        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val capture = ImageCapture.Builder().build()
        imageCapture = capture
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            preview, capture
        )
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Button(
            onClick = {
                vm.createPending(fileUri = null) { nav.popBackStack() }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
        ) { Text("Guardar documento") }
    }
}