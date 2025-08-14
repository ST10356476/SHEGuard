package com.iiest10356476.sheguard.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.iiest10356476.sheguard.R

class LoadingActivity : AppCompatActivity() {

    private lateinit var logoImage: ImageView
    private lateinit var appName: TextView
    private lateinit var slogan: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)


        logoImage = findViewById(R.id.logo_image)
        appName = findViewById(R.id.app_name)
        slogan = findViewById(R.id.slogan)


        startAnimations()


        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, GetStartedActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }

    private fun startAnimations() {

        val logoFadeIn = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f)
        logoFadeIn.duration = 800
        logoFadeIn.interpolator = DecelerateInterpolator()

        val logoScaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.5f, 1f)
        logoScaleX.duration = 800
        logoScaleX.interpolator = DecelerateInterpolator()

        val logoScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.5f, 1f)
        logoScaleY.duration = 800
        logoScaleY.interpolator = DecelerateInterpolator()


        logoFadeIn.start()
        logoScaleX.start()
        logoScaleY.start()


        Handler(Looper.getMainLooper()).postDelayed({

            val nameFadeIn = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f)
            nameFadeIn.duration = 600
            nameFadeIn.interpolator = DecelerateInterpolator()


            val sloganFadeIn = ObjectAnimator.ofFloat(slogan, "alpha", 0f, 1f)
            sloganFadeIn.duration = 600
            sloganFadeIn.interpolator = DecelerateInterpolator()


            nameFadeIn.start()
            sloganFadeIn.start()
        }, 500)
    }
}