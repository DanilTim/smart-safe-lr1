package com.example.laba1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.laba1.SetupActivity.Companion.KEY_COMBO
import com.example.laba1.SetupActivity.Companion.PREFS_NAME
import com.example.laba1.SetupActivity.Companion.toEmoji


class MainActivity : AppCompatActivity() {

    private lateinit var tiltDetector: TiltDetector
    private lateinit var tvStatus: TextView
    private lateinit var tvInput: TextView
    private lateinit var tvSavedCombo: TextView
    private lateinit var btnSetup: Button
    private lateinit var btnReset: Button

    // Буфер последних N наклонов (N = длина комбинации)
    private val inputBuffer = mutableListOf<TiltDirection>()
    private var savedCombo: List<TiltDirection> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus      = findViewById(R.id.tvStatus)
        tvInput       = findViewById(R.id.tvInput)
        tvSavedCombo  = findViewById(R.id.tvSavedCombo)
        btnSetup      = findViewById(R.id.btnSetup)
        btnReset      = findViewById(R.id.btnReset)

        tiltDetector = TiltDetector(this) { direction ->
            onTiltDetected(direction)
        }

        btnSetup.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        btnReset.setOnClickListener {
            resetInput()
        }
    }

    // ── Жизненный цикл ────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        loadSavedCombo()
        tiltDetector.register()
    }

    override fun onPause() {
        super.onPause()
        tiltDetector.unregister()
    }

    // ── Логика ────────────────────────────────────────────────────────────────

    private fun loadSavedCombo() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_COMBO, null)

        savedCombo = if (raw.isNullOrEmpty()) {
            emptyList()
        } else {
            raw.split(",").mapNotNull {
                runCatching { TiltDirection.valueOf(it) }.getOrNull()
            }
        }

        val comboEmoji = savedCombo.joinToString(" ") { it.toEmoji() }
        tvSavedCombo.text = if (savedCombo.isEmpty()) "Не задана" else comboEmoji

        tvStatus.text = when {
            !tiltDetector.isAvailable() -> "⚠️ Акселерометр недоступен"
            savedCombo.isEmpty()        -> "Сначала задайте комбинацию →"
            else                        -> "Введите комбинацию наклонами"
        }

        resetInput()
    }

    private fun onTiltDetected(direction: TiltDirection) {
        if (savedCombo.isEmpty()) return

        // Добавляем в буфер и обрезаем до длины комбинации
        inputBuffer.add(direction)
        if (inputBuffer.size > savedCombo.size) {
            inputBuffer.removeAt(0)
        }

        updateInputUI()
        checkCombo()
    }

    private fun checkCombo() {
        if (inputBuffer.size < savedCombo.size) return   // ещё не набрали нужное количество

        if (inputBuffer == savedCombo) {
            onSuccess()
        }
        // При неверном вводе буфер продолжает накапливаться (скользящее окно),
        // ничего дополнительно не сбрасываем — удобнее для пользователя.
    }

    private fun onSuccess() {
        Toast.makeText(this, "🔓 Сейф открыт!", Toast.LENGTH_LONG).show()
        tvStatus.text = "🔓 Доступ разрешён!"
        inputBuffer.clear()
        updateInputUI()
    }

    private fun resetInput() {
        inputBuffer.clear()
        updateInputUI()
    }

    private fun updateInputUI() {
        tvInput.text = if (inputBuffer.isEmpty()) {
            "—"
        } else {
            inputBuffer.joinToString(" ") { it.toEmoji() }
        }
    }
}