package com.example.scanworld

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutUs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включаем edge-to-edge режим
        setContentView(R.layout.activity_about_us)

        // Находим кнопку "Назад на главную"
        val backToHomeButton = findViewById<Button>(R.id.back_to_home_button_3)
        backToHomeButton.setOnClickListener {
            finish() // Закрываем текущую активность
        }

        // Устанавливаем обработчик для отступов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.back_to_home_button)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets // Возвращаем insets для дальнейшей обработки
        }
    }
}