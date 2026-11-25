package com.sierracode.norte.feature.documents

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.data.local.entity.*
import com.sierracode.norte.data.local.repository.*
import com.sierracode.norte.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repo: DocumentRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private var _capturedUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val capturedUri: StateFlow<Uri?> = _capturedUri.asStateFlow()
    private var _error: MutableStateFlow<String?> = MutableStateFlow(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun capturePhoto(photographer: ImageCapture.PhotosProcessor<out ImageProxy>): Uri {
        Log.d("Scanner", "Intentando capturar foto")
        
        viewModelScope.launch {
            try {
                // Usar el photographer para capturar la imagen
                photographer.takePicture()?.let { uri ->
                    if (!uri.toString().isNullOrEmpty()) {
                        val processedUri = saveImageToAttachments(uri)
                        
                        // Guardar en repo y actualizar state
                        viewModelScope.launch {
                            repo.upsert(
                                Document(
                                    ownerName = "Yo",
                                    category = "Otros",
                                    name = "Documento escaneado",
                                    documentType = DocumentType.OTRO,
                                    issueDateEpochDay = LocalDate.now().toEpochDay(),
                                    expiryDateEpochDay = LocalDate.now().plusYears(1).toEpochDay(),
                                    reminderLeadDays = settingsStore.settings.first().defaultReminderDays,
                                    fileUri = processedUri.toString()
                                )
                            )
                            
                            _capturedUri.value = uri
                            _error.value = null
                        }
                        
                        Toast.makeText(LocalContext.current, "Foto capturada", Toast.LENGTH_SHORT).show()
                    } else {
                        _error.value = "URI de captura es null o vacío"
                    }
                } ?: run {
                    _error.value = "Captura falló: takePicture() retornó null"
                }
            } catch (e: ImageCaptureException) {
                Log.e("Scanner", "Error al capturar imagen", e)
                _error.value = "Error de cámara: ${e.message}"
                Toast.makeText(LocalContext.current, "Error capturando: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("Scanner", "Error inesperado", e)
                _error.value = "Error: ${e.message}"
                e.printStackTrace()
                Toast.makeText(LocalContext.current, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        return Uri.parse("") // Retornar URI vacío como placeholder
    }

    private fun saveImageToAttachments(uri: Uri): Uri {
        val context = LocalContext.current as Context
        val attachmentsDir = File(context.filesDir, "attachments")
        
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }

        // Generar nombre único basado en timestamp
        val timestamp = System.currentTimeMillis()
        val outputName = "captura_$timestamp.jpg"
        val outputFile = File(attachmentsDir, outputName)

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return uri
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            
            inputStream.close()

            if (bitmap == null) {
                return uri
            }

            // Resize si es necesario
            val resizedBitmap = resizeIfNeeded(bitmap)

            // Comprimir y guardar como JPEG con quality 80%
            outputStreamToFile(outputFile, resizedBitmap) ?: return uri

            val outputUri = Uri.fromFile(outputFile)
            Log.d("Scanner", "Imagen guardada: $outputUri")
            
            return outputUri
            
        } catch (e: Exception) {
            Log.e("Scanner", "Error procesando imagen", e)
            return uri
        }
    }

    private fun resizeIfNeeded(bitmap: Bitmap): Bitmap {
        return if (bitmap.width <= 1280 && bitmap.height <= 853) {
            bitmap
        } else {
            val scale = minOf(
                1280f / bitmap.width.toFloat(),
                853f / bitmap.height.toFloat()
            )
            
            if (scale >= 1f) return bitmap
            
            val width = (bitmap.width * scale).toInt()
            val height = (bitmap.height * scale).toInt()

            Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
    }

    private fun outputStreamToFile(file: File, bitmap: Bitmap): OutputStream? {
        return try {
            val stream = file.outputStream()
            
            var compressedBitmap = bitmap
            if (compressedBitmap.isMutable) {
                // Ya es mutable, usar directamente
            } else {
                compressedBitmap = Bitmap.createScaledBitmap(compressedBitmap, 1280, 853, false)
            }

            val success = compressedBitmap.compress(
                android.graphics.Bitmap.CompressFormat.JPEG,
                80, stream
            )
            
            stream.close()
            return if (success) stream else null
            
        } catch (e: Exception) {
            Log.e("Scanner", "Error escribiendo archivo", e)
            null
        }
    }

    fun createPending(fileUri: Uri?, onDone: () -> Unit) = viewModelScope.launch {
        repo.upsert(
            Document(
                ownerName = "Yo",
                category = "Otros",
                name = fileUri?.let { "Documento sin imagen" } ?: "Documento escaneado",
                documentType = DocumentType.OTRO,
                issueDateEpochDay = LocalDate.now().toEpochDay(),
                expiryDateEpochDay = LocalDate.now().plusYears(1).toEpochDay(),
                reminderLeadDays = settingsStore.settings.first().defaultReminderDays,
                fileUri = fileUri?.toString()
            )
        )
        _capturedUri.value = null
        onDone()
    }

    fun clearCapture() {
        _capturedUri.value = null
        _error.value = null
    }
}

@Composable
fun ScannerDocumentScreen(
    nav: NavController, 
    vm: ScannerViewModel = hiltViewModel(),
    activity: Activity? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Estado para controlar la cámara
    var cameraBound by remember { mutableStateOf(false) }
    
    // Launcher para pedir permisos de cámara
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context, 
                "La cámara se necesita para escanear. El documento se creará sin imagen.", 
                Toast.LENGTH_LONG
            ).show()
            
            vm.createPending(null) { nav.popBackStack() }
        } else {
            initCamera(context as Context, lifecycleOwner as LifecycleOwner)
        }
    }

    // Preview View para CameraX
    val previewView = remember { PreviewView(context) }
    
    // Inicializar cámara
    fun initCamera(_context: Context, _lifecycleOwner: LifecycleOwner) {
        LaunchedEffect(Unit) {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(_context)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Configurar preview
                        Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        },
                        
                        // Configurar ImageCapture para guardar en FILES_URI
                        ImageCapture.Builder()
                            .setImageFormat(ImageCapture.ImageFormatNames.JPEG)
                            .setCompressionQuality(80)
                            .build())

                    try {
                        cameraProvider.unbindAll()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        // Guardar referencia al photographer para usar en capturePhoto
                        val imageCapture = ImageCapture.Builder()
                            .setImageFormat(ImageCapture.ImageFormatNames.JPEG)
                            .setCompressionQuality(80)
                            .build()

                        cameraProvider.bindToLifecycle(
                            _lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview, imageCapture
                        )
                        
                        cameraBound = true
                        
                    } catch (e: Exception) {
                        Toast.makeText(_context, "Error uniendo cámara: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                    
                }, ContextCompat.mainLooper(_context))

            } catch (e: Exception) {
                Log.e("Scanner", "Error inicializando cámara", e)
                Toast.makeText(_context, "Error inicializando cámara: ${e.message}", Toast.LENGTH_LONG).show()
                vm.createPending(null) { nav.popBackStack() }
            }
        }
    }

    // Verificar estado de captura y navegar
    val capturedUri by vm.capturedUri.collectAsState()
    val errorText by vm.error.collectAsState()
    
    Box(Modifier.fillMaxSize()) {
        when {
            !cameraBound -> {
                // Estado inicial - solicitar permisos o mostrar loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Configurando cámara...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

                        Button(
                            onClick = { 
                                permLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.align(Alignment.Center).padding(bottom = 8.dp)
                        ) {
                            Text("Iniciar Cámara")
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                vm.createPending(null) { nav.popBackStack() }
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Text("Crear sin foto")
                        }
                    }
                }
            }
            
            capturedUri != null -> {
                // Si ya capturó, mostrar botón continuar
                Button(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                ) {
                    Text("Continuar", color = androidx.compose.ui.graphics.Color.White)
                }
            }
            
            cameraBound -> {
                // Preview View para CameraX
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                
                if (capturedUri == null && errorText == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Toque para capturar foto", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

                            Button(
                                onClick = { 
                                    // Necesitamos el photographer de ImageCapture
                                    // Esto requiere una refactorización
                                    vm.capturePhotoPlaceholder()
                                },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                            ) {
                                Text("Capturar Foto")
                            }
                            
                            OutlinedButton(
                                onClick = { 
                                    vm.createPending(null) { nav.popBackStack() }
                                },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                            ) {
                                Text("Crear sin foto")
                            }
                        }
                    }
                }
            }
        }

        // Mostrar error si existe
        LaunchedEffect(errorText) {
            if (!errorText.isNullOrEmpty()) {
                Toast.makeText(context, errorText, Toast.LENGTH_LONG).show()
                vm.clearCapture()
            }
        }
    }
}

// Función placeholder para capturar foto (debe implementarse con photographer real)
fun ScannerViewModel.capturePhotoPlaceholder(): Uri {
    // Esto es temporal hasta que podamos acceder al photographer real
    return Uri.parse("")
}
