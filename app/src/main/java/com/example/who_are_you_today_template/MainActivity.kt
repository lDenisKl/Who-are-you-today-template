package com.example.who_are_you_today_template

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var allMemeNames: List<String>  // имена всех картинок в drawable
    private lateinit var phrases: Array<String>      // фразы из ресурсов

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        prefs = getSharedPreferences("MemePrefs", MODE_PRIVATE)
        phrases = resources.getStringArray(R.array.phrases_array)

        allMemeNames = getAllDrawableNames()

        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastSelectedDate = prefs.getString("last_selected_date", "")
        val questionText = findViewById<TextView>(R.id.question_text)

        if (lastSelectedDate == today) {
            val selectedImageName = prefs.getString("selected_image", null)
            if (selectedImageName != null) {
                questionText.text = "Твоя картинка на сегодня:"
                val phrase = prefs.getString("selected_phrase", "")
                showSelectedMeme(selectedImageName, phrase ?: "")
            } else {
                questionText.text = "Кто ты сегодня?"
                setupTodaySelection()
            }
        } else {
            questionText.text = "Кто ты сегодня?"
            setupTodaySelection()
        }
    }

    /**
     * Получает список имён всех ресурсов drawable
     */
    private fun getAllDrawableNames(): List<String> {
        val fields: Array<Field> = R.drawable::class.java.fields
        val excludePrefixes = listOf("ic_launcher", "abc_", "design_", "material_")
        return fields
            .map { it.name }
            .filter { name ->
                excludePrefixes.none { prefix -> name.startsWith(prefix) }
            }
            .sorted()
    }

    /**
     * Настройка экрана выбора
     */
    private fun setupTodaySelection() {
        findViewById<ImageView>(R.id.selected_meme_image).visibility = View.GONE
        val gridLayout = findViewById<GridLayout>(R.id.meme_grid)
        gridLayout.visibility = View.VISIBLE

        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastSelectionDate = prefs.getString("last_selection_date", "")

        val todaysMemeNames = if (lastSelectionDate == today) {
            getTodaysMemeNamesFromPrefs()
        } else {
            val newSet = generateRandomMemeSet()
            saveTodaysMemeNamesToPrefs(newSet)
            prefs.edit().putString("last_selection_date", today).apply()
            newSet
        }

        val imageViewIds = listOf(
            R.id.btn_meme_1, R.id.btn_meme_2,
            R.id.btn_meme_3, R.id.btn_meme_4,
            R.id.btn_meme_5, R.id.btn_meme_6,
            R.id.btn_meme_7, R.id.btn_meme_8
        )

        for (index in todaysMemeNames.indices) {
            val imageView = findViewById<ImageView>(imageViewIds[index])
            val imageName = todaysMemeNames[index]
            val resourceId = resources.getIdentifier(imageName, "drawable", packageName)
            if (resourceId != 0) {
                imageView.setImageResource(resourceId)
                imageView.setOnClickListener { onMemeSelected(imageName) }
                imageView.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.GONE
            }
        }
        for (index in todaysMemeNames.size until imageViewIds.size) {
            findViewById<ImageView>(imageViewIds[index]).visibility = View.GONE
        }
    }

    /**
     * Генерация случайного набора мемов
     */
    private fun generateRandomMemeSet(): List<String> {
        val shuffled = allMemeNames.shuffled()
        return shuffled.take(minOf(8, allMemeNames.size))
    }

    /**
     * Сохранить сегодняшний набор в Preferences
     */
    private fun saveTodaysMemeNamesToPrefs(memeNames: List<String>) {
        val jsonArray = JSONArray()
        memeNames.forEach { jsonArray.put(it) }
        prefs.edit().putString("todays_memes", jsonArray.toString()).apply()
    }

    /**
     * Получить сегодняшний набор из Preferences
     */
    private fun getTodaysMemeNamesFromPrefs(): List<String> {
        val jsonString = prefs.getString("todays_memes", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        return (0 until jsonArray.length()).map { jsonArray.getString(it) }
    }

    /**
     * Отображение выбранного мема и фразы
     */
    private fun showSelectedMeme(imageName: String, phrase: String) {
        findViewById<GridLayout>(R.id.meme_grid).visibility = View.GONE
        findViewById<TextView>(R.id.result_text).text = phrase
        findViewById<TextView>(R.id.question_text).text = "Твоя картинка на сегодня:"

        val selectedImageView = findViewById<ImageView>(R.id.selected_meme_image)
        val resourceId = resources.getIdentifier(imageName, "drawable", packageName)
        if (resourceId != 0) {
            selectedImageView.setImageResource(resourceId)
            selectedImageView.visibility = View.VISIBLE
        }
    }

    /**
     * Обработка клика по картинке
     */
    private fun onMemeSelected(imageName: String) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val phrase = phrases.random()

        updateStreak(today)

        prefs.edit().apply {
            putString("last_selected_date", today)
            putString("selected_image", imageName)
            putString("selected_phrase", phrase)
            apply()
        }

        updateWidget()
        Toast.makeText(this, "Выбор сохранён до завтра!", Toast.LENGTH_SHORT).show()
        showSelectedMeme(imageName, phrase)
    }

    /**
     * streak
     */
    private fun updateStreak(today: String) {
        val lastDate = prefs.getString("last_selected_date", "")
        val currentStreak = prefs.getInt("streak", 0)

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayDate = sdf.parse(today)!!
        val lastDateObj = if (!lastDate.isNullOrEmpty()) sdf.parse(lastDate) else null

        val calendar = Calendar.getInstance()
        calendar.time = todayDate
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(calendar.time)

        val newStreak = when {
            lastDate == today -> currentStreak
            lastDate == yesterday -> currentStreak + 1
            lastDate.isNullOrEmpty() -> 1
            else -> 0
        }

        prefs.edit().putInt("streak", newStreak).apply()
    }

    /**
     * Обновление виджета
     */
    private fun updateWidget() {
        val intent = Intent(this, MemeWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(this)
            .getAppWidgetIds(ComponentName(this, MemeWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }
}