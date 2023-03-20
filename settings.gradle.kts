include(":example-app")
include(":famoco-plugin")
rootProject.name = "keyple-plugin-se-communication-lib"

// Fix resolution of dependencies with dynamic version in order to use SNAPSHOT first when available.
// See explanation here : https://docs.gradle.org/6.8.3/userguide/single_versions.html
enableFeaturePreview("VERSION_ORDERING_V2")