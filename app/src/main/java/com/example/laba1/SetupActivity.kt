package com.example.laba1

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class SetupActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME   = "safe_prefs"
        const val KEY_COMBO    = "combination"
        const val MIN_COMBO_LENGTH = 3
        const val MAX_COMBO_LENGTH = 8


        fun TiltDirection.toEmoji() = when (this) {
            TiltDirection.LEFT  -> "⬅️"
            TiltDirection.RIGHT -> "➡️"
            TiltDirection.UP    -> "⬆️"
            TiltDirection.DOWN  -> "⬇️"
        }
    }

    private lateinit var tiltDetector: TiltDetector
    private lateinit var tvStatus: TextView
    private lateinit var tvCombo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button

    private val recordedCombo = mutableListOf<TiltDirection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        tvStatus  = findViewById(R.id.tvSetupStatus)
        tvCombo   = findViewById(R.id.tvRecordedCombo)
        btnSave   = findViewById(R.id.btnSaveCombo)
        btnClear  = findViewById(R.id.btnClearCombo)

        tiltDetector = TiltDetector(this) { direction ->
            onTiltDetected(direction)
        }

        if (!tiltDetector.isAvailable()) {
            tvStatus.text = "⚠️ Акселерометр не найден на этом устройстве"
            return
        }

        btnSave.setOnClickListener { saveCombo() }
        btnClear.setOnClickListener { clearCombo() }

        updateUI()
    }

    // ── Жизненный цикл сенсора ────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        tiltDetector.register()
    }

    override fun onPause() {
        super.onPause()
        tiltDetector.unregister()
    }

    // ── Логика ────────────────────────────────────────────────────────────────

    private fun onTiltDetected(direction: TiltDirection) {
        if (recordedCombo.size >= MAX_COMBO_LENGTH) {
            Toast.makeText(this, "Максимум $MAX_COMBO_LENGTH шагов. Сохраните или очистите.", Toast.LENGTH_SHORT).show()
            return
        }
        recordedCombo.add(direction)
        updateUI()
    }

    private fun saveCombo() {
        if (recordedCombo.size < MIN_COMBO_LENGTH) {
            Toast.makeText(
                this,
                "Нужно минимум $MIN_COMBO_LENGTH наклона. Сейчас: ${recordedCombo.size}",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val comboString = recordedCombo.joinToString(",") { it.name }
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COMBO, comboString)
            .apply()

        Toast.makeText(this, "✅ Комбинация сохранена!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun clearCombo() {
        recordedCombo.clear()
        updateUI()
    }

    private fun updateUI() {
        val emojiLine = recordedCombo.joinToString(" ") { it.toEmoji() }

        tvCombo.text = if (recordedCombo.isEmpty()) "—" else emojiLine

        tvStatus.text = when {
            recordedCombo.isEmpty()                  -> "Наклоняйте телефон, чтобы записать комбинацию"
            recordedCombo.size < MIN_COMBO_LENGTH    -> "Ещё ${MIN_COMBO_LENGTH - recordedCombo.size} наклон(а)…"
            recordedCombo.size >= MAX_COMBO_LENGTH   -> "Максимум достигнут. Нажмите «Сохранить»"
            else                                     -> "Готово! Можно сохранить или продолжить"
        }

        btnSave.isEnabled = recordedCombo.size >= MIN_COMBO_LENGTH
    }
}