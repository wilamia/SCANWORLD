package com.example.scanworld

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.URLUtil.isValidUrl
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class BarcodeScanner : AppCompatActivity() {
    private lateinit var barcodeView: BarcodeView
    private lateinit var scanResultTextView: TextView
    private lateinit var flashlightButton: Button
    private lateinit var scanAgainButton: Button
    private lateinit var backToHomeButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var result: TextView
    private lateinit var navigationView: com.google.android.material.navigation.NavigationView
    private var isFlashlightOn = false
    private var hasScanned = false

    private companion object {
        const val CAMERA_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_barcode_scanner)

        // Инициализация компонентов
        barcodeView = findViewById(R.id.barcode_scanner)
        scanResultTextView = findViewById(R.id.scan_result)
        flashlightButton = findViewById(R.id.flashlight_button)
        scanAgainButton = findViewById(R.id.scan_again_button)
        backToHomeButton = findViewById(R.id.back_to_home_button)
        drawerLayout = findViewById(R.id.back_to_home_button)
        navigationView = findViewById(R.id.nav_view)
        result = findViewById(R.id.scan_result)
        result.setMovementMethod(ScrollingMovementMethod())

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.frame_4)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        } else {
            setupScanner()
        }

        flashlightButton.setOnClickListener {
            toggleFlashlight()
        }

        scanAgainButton.setOnClickListener {
            hasScanned = false
            scanResultTextView.text = ""
            setupScanner()
        }

        backToHomeButton.setOnClickListener {
            finish()
        }

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.main_page -> {
                    val intentMain = Intent(this, MainActivity::class.java)
                    startActivity(intentMain)
                    drawerLayout.close()
                    true
                }

                R.id.about_us -> {
                    val intentAbout = Intent(this, AboutUs::class.java)
                    startActivity(intentAbout)
                    drawerLayout.close()
                    true
                }

                R.id.instagram -> {
                    val instagramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/your_instagram_account"))
                    startActivity(instagramIntent)
                    drawerLayout.close()
                    true
                }

                R.id.facebook -> {
                    val facebookIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/your_facebook_account"))
                    startActivity(facebookIntent)
                    drawerLayout.close()
                    true
                }

                R.id.phone_number -> {
                    val phoneIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+37500000000"))
                    startActivity(phoneIntent)
                    drawerLayout.close()
                    true
                }

                R.id.email -> {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:scanworld@gmail.com")
                    }
                    startActivity(emailIntent)
                    drawerLayout.close()
                    true
                }

                else -> false
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun setupScanner() {
        barcodeView.decoderFactory = DefaultDecoderFactory()

        val settings = CameraSettings().apply {
            isAutoFocusEnabled = true
        }
        barcodeView.cameraSettings = settings

        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (!hasScanned) {
                    hasScanned = true

                    if (result.barcodeFormat == com.google.zxing.BarcodeFormat.QR_CODE) {
                        val scannedText = result.text
                        scanResultTextView.text = "QR Code отсканирован: $scannedText"

                        scanResultTextView.setOnClickListener {
                            if (isValidUrl(scannedText)) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedText))
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@BarcodeScanner, "Отсанированный URL невалидный", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {

                        scanResultTextView.text = "Штрих-код: ${result.text}"
                        fetchProductInfo(result.text)
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>) {}
        })
    }
    @SuppressLint("SetTextI18n")
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
                        val countries = product.countries_tags
                            ?.map { it.replace("en:", "").trim().capitalize() }
                            ?.joinToString(", ") ?: "N/A"

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
    private fun showToast(message: String) {
        Toast.makeText(scanResultTextView.context, message, Toast.LENGTH_SHORT).show()
    }

    private fun toggleFlashlight() {
        isFlashlightOn = !isFlashlightOn
        barcodeView.setTorch(isFlashlightOn)
        flashlightButton.text = if (isFlashlightOn) "Выключить фонарик" else "Включить фонарик"
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupScanner()
            } else {
                scanResultTextView.text = "Запрос на использование камеры отклонен."
            }
        }
    }
}