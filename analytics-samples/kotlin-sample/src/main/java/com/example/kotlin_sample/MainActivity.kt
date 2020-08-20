package com.example.kotlin_sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlin_sample.databinding.ActivityMainBinding
import com.segment.analytics.Analytics
import com.segment.analytics.Properties
import com.segment.analytics.Traits
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    /** Returns true if the string is null, or empty (when trimmed).  */
    private fun isNullOrEmpty(text: String): Boolean {
        return TextUtils.isEmpty(text) || text.trim { it <= ' ' }.isEmpty()
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        //substitute with your own write key
        val analytics: Analytics =
                Analytics.Builder(this, "ek2TEqayWNAYGWRj4JQnuJLkG6lQgthL").build()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Analytics.setSingletonInstance(analytics)

        binding.actionTrackA.setOnClickListener { onAButtonClick() }
        binding.actionTrackB.setOnClickListener { onBButtonClick() }
        binding.identifyButton.setOnClickListener { onIdentifyClick() }
        binding.groupButton.setOnClickListener { onGroupClick() }
        binding.aliasButton.setOnClickListener { onAliasClick() }
        binding.screenButton.setOnClickListener { onScreenClick() }
        binding.flush.setOnClickListener { onFlushClick() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
            R.id.reset -> {
                Analytics.with(this).reset()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onAButtonClick() {
        Analytics.with(this).track("Button A clicked")
        Toast.makeText(this, "Button A clicked", Toast.LENGTH_SHORT).show()
    }

    private fun onBButtonClick() {
        Analytics.with(this).track("Button B clicked")
        Toast.makeText(this, "Button B clicked", Toast.LENGTH_SHORT).show()
    }

    private fun onIdentifyClick() {
        val name = identify_name.text.toString()
        val email = identify_email.text.toString()
        val age = identify_age.text.toString()
        val allFieldsEmpty = isNullOrEmpty(name) && isNullOrEmpty(email) && isNullOrEmpty(age)

        if (allFieldsEmpty) {
            Toast.makeText(this, "At least one field must be filled in", Toast.LENGTH_SHORT).show()
        } else {
            if (!isNullOrEmpty(name)) {
                Analytics.with(this).identify(Traits().putName(name))
            }
            if (!isNullOrEmpty(email)) {
                Analytics.with(this).identify(Traits().putEmail(email))
            }
            if (!isNullOrEmpty(age)) {
                Analytics.with(this).identify(Traits().putAge(age.toInt()))
            }
        }

        Toast.makeText(this, "Identification acknowledged", Toast.LENGTH_SHORT).show()
    }

    private fun onGroupClick() {
        val groupId = group_id.text.toString()

        if (isNullOrEmpty(groupId)) {
            Toast.makeText(this, "Cannot have an empty group ID", Toast.LENGTH_SHORT).show()
        } else {
            Analytics.with(this).group(groupId)
            Toast.makeText(this, "Group acknowledged", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onScreenClick() {
        val screenIntent = Intent(this, ScreenActivity::class.java)
        startActivity(screenIntent)
        Toast.makeText(this, "Screen acknowledged", Toast.LENGTH_SHORT).show()
    }

    private fun onAliasClick() {
        val aliasCopy = alias_text.text.toString()

        if(isNullOrEmpty(aliasCopy)) {
            Toast.makeText(this, "Alias cannot be empty", Toast.LENGTH_SHORT).show()
        } else {
            Analytics.with(this).alias(aliasCopy)
            Analytics.with(this).identify(aliasCopy)
        }

        Toast.makeText(this, "Alias acknowledged", Toast.LENGTH_SHORT).show()
    }

    private fun onFlushClick() {
        Analytics.with(this).flush()
        Toast.makeText(this, "Events flushed", Toast.LENGTH_SHORT).show()
    }
}
