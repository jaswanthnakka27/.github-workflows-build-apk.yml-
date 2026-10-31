package com.karen.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(5, 10, 24)
    private val panel = Color.rgb(13, 23, 43)
    private val cyan = Color.rgb(54, 220, 255)
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var send: Button
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var apiKey = ""
    private val prefs by lazy { getSharedPreferences("karen_settings", MODE_PRIVATE) }
    private val history = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        apiKey = prefs.getString("gemini_key", "") ?: ""
        buildScreen()
        if (apiKey.isBlank()) showKeyDialog()
        addMessage("KAREN", "Systems online. I'm ready. Add your Gemini API key in Settings to activate my AI brain.")
    }

    private fun rounded(color: Int, radius: Float = 22f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildScreen() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 22, 18, 14)
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(label("K A R E N", 25f, cyan, true))
        titleBox.addView(label("PERSONAL AI • GEMINI POWERED", 10f, Color.LTGRAY))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val settings = Button(this).apply {
            text = "⚙"; textSize = 18f; setTextColor(cyan)
            background = rounded(panel, 18f); setOnClickListener { showKeyDialog() }
        }
        header.addView(settings, LinearLayout.LayoutParams(54, 48))
        outer.addView(header)

        val orb = TextView(this).apply {
            text = "K"; textSize = 38f; gravity = Gravity.CENTER
            setTextColor(cyan); typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(14, 65, 100), Color.rgb(8, 18, 42))).apply {
                shape = GradientDrawable.OVAL
                setStroke(2, cyan)
            }
        }
        val orbParams = LinearLayout.LayoutParams(112, 112).apply {
            gravity = Gravity.CENTER; topMargin = 22; bottomMargin = 8
        }
        outer.addView(orb, orbParams)
        status = label("● READY TO ASSIST", 11f, cyan, true).apply { gravity = Gravity.CENTER }
        outer.addView(status)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = rounded(panel, 20f)
            setPadding(12, 12, 12, 12)
        }
        messages = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(messages)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = 18; bottomMargin = 12
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        input = EditText(this).apply {
            hint = "Ask KAREN anything..."
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            textSize = 15f; singleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(16, 0, 16, 0)
            background = rounded(panel, 24f)
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        row.addView(input, LinearLayout.LayoutParams(0, 54, 1f))
        val mic = Button(this).apply {
            text = "🎙"; textSize = 18f; setTextColor(cyan)
            background = rounded(panel, 18f)
            setOnClickListener { startVoiceInput() }
        }
        row.addView(mic, LinearLayout.LayoutParams(52, 52).apply { leftMargin = 6 })
        send = Button(this).apply {
            text = "➤"; textSize = 20f; setTextColor(Color.BLACK)
            background = rounded(cyan, 18f); setOnClickListener { sendMessage() }
        }
        row.addView(send, LinearLayout.LayoutParams(52, 52).apply { leftMargin = 6 })
        outer.addView(row)
        setContentView(outer)
    }

    private fun addMessage(who: String, text: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(if (who == "YOU") Color.rgb(18, 48, 67) else Color.rgb(18, 31, 54), 16f)
            setPadding(14, 10, 14, 10)
        }
        card.addView(label(who, 10f, cyan, true))
        card.addView(label(text, 15f, Color.WHITE).apply { setPadding(0, 5, 0, 0) })
        messages.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        messages.post { (messages.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendMessage() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        input.text.clear()
        addMessage("YOU", prompt)
        if (apiKey.isBlank()) { addMessage("KAREN", "Please add your Gemini API key using the ⚙ button."); return }
        send.isEnabled = false
        status.text = "● THINKING..."
        thread {
            val answer = try { askGemini(prompt) } catch (e: Exception) {
                "I couldn't reach Gemini: ${e.message ?: "connection error"}. Check your internet and API key."
            }
            runOnUiThread {
                addMessage("KAREN", answer)
                status.text = "● READY TO ASSIST"
                send.isEnabled = true
            }
        }
    }

    private fun askGemini(prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        val contents = JSONArray()
        contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))
        val body = JSONObject().put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                "You are KAREN, a helpful, friendly, concise personal AI assistant. Be accurate and explain clearly."))))
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val json = JSONObject(response)
        if (!json.has("candidates")) throw Exception(json.optJSONObject("error")?.optString("message") ?: "Gemini returned no answer")
        return json.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text", "I couldn't form a reply.")
    }

    private fun showKeyDialog() {
        val field = EditText(this).apply {
            hint = "Paste Gemini API key"; setText(apiKey); setSingleLine(true)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(22, 12, 22, 0)
            addView(label("Your key is stored locally on this device. Never commit it to GitHub.", 13f, Color.DKGRAY))
            addView(field)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Connect Gemini to KAREN")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                apiKey = field.text.toString().trim()
                prefs.edit().putString("gemini_key", apiKey).apply()
                addMessage("KAREN", if (apiKey.isNotBlank()) "Gemini key saved on this device. Ready when you are." else "No API key saved.")
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to KAREN")
        }
        try { startActivityForResult(intent, 42) }
        catch (_: Exception) { addMessage("KAREN", "Voice recognition isn't available on this device.") }
    }

    @Deprecated("Deprecated in Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) { input.setText(spoken); sendMessage() }
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) tts.language = Locale.getDefault()
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
