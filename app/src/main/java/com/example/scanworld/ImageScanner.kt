package com.example.scanworld

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Suppress("DEPRECATION")
class ImageScanner : AppCompatActivity() {
    private lateinit var selectImageButton: Button
    private lateinit var scanResultTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var backToHomeButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var croppedBitmap: Bitmap? = null

    private companion object {
        const val PICK_IMAGE_REQUEST = 1001
        const val CROP_IMAGE_REQUEST = 1002
        const val PERMISSION_REQUEST_CODE = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_scanner)

        checkPermissions()

        scanResultTextView = findViewById(R.id.scan_result)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        scanResultTextView.movementMethod = ScrollingMovementMethod()

        selectImageButton = findViewById(R.id.select_image_button)
        imageView = findViewById(R.id.image_view)
        backToHomeButton = findViewById(R.id.back_to_home_button_image)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
            overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
        }

        backToHomeButton.setOnClickListener {
            finish()
            overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.main_page -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    drawerLayout.close()
                    overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
                    true
                }
                R.id.about_us -> {
                    startActivity(Intent(this, AboutUs::class.java))
                    drawerLayout.close()
                    overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
                    true
                }
                R.id.instagram -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/your_instagram_account")))
                    drawerLayout.close()
                    overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
                    true
                }
                R.id.facebook -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/your_facebook_account")))
                    drawerLayout.close()
                    overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
                    true
                }
                R.id.phone_number -> {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+37500000000")))
                    drawerLayout.close()
                    overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
                    true
                }
                R.id.email -> {
                    startActivity(Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:scanworld@gmail.com")
                    })
                    drawerLayout.close()
                    overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
                    true
                }
                else -> false
            }
        }

        selectImageButton.setOnClickListener {
            openImagePicker()
            overridePendingTransition(R.animator.no_animation_in, R.animator.no_animation_out)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null -> {
                val imageUri: Uri? = data.data
                imageUri?.let { cropImage(it) }
            }
            requestCode == CROP_IMAGE_REQUEST && resultCode == RESULT_OK && data != null -> {
                val extras = data.extras
                croppedBitmap = extras?.getParcelable("data")
                croppedBitmap?.let {
                    imageView.setImageBitmap(it)
                    decodeBarcodeFromImage(it)
                } ?: run {
                    Toast.makeText(this, "Ошибка: не удалось получить обрезанное изображение.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cropImage(imageUri: Uri) {
        val cropIntent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(imageUri, "image/*")
            putExtra("crop", "true")
            putExtra("aspectX", 3.5)
            putExtra("aspectY", 2)
            putExtra("outputX", 350)
            putExtra("outputY", 350)
            putExtra("return-data", true)
        }
        startActivityForResult(cropIntent, CROP_IMAGE_REQUEST)
    }

    @SuppressLint("SetTextI18n")
    private fun decodeBarcodeFromImage(bitmap: Bitmap) {
        val orientations = listOf(0, 90, 180, 270)
        for (angle in orientations) {
            val rotatedBitmap = rotateBitmap(bitmap, angle)
            try {
                val multiFormatReader = MultiFormatReader()
                multiFormatReader.setHints(
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(
                            BarcodeFormat.AZTEC,
                            BarcodeFormat.CODABAR,
                            BarcodeFormat.CODE_39,
                            BarcodeFormat.CODE_93,
                            BarcodeFormat.CODE_128,
                            BarcodeFormat.DATA_MATRIX,
                            BarcodeFormat.EAN_8,
                            BarcodeFormat.EAN_13,
                            BarcodeFormat.ITF,
                            BarcodeFormat.PDF_417,
                            BarcodeFormat.QR_CODE,
                            BarcodeFormat.UPC_A,
                            BarcodeFormat.UPC_E,
                            BarcodeFormat.RSS_14,
                            BarcodeFormat.RSS_EXPANDED,
                            BarcodeFormat.MAXICODE,
                            BarcodeFormat.UPC_EAN_EXTENSION
                        )
                    )
                )

                val source = MyBitmapLuminanceSource(rotatedBitmap)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result: Result = multiFormatReader.decode(binaryBitmap)

                if (result.barcodeFormat == BarcodeFormat.QR_CODE) {
                    val scannedText = result.text
                    scanResultTextView.text = "QR-код отсканирован: $scannedText"
                    Log.d("ImageScanner", "QR-код отсканирован: $scannedText")

                    scanResultTextView.setOnClickListener {
                        if (isValidUrl(scannedText)) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedText))
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Отсканированный URL невалидный", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    scanResultTextView.text = "Штрих-код: ${result.text}"
                    Log.d("ImageScanner", "Штрих-код: ${result.text}")
                    fetchProductInfo(result.text)
                }
                return
            } catch (e: NotFoundException) {
                Log.e("ImageScanner", "Штрих-код не найден: ${e.message}")
            } catch (e: ChecksumException) {
                scanResultTextView.text = "Checksum error: ${e.message}"
                Log.e("ImageScanner", "Checksum error", e)
                return
            } catch (e: FormatException) {
                scanResultTextView.text = "Ошибка формата: ${e.message}"
                Log.e("ImageScanner", "Format error", e)
                return
            } catch (e: Exception) {
                scanResultTextView.text = "Ошибка расшифровки штрих-кода: ${e.message}"
                Log.e("ImageScanner", "Error decoding barcode", e)
                return
            }
        }
        scanResultTextView.text = "Штрих-код не найден."
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    private fun rotateBitmap(source: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun fetchProductInfo(barcode: String) {
        Log.d("ImageScanner", "Информация о штрих-коде: $barcode")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getProductByBarcode(barcode)
                val product = response.product
                runOnUiThread {
                    if (product != null) {
                        val displayBrand = product.brands?.capitalize() ?: "N/A"
                        val displayName = product.product_name?.capitalize() ?: "N/A"
                        val countries = product.countries_tags?.joinToString(", ") {
                            it.replace(
                                "en:",
                                ""
                            ).trim().capitalize()
                        } ?: "N/A"

                        scanResultTextView.text = """
                        Штрих-код: $barcode
                        Бренд: $displayBrand
                        Название продукта: $displayName
                        Страны: $countries
                        """.trimIndent()

                        val productLink = product.link
                        if (!productLink.isNullOrEmpty()) {
                            scanResultTextView.append("\nСсылка: $productLink")
                            scanResultTextView.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(productLink))
                                startActivity(intent)
                            }
                        }
                    } else {
                        scanResultTextView.text = "Информация для штрих-кода не найдена: $barcode"
                    }
                }
            } catch (e: HttpException) {
                runOnUiThread {
                    scanResultTextView.text = "HTTP error: ${e.message()}"
                    Log.e("ImageScanner", "HTTP error", e)
                }
            } catch (e: IOException) {
                runOnUiThread {
                    scanResultTextView.text = "Ошибка подключения: ${e.message}"
                    Log.e("ImageScanner", "Ошибка подключения", e)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    scanResultTextView.text = "Ошибка поиска информации: ${e.message}"
                    Log.e("ImageScanner", "Ошибка поиска информации", e)
                }
            }
        }
    }

    private class MyBitmapLuminanceSource(bitmap: Bitmap) : LuminanceSource(bitmap.width, bitmap.height) {
        private val luminances: ByteArray = ByteArray(bitmap.width * bitmap.height)

        init {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val pixel = pixels[y * bitmap.width + x]
                    val luminance = (0.299 * ((pixel shr 16) and 0xff) +
                            0.587 * ((pixel shr 8) and 0xff) +
                            0.114 * (pixel and 0xff)).toInt()
                    luminances[y * bitmap.width + x] = luminance.coerceIn(0, 255).toByte()
                }
            }
        }

        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            if (row == null || row.size < width) {
                return ByteArray(width)
            }
            System.arraycopy(luminances, y * width, row, 0, width)
            return row
        }

        override fun getMatrix(): ByteArray {
            return luminances
        }
    }
}