package com.iiest10356476.sheguard.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.iiest10356476.sheguard.utils.LanguageHelper

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language on activity creation
        applyLanguage()
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val languageCode = LanguageHelper.getSavedLanguage(newBase)
            val context = LanguageHelper.setAppLanguage(newBase, languageCode)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    private fun applyLanguage() {
        val languageCode = LanguageHelper.getSavedLanguage(this)
        LanguageHelper.setAppLanguage(this, languageCode)
    }

    /**
     * Call this method when language is changed to restart the activity
     */
    protected fun onLanguageChanged() {
        recreate()
    }
}