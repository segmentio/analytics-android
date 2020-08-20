package com.example.kotlin_sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.example.kotlin_sample.databinding.ActivityScreenBinding
import com.segment.analytics.Analytics

class ScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initialSetup()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.docs -> {
                val openDocs = Intent(Intent.ACTION_VIEW)
                openDocs.data = Uri.parse("https://segment.com/docs/tutorials/quickstart-android/")
                startActivity(openDocs)
                try {
                    startActivity(openDocs)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "No browser to open link", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onFlushClicked() {
        Analytics.with(this).flush()
        Toast.makeText(this, "Events flushed", Toast.LENGTH_SHORT).show()
    }

    private fun initialSetup() {
        Analytics.with(this).screen("Screen activity viewed")

        val returnMessage =
                """
                Screen event has been recorded!
                Press 'Flush' to send events to Segment.
                """.trimIndent()
        binding.screenResult.text = returnMessage

        binding.screenFlush.setOnClickListener { onFlushClicked() }
    }
}