package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.camera.core.ImageProxy
import com.example.data.local.TransactionEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object QrAndMediaUtils {

    private val qrReader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    fun generateQrBitmap(content: String, sizePx: Int = 640, darkColor: Int = Color.BLACK, lightColor: Int = Color.WHITE): Bitmap {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (bitMatrix[x, y]) darkColor else lightColor
            }
        }
        bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        return bitmap
    }

    fun decodeQrFromImageProxy(imageProxy: ImageProxy): String? {
        return try {
            val plane = imageProxy.planes.firstOrNull() ?: return null
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val width = imageProxy.width
            val height = imageProxy.height
            val rowStride = plane.rowStride

            val yData = if (rowStride == width) {
                data
            } else {
                ByteArray(width * height).also { out ->
                    for (row in 0 until height) {
                        val srcPos = row * rowStride
                        val copyLen = minOf(width, data.size - srcPos)
                        if (copyLen > 0) {
                            System.arraycopy(data, srcPos, out, row * width, copyLen)
                        }
                    }
                }
            }

            val source = PlanarYUVLuminanceSource(
                yData,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            qrReader.decodeWithState(bitmap).text
        } catch (_: Exception) {
            null
        } finally {
            qrReader.reset()
        }
    }

    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return null
                val width = bmp.width
                val height = bmp.height
                val pixels = IntArray(width * height)
                bmp.getPixels(pixels, 0, width, 0, 0, width, height)
                val source = RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                qrReader.decodeWithState(binaryBitmap).text
            }
        } catch (_: Exception) {
            null
        } finally {
            qrReader.reset()
        }
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri, prefix: String): String? {
        return try {
            val mime = context.contentResolver.getType(uri)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            val dir = File(context.filesDir, "wallet_media").apply { mkdirs() }
            val dest = File(dir, "${prefix}_${UUID.randomUUID().toString().take(8)}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(dest).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun saveReceiptToGallery(
        context: Context,
        tx: TransactionEntity,
        walletName: String,
        primaryColor: Int,
        accentColor: Int
    ): Result<Uri> {
        return runCatching {
            val width = 1080
            val height = 1520
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Pure White Canvas
            canvas.drawColor(Color.WHITE)

            // Top Red Header
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
            canvas.drawRect(0f, 0f, width.toFloat(), 360f, headerPaint)

            // Wallet Name in Yellow
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                textSize = 54f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(walletName, width / 2f, 115f, titlePaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Transaction Receipt", width / 2f, 180f, subPaint)

            // Success Badge Circle overlapping header
            val circleOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val circleInner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#16A34A") }
            canvas.drawCircle(width / 2f, 360f, 88f, circleOuter)
            canvas.drawCircle(width / 2f, 360f, 74f, circleInner)

            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                strokeWidth = 14f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawLine(width / 2f - 30f, 360f, width / 2f - 8f, 386f, tickPaint)
            canvas.drawLine(width / 2f - 8f, 386f, width / 2f + 36f, 336f, tickPaint)

            val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                textSize = 76f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Rs. ${"%,.2f".format(tx.amountPkr)}", width / 2f, 540f, amountPaint)

            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#16A34A")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Transaction Successful", width / 2f, 600f, statusPaint)

            // Details Card Box
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFF8F8")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1D4D8")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val rect = RectF(72f, 660f, width - 72f, 1340f)
            canvas.drawRoundRect(rect, 36f, 36f, cardPaint)
            canvas.drawRoundRect(rect, 36f, 36f, borderPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6B7280")
                textSize = 32f
                textAlign = Paint.Align.LEFT
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111827")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = 2f
            }

            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date(tx.timestamp))

            val rows = listOf(
                "Transaction ID" to tx.transactionRef,
                "Date & Time" to dateStr,
                "Method" to tx.methodTitle,
                "From Name" to tx.senderName,
                "From Number" to tx.senderPhone,
                "To Name" to tx.receiverName,
                "To Number" to tx.receiverPhone,
                "Fee" to "Rs. ${"%,.2f".format(tx.feePkr)}"
            )

            var y = 735f
            rows.forEachIndexed { index, (label, value) ->
                canvas.drawText(label, 112f, y, labelPaint)
                canvas.drawText(value.take(28), width - 112f, y, valuePaint)
                if (index < rows.lastIndex) {
                    canvas.drawLine(112f, y + 26f, width - 112f, y + 26f, dividerPaint)
                }
                y += 74f
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6B7280")
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Secured by $walletName", width / 2f, 1430f, footerPaint)

            val fileName = "Receipt_${tx.transactionRef}.png"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ApnaWallet")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: error("Could not create MediaStore entry")

            resolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: error("Failed to write receipt image")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            imageUri
        }
    }
}
