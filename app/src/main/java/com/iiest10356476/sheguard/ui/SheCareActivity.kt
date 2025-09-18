package com.iiest10356476.sheguard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.iiest10356476.sheguard.R

class SheCareActivity :  BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_she_care)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        setupBottomNavigation()
    }

    private fun setupUI() {
        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        // Set up emergency contact buttons
        setupEmergencyContacts()

        // Set up podcast section
        setupPodcastSection()

        // Set up video section
        setupVideoSection()

        // Set up information sections
        setupInformationSections()
    }

    private fun setupEmergencyContacts() {
        val tearsHotlineButton = findViewById<LinearLayout>(R.id.tears_hotline_button)
        val policeButton = findViewById<LinearLayout>(R.id.police_button)
        val supportLocatorButton = findViewById<LinearLayout>(R.id.support_locator_button)

        tearsHotlineButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:0800083277")
            }
            startActivity(intent)
        }

        policeButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:10111")
            }
            startActivity(intent)
        }

        supportLocatorButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:*134*7355#")
            }
            startActivity(intent)
        }
    }

    private fun setupPodcastSection() {
        // Podcast containers (clicking anywhere on the card)
        val podcast1 = findViewById<LinearLayout>(R.id.podcast_1)
        val podcast2 = findViewById<LinearLayout>(R.id.podcast_2)
        val podcast3 = findViewById<LinearLayout>(R.id.podcast_3)

        // Setup podcast card click listeners - this will handle both card and image clicks
        podcast1.setOnClickListener {
            openSpotifyLink("https://open.spotify.com/episode/2Y475ZaQHN7BAE2spEpeI3?si=XbOCrUJ9R7C0CsO6O8SPig")
        }
        podcast2.setOnClickListener {
            openSpotifyLink("https://open.spotify.com/episode/6umqGGGbpaOoRVs7IIqJSw?si=gNM563r-Sp-huLy-Vw5XJw")
        }
        podcast3.setOnClickListener {
            openSpotifyLink("https://open.spotify.com/show/6p4PBAYF09M06TIP8oWVk9")
        }
    }

    private fun setupVideoSection() {
        // Video containers (clicking anywhere on the card)
        val video1 = findViewById<LinearLayout>(R.id.video_1)
        val video2 = findViewById<LinearLayout>(R.id.video_2)
        val video3 = findViewById<LinearLayout>(R.id.video_3)

        // Setup video card click listeners - this will handle both card and play button clicks
        video1.setOnClickListener {
            openYouTubeLink("https://youtu.be/AfWHGpwWE1o?si=NJmCadhC4Uw089Nw")
        }
        video2.setOnClickListener {
            openYouTubeLink("https://youtu.be/pndPbpHLpos?si=Cy_YOihlhLab3GCz")
        }
        video3.setOnClickListener {
            openYouTubeLink("https://youtu.be/9I14yOakmt0?si=8h8pbp9NpoE9gMUo")
        }
    }

    private fun setupInformationSections() {
        val domesticViolenceInfo = findViewById<LinearLayout>(R.id.domestic_violence_info)
        val emergencyResponseInfo = findViewById<LinearLayout>(R.id.emergency_response_info)
        val reportingGuideInfo = findViewById<LinearLayout>(R.id.reporting_guide_info)

        domesticViolenceInfo.setOnClickListener {
            // You can create a detailed information activity later
            // val intent = Intent(this, GBVInfoDetailActivity::class.java)
            // intent.putExtra("info_type", "domestic_violence")
            // startActivity(intent)
        }

        emergencyResponseInfo.setOnClickListener {
            // val intent = Intent(this, GBVInfoDetailActivity::class.java)
            // intent.putExtra("info_type", "emergency_response")
            // startActivity(intent)
        }

        reportingGuideInfo.setOnClickListener {
            // val intent = Intent(this, GBVInfoDetailActivity::class.java)
            // intent.putExtra("info_type", "reporting_guide")
            // startActivity(intent)
        }
    }

    private fun openSpotifyLink(spotifyUrl: String) {
        try {
            // First, try to open with Spotify app using the spotify: URI scheme
            val spotifyUri = convertWebUrlToSpotifyUri(spotifyUrl)
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri))
            spotifyIntent.setPackage("com.spotify.music")
            startActivity(spotifyIntent)
        } catch (e: Exception) {
            try {
                // If that fails, try the web URL with Spotify app
                val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl))
                spotifyIntent.setPackage("com.spotify.music")
                startActivity(spotifyIntent)
            } catch (e2: Exception) {
                // If Spotify app is not installed, open in browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl))
                startActivity(browserIntent)
            }
        }
    }

    private fun openYouTubeLink(youtubeUrl: String) {
        try {
            // Extract video ID from YouTube URL for better app compatibility
            val videoId = extractYouTubeVideoId(youtubeUrl)
            if (videoId != null) {
                // Try to open with YouTube app using video ID
                val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
                youtubeIntent.setPackage("com.google.android.youtube")
                startActivity(youtubeIntent)
            } else {
                throw Exception("Could not extract video ID")
            }
        } catch (e: Exception) {
            try {
                // If that fails, try with regular YouTube URL
                val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                youtubeIntent.setPackage("com.google.android.youtube")
                startActivity(youtubeIntent)
            } catch (e2: Exception) {
                // If YouTube app is not installed, open in browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                startActivity(browserIntent)
            }
        }
    }

    private fun convertWebUrlToSpotifyUri(webUrl: String): String {
        // Convert Spotify web URL to spotify: URI scheme
        return when {
            webUrl.contains("/episode/") -> {
                val episodeId = webUrl.substringAfter("/episode/").substringBefore("?")
                "spotify:episode:$episodeId"
            }
            webUrl.contains("/show/") -> {
                val showId = webUrl.substringAfter("/show/").substringBefore("?")
                "spotify:show:$showId"
            }
            webUrl.contains("/track/") -> {
                val trackId = webUrl.substringAfter("/track/").substringBefore("?")
                "spotify:track:$trackId"
            }
            else -> webUrl // Return original if no pattern matches
        }
    }

    private fun extractYouTubeVideoId(url: String): String? {
        return when {
            url.contains("youtu.be/") -> {
                url.substringAfter("youtu.be/").substringBefore("?")
            }
            url.contains("watch?v=") -> {
                url.substringAfter("watch?v=").substringBefore("&")
            }
            url.contains("/embed/") -> {
                url.substringAfter("/embed/").substringBefore("?")
            }
            else -> null
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.itemIconTintList = null
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_shetrack -> {
                    val intent = Intent(this, TrackingEventActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_vault -> {
                    val intent = Intent(this, SecureVault::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_community -> {
                    // Already on SHECare/Community - no action needed
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set the current item as selected (Community)
        bottomNavigation.selectedItemId = R.id.nav_community
    }
}