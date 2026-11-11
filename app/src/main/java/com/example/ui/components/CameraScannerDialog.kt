package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.Navy800
import com.example.ui.theme.TextMuted
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScannerDialog(
  onBarcodeScanned: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  var isScanningEnabled by remember { mutableStateOf(true) }

  DisposableEffect(Unit) {
    onDispose {
      cameraExecutor.shutdown()
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = Navy800),
      modifier = Modifier
        .fillMaxWidth()
        .height(480.dp)
        .testTag("camera_scanner_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Scan Kamera (ML Kit)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (hasCameraPermission) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black),
            contentAlignment = Alignment.Center
          ) {
            AndroidView(
              factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                  val cameraProvider = cameraProviderFuture.get()
                  val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                  }

                  val barcodeScanner = BarcodeScanning.getClient()

                  val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analyzer ->
                      analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && isScanningEnabled) {
                          val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                          barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                              for (barcode in barcodes) {
                                val rawValue = barcode.rawValue
                                if (!rawValue.isNullOrBlank() && isScanningEnabled) {
                                  isScanningEnabled = false
                                  onBarcodeScanned(rawValue)
                                  break
                                }
                              }
                            }
                            .addOnCompleteListener {
                              imageProxy.close()
                            }
                        } else {
                          imageProxy.close()
                        }
                      }
                    }

                  try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                      lifecycleOwner,
                      CameraSelector.DEFAULT_BACK_CAMERA,
                      preview,
                      imageAnalyzer
                    )
                  } catch (e: Exception) {
                    Log.e("CameraScanner", "Camera bind failed", e)
                  }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
              },
              modifier = Modifier.fillMaxSize()
            )

            // Viewfinder Targeting Frame
            Box(
              modifier = Modifier
                .size(240.dp, 140.dp)
                .border(2.dp, AmberAccent, RoundedCornerShape(12.dp))
            )

            // Red laser scan indicator line
            Canvas(modifier = Modifier.size(240.dp, 140.dp)) {
              drawLine(
                color = Color.Red,
                start = Offset(10f, size.height / 2),
                end = Offset(size.width - 10f, size.height / 2),
                strokeWidth = 3f
              )
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .background(Navy800),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Izin Kamera Diperlukan", color = Color.White, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(8.dp))
              Text("Berikan izin kamera untuk menggunakan fitur scan langsung.", color = TextMuted, fontSize = 12.sp)
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
              ) {
                Text("Izinkan Kamera")
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Arahkan barcode bagseal ke dalam kotak penargetan.",
          fontSize = 11.sp,
          color = TextMuted,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )
      }
    }
  }
}
