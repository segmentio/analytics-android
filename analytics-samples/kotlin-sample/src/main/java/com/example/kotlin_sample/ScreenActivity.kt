/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.example.kotlin_sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlin_sample.databinding.ActivityScreenBinding
import com.segment.analytics.Analytics

class ScreenActivity : AppCompatActivity() {

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

    private lateinit var binding: ActivityScreenBinding

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
