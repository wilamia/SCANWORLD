package com.example.scanworld

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CreateBarcode : AppCompatActivity() {
    private lateinit var urlEditText: EditText
    private lateinit var qrCodeImageView: ImageView
    private lateinit var downloadButton: Button
    private lateinit var generateButton: Button
    private lateinit var backToHomeButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: com.google.android.material.navigation.NavigationView
    private var qrCodeBitmap: Bitmap? = null

    private val REQUEST_CODE_STORAGE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_barcode)

        // Инициализация компонентов
        urlEditText = findViewById(R.id.urlEditText)
        qrCodeImageView = findViewById(R.id.barcodeImageView)
        downloadButton = findViewById(R.id.downloadButton)
        generateButton = findViewById(R.id.generateButton)
        backToHomeButton = findViewById(R.id.back_to_home_button_3)
        drawerLayout = findViewById(R.id.drawer_layout) // Убедитесь, что ID соответствует вашему XML
        navigationView = findViewById(R.id.nav_view)

        // Настройка тулбара
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.frame_4) // Убедитесь, что иконка существует

        // Обработчик нажатий для открытия бокового меню
        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // Настройка NavigationView
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
                    // Отправка email
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:scanworld@gmail.com") // только email
                    }
                    startActivity(emailIntent)
                    drawerLayout.close()
                    true
                }
                else -> false
            }
        }

        downloadButton.isEnabled = false

        generateButton.setOnClickListener {
            val url = urlEditText.text.toString()
            if (url.isNotEmpty()) {
                qrCodeBitmap = generateQRCode(url)
                qrCodeImageView.setImageBitmap(qrCodeBitmap)
                downloadButton.isEnabled = qrCodeBitmap != null
            } else {
                Toast.makeText(this, "Введите ссылку", Toast.LENGTH_SHORT).show()
            }
        }

        downloadButton.setOnClickListener {
            qrCodeBitmap?.let { bitmap -> saveImage(bitmap) } ?: Toast.makeText(this, "QR-код не сгенерирован", Toast.LENGTH_SHORT).show()
        }

        backToHomeButton.setOnClickListener {
            finish()  // Возвращение на главный экран
        }
    }

    private fun generateQRCode(content: String): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 600, 600)
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.createBitmap(bitMatrix)
        } catch (e: WriterException) {
            Log.e("CreateBarcode", "Ошибка генерации QR-кода", e)
            Toast.makeText(this, "Ошибка генерации QR-кода: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun saveImage(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                writeImageToStorage(bitmap)
            } else {
                requestManageStoragePermission()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                writeImageToStorage(bitmap)
            } else {
                requestWriteExternalStoragePermission()
            }
        }
    }

    private fun requestWriteExternalStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE)
    }

    private fun requestManageStoragePermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, REQUEST_CODE_STORAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    qrCodeBitmap?.let { bitmap -> writeImageToStorage(bitmap) }
                } else {
                    Toast.makeText(this, "Разрешение на управление файлами не получено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun writeImageToStorage(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "qrcode_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                try {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        Toast.makeText(this, "QR-код сохранен", Toast.LENGTH_LONG).show()
                    }
                } catch (e: IOException) {
                    Log.e("CreateBarcode", "Ошибка сохранения изображения", e)
                    Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(path, "qrcode_${System.currentTimeMillis()}.png")
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                Toast.makeText(this, "QR-код сохранен в ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("CreateBarcode", "Ошибка сохранения изображения", e)
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
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
}