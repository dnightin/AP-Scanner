# AP Scanner

AP Scanner is a simple Android Wi-Fi survey app that scans nearby access points and shows the details that matter when checking RF coverage.

## Features

- Lists nearby SSIDs sorted by strongest signal
- Shows RF signal strength in dBm
- Shows channel, frequency, and Wi-Fi band
- Groups scan totals by 2.4 GHz, 5 GHz, and 6 GHz
- Highlights signal quality with labels and signal bars
- Displays BSSID and security capabilities for each access point

## Requirements

- Android Studio
- Android SDK with API 36 installed
- Android phone connected over USB with USB debugging enabled
- Wi-Fi and Location enabled on the phone

## Permissions

Android requires Wi-Fi scan apps to request location-related permissions because scan results can reveal physical location. The app requests:

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_STATE`
- `NEARBY_WIFI_DEVICES` on Android 13 and newer

Grant the location prompt when the app launches so SSID scan results can be displayed.

## Build And Run

Open this folder in Android Studio, wait for Gradle sync, then choose your connected phone and press Run.

You can also build from PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
gradle assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

Android may throttle repeated Wi-Fi scans. If a fresh scan is throttled, the app shows cached scan results and marks them as cached.
