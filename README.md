# iNGenDNS DNS Optimizer

iNGenDNS DNS Optimizer is an Android 8.0+ Kotlin application that benchmarks DNS resolvers and identifies reliable, low-latency options for the active network.

## Current capabilities

- Immediate Wi-Fi, cellular, and validated-internet detection using `ConnectivityManager.NetworkCallback`.
- UDP DNS wire-format benchmarking for built-in and custom DNS profiles.
- Ten samples per resolver with latency, reliability, success rate, jitter, and packet-loss measurements.
- DNS Health Score: latency 40%, reliability 25%, success 20%, jitter 10%, and packet loss 5%.
- Automatic selection includes managed and custom profiles, requires a score of 70 or higher, and falls back to network DNS when no resolver qualifies.
- Persisted, validated custom IPv4/IPv6 resolvers.
- Latest benchmark persisted locally and restored when the app opens.
- Background benchmark scheduled with WorkManager every six hours, only on a connected network.
- A network-constrained health check runs every 30 minutes and logs DNS plus HTTPS reachability.
- Material 3 Compose dashboard, results, and settings UI.

## VPN behavior

Android does not permit normal apps to modify the system cellular DNS directly. iNGenDNS therefore uses a local, DNS-only `VpnService` after the user grants Android's VPN permission. It forwards IPv4 UDP DNS requests to the selected resolver and leaves non-DNS traffic on the underlying network.

On cellular networks, the tunnel uses the selected encrypted DNS resolver. On Wi-Fi, it stays active and forwards DNS to the DNS servers supplied by that Wi-Fi network. This hybrid behavior keeps Android's Always-on VPN active while supporting networks, such as managed office Wi-Fi, that reject external DNS resolvers.

When Android's **Block connections without VPN** option prevents connectivity during tunnel reconfiguration, the dashboard explains the condition and provides a shortcut to Android's VPN settings. Explicitly stopping DNS optimization stops the service unless Android has configured iNGenDNS as the Always-on VPN.

The forwarder currently supports IPv4 UDP DNS. TCP fallback and IPv6 DNS forwarding require additional implementation and device testing before a production release.

## Build

1. Open the project in Android Studio.
2. Install Android SDK Platform 36 and Build Tools 36.0.0.
3. Sync Gradle.
4. Run `gradlew.bat :app:assembleDebug` from Windows PowerShell, or use **Run** in Android Studio.

Run local tests with:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Build the Google Play variant with:

```powershell
.\gradlew.bat :app:assemblePlayDebug
```

Build the FLOSS-only F-Droid variant with:

```powershell
.\gradlew.bat :app:assembleFdroidRelease
```

The F-Droid variant uses the application ID `com.ingendns.app.fdroid` and
excludes the Google Play in-app update dependency.

## Release

Release builds have R8 optimization enabled. A release bundle must be signed with the developer’s own private keystore; this repository does not and should not contain signing credentials.

```powershell
.\gradlew.bat :app:bundleRelease
```

Before publishing, complete physical-device transition and failure testing, privacy disclosures, the foreground-service special-use policy review, and Play Console’s current data-safety declarations.

### Signed GitHub releases

The `Release signed F-Droid APK` GitHub Actions workflow builds, verifies, and
publishes a developer-signed APK whenever a tag matching `v*-fdroid` is pushed.
Configure these encrypted repository secrets before running it:

- `ANDROID_KEYSTORE_BASE64`: the release keystore encoded as one-line Base64
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password
- `ANDROID_KEY_ALIAS`: the permanent signing-key alias
- `ANDROID_KEY_PASSWORD`: the signing-key password

Keep the original keystore and passwords in secure offline backups. Losing the
key prevents Android from accepting future updates. Never commit a keystore or
password to this repository.

Each release publishes the signed APK and a SHA-256 checksum to GitHub Releases.
These assets can be consumed by direct-download catalogs such as OpenAPK and by
update clients such as Obtainium.

The permanent public download link is:

https://github.com/pkmdinesh/iNGenDNS/releases/latest

## License

Copyright (C) 2026 Dinesh K.

iNGenDNS is licensed under the GNU General Public License, version 3 only.
See [LICENSE](LICENSE).
