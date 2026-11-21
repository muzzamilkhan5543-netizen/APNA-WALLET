package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.AccountStatus
import com.example.data.local.UserEntity
import com.example.security.SecurityUtils
import com.example.ui.components.UserAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalWalletPalette
import com.example.util.QrAndMediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrScannerScreen(
    currentUser: UserEntity,
    walletName: String,
    initialTab: Int = 0,
    onLookupRegisteredUser: suspend (String) -> UserEntity?,
    onQrRecipientResolved: (phone: String) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalWalletPalette.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val scanLock = remember { AtomicBoolean(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    fun handleDecodedQr(rawText: String) {
        val parsed = SecurityUtils.parseQrPayload(rawText)
        if (parsed == null) {
            scanError = "Invalid QR code. Scan an $walletName QR code."
            scanLock.set(false)
            return
        }
        if (parsed.phone == currentUser.phoneNumber) {
            scanError = "You cannot scan your own QR code to pay yourself."
            scanLock.set(false)
            return
        }
        scope.launch {
            val recipient = onLookupRegisteredUser(parsed.phone)
            if (recipient == null) {
                scanError = "User ${parsed.phone} is not registered on $walletName."
                scanLock.set(false)
            } else if (recipient.status != AccountStatus.APPROVED) {
                scanError = "Recipient account is ${recipient.status.name.lowercase()}."
                scanLock.set(false)
            } else {
                onQrRecipientResolved(recipient.phoneNumber)
            }
        }
    }

    val galleryQrPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    QrAndMediaUtils.decodeQrFromUri(context, uri)
                }
                if (decoded != null) {
                    handleDecodedQr(decoded)
                } else {
                    scanError = "No QR code detected in the selected image."
                }
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageWhite)
            .navigationBarsPadding()
    ) {
        // Red Header with Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.headerRed)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.accentYellow
                    )
                }
                Text(
                    text = if (selectedTab == 0) "Scan & Pay" else "My QR Code",
                    color = palette.accentYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0 to "Scan QR", 1 to "My QR Code").forEach { (idx, label) ->
                    val active = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) palette.accentYellow else Color.Transparent)
                            .clickable {
                                selectedTab = idx
                                scanError = null
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active) palette.headerRed else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // Live CameraX QR Scanner
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                if (hasCameraPermission) {
                    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

                    DisposableEffect(Unit) {
                        onDispose {
                            analysisExecutor.shutdown()
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                            if (scanLock.compareAndSet(false, true)) {
                                                val result = QrAndMediaUtils.decodeQrFromImageProxy(imageProxy)
                                                if (result != null) {
                                                    previewView.post { handleDecodedQr(result) }
                                                } else {
                                                    scanLock.set(false)
                                                }
                                            }
                                            imageProxy.close()
                                        }
                                    }

                                runCatching {
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                    cameraRef = camera
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Viewfinder Overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val boxSize = minOf(size.width, size.height) * 0.62f
                        val left = (size.width - boxSize) / 2f
                        val top = (size.height - boxSize) / 2f
                        drawRoundRect(
                            color = Color(0xFFFFD100),
                            topLeft = Offset(left, top),
                            size = Size(boxSize, boxSize),
                            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // Bottom Scanner Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (scanError != null) {
                            Surface(
                                color = palette.headerRed,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = scanError!!,
                                    color = palette.accentYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = palette.headerRed,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable {
                                    val next = !torchEnabled
                                    torchEnabled = next
                                    cameraRef?.cameraControl?.enableTorch(next)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                        contentDescription = "Toggle flashlight",
                                        tint = palette.accentYellow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (torchEnabled) "Flash On" else "Flash",
                                        color = palette.accentYellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Surface(
                                color = palette.accentYellow,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .testTag("scan_from_gallery_button")
                                    .clickable {
                                        galleryQrPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = "Scan QR from Gallery",
                                        tint = palette.headerRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "From Gallery",
                                        color = palette.headerRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Permission Required State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.pageWhite)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = palette.headerRed,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera Permission Needed",
                            color = palette.headerRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Allow camera access to scan live QR codes, or pick a QR code image from your Gallery.",
                            color = palette.mutedText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.headerRed,
                                contentColor = palette.accentYellow
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Allow Camera", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                galleryQrPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accentYellow,
                                contentColor = palette.headerRed
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Scan QR from Gallery", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // My QR Code Tab
            val payload = remember(currentUser.phoneNumber, currentUser.fullName) {
                SecurityUtils.buildQrPayload(currentUser.phoneNumber, currentUser.fullName)
            }
            val qrBitmap: Bitmap = remember(payload) {
                QrAndMediaUtils.generateQrBitmap(payload, sizePx = 640)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = palette.headerRed,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UserAvatar(
                                fullName = currentUser.fullName,
                                imageUri = currentUser.profileImageUri,
                                size = 46.dp,
                                borderColor = palette.accentYellow
                            )
                            Column {
                                Text(
                                    text = currentUser.fullName,
                                    color = palette.accentYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = currentUser.phoneNumber,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(3.dp, palette.accentYellow, RoundedCornerShape(20.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "My Wallet QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = "Ask sender to scan this QR to pay directly into your $walletName account.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
