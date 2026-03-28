dependencies {
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1

cloudstream {
    // All of these properties are optional, you can safely remove any of them.
    authors = listOf("eRHaDev")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1 // Will be 3 if unspecified

    tvTypes = listOf("TvSeries", "Live")

    requiresResources = true
    language = "en"

    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/a/a8/CVUT_znak.svg"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}