# GeoCamOff - Location-Based Camera Security System

<div align="center">

**An advanced Android security application that monitors and controls camera access based on geographic location with automated security alerting.**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

</div>

App Preview Video Link:  
<div align="center">
   https://github.com/user-attachments/assets/8d67fedc-09fb-4b67-ae66-73e7bdadf900
</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Security Features](#-security-features)
  - [Security Notifications](#security-notifications)
  - [Security Configuration](#security-configuration)
- [Installation](#-installation)
- [Permissions](#-permissions)
- [Usage Guide](#-usage-guide)


---

## 🎯 Overview

**GeoCamOff** is a sophisticated Android security application designed for controlled environments where camera usage must be restricted based on geographic location. The app provides real-time monitoring, automatic camera blocking in restricted zones, and comprehensive security alerting through multiple channels.

### Use Cases

- **Defense & Military Installations**: Prevent unauthorized photography in sensitive areas
- **Corporate Facilities**: Protect intellectual property and sensitive information
- **Research Laboratories**: Maintain security protocols in classified research areas
- **Government Buildings**: Enforce photography restrictions in secure zones
- **Manufacturing Plants**: Protect proprietary processes and equipment

---

## ✨ Key Features

### 🌍 Geofencing Technology
- **Polygon-based restricted zones** with precise boundary detection
- **Real-time location tracking** with GPS and network providers
- **Custom geofence definitions** via JSON configuration
- **Multiple zone support** with individual zone identification
- **Background location monitoring** for continuous protection

### 📷 Camera Access Control
- **Automatic camera blocking** when entering restricted zones
- **Full-screen overlay** prevents camera app usage
- **Foreground and background detection** using CameraManager API
- **Accessibility service integration** for comprehensive app monitoring
- **Multi-app camera detection** (native camera, third-party apps, etc.)

### 🔐 Security Alert System
- **Multi-channel notifications**: SMS, Email, and Local Notifications
- **Automated security alerts** when camera is accessed in restricted zones
- **Detailed alert information**: Timestamp, location, device info, and zone status
- **File-based configuration** for administrative control
- **Audit logging** for compliance and investigation

### 🎨 User Interface
- **Status Dashboard**: Real-time monitoring of camera and location status
- **Settings Panel**: Configure security preferences and test alerts
- **Visual indicators**: Clear status displays for location and camera state
- **Permission management**: Guided permission request workflow

### 🔄 Background Services
- **Persistent monitoring** with foreground services
- **Boot-on-startup** automatic service initialization
- **Battery optimized** efficient background processing
- **Crash recovery** automatic service restart mechanisms

---

## 🏗 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     MainActivity (UI Layer)                  │
│  ┌──────────────┐              ┌───────────────┐           │
│  │   Status     │              │   Settings    │           │
│  │  Fragment    │              │   Fragment    │           │
│  └──────────────┘              └───────────────┘           │
└─────────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
┌───────────▼────┐  ┌──────▼──────┐  ┌─────▼─────────┐
│  StateManager  │  │   Security  │  │   Restricted  │
│   (Central)    │  │   Config    │  │     Area      │
│   Controller   │  │   Manager   │  │    Loader     │
└────────┬───────┘  └──────┬──────┘  └───────┬───────┘
         │                 │                  │
    ┌────┴────┬───────────┴─────┬────────────┘
    │         │                 │
┌───▼─────────▼──┐   ┌──────────▼──────────┐
│  Camera         │   │   Location          │
│  Detection      │   │   Monitoring        │
│  Service        │   │   Service           │
└────────┬────────┘   └──────────┬──────────┘
         │                       │
         ├───────────┬───────────┤
         │           │           │
┌────────▼─────┐ ┌──▼─────┐ ┌───▼──────────┐
│   Overlay    │ │Security│ │  Geofence    │
│   Service    │ │Notif.  │ │  Broadcast   │
│   (Blocker)  │ │Service │ │  Receiver    │
└──────────────┘ └────────┘ └──────────────┘
```

### Core Components

#### 1. **MainActivity** (`MainActivity.kt`)
- Entry point and UI coordinator
- Permission request management
- Fragment navigation (Status/Settings)
- Foreground camera detection
- Service lifecycle management

#### 2. **StateManager** (`StateManager.kt`)
- Central state controller
- Camera state tracking
- Location/restriction state management
- Overlay service coordination
- Security notification triggering

#### 3. **CameraDetectionService** (`CameraDetectionService.kt`)
- Background camera monitoring
- CameraManager callback registration
- Camera availability tracking
- Service persistence management

#### 4. **LocationMonitoringService** (`LocationMonitoringService.kt`)
- Continuous GPS/network location tracking
- Geofence boundary detection
- Location update processing
- Restricted zone identification

#### 5. **OverlayService** (`OverlayService.kt`)
- Full-screen blocking overlay
- Camera access prevention
- User notification display
- System-level window management

#### 6. **SecurityNotificationService** (`SecurityNotificationService.kt`)
- Multi-channel alert delivery (SMS/Email/Local)
- Security contact management
- Alert content formatting
- Audit log maintenance

#### 7. **GeoCamAccessibilityService** (`GeoCamAccessibilityService.kt`)
- App launch detection
- Camera app identification
- System-wide monitoring
- Enhanced detection capability

---

## 🔐 Security Features

### Security Notifications

The app includes a comprehensive **automated security alert system** that sends real-time notifications when camera access is detected, particularly in restricted zones.

#### Alert Channels

1. **SMS Alerts**
   - Instant text messages to security personnel
   - Includes timestamp, location, and device info
   - Requires `SEND_SMS` permission

2. **Local Notifications**
   - On-device push notifications
   - Immediate visual alerts
   - Persistent notification channel


#### Alert Content Example

```
🚨 SECURITY ALERT 🚨

Camera accessed at: 2025-10-19 14:30:15

Device Information:
Device: Pixel 6 (Google)
Android: 14
App State: Background
Screen: Locked

Location Details:
User is in a RESTRICTED ZONE
Zone ID: BDL_01 (BDL Kanchanbagh)
GPS Coordinates: 17.3346, 78.5078
This is an automated security notification from GeoCamOff monitoring system.
```

#### Features

✅ **Automatic triggering** when camera becomes active  
✅ **Multiple notification channels** ensure delivery  
✅ **Rich contextual information** for security teams  
✅ **Audit logging** for compliance and investigation  
✅ **Configurable contacts** via file-based system  

### Security Configuration

The security system uses **file-based configuration** for administrative control, eliminating user-facing settings that could be tampered with.

#### Configuration File

**Location**: `/data/data/com.example.geocamoff/files/security_config.properties`

**Format**:
```properties
# Security contact phone number (international format)
security_phone=+1234567890

# Security contact email address
security_email=security@company.com

# Enable/disable notifications (true/false)
notifications_enabled=true
```

#### Default Configuration

If the configuration file doesn't exist, it will be created automatically with default values:
- **Phone**: `+1234567890`
- **Email**: `security@company.com`
- **Notifications**: `enabled`

#### Security Benefits

🔒 **No user-accessible UI** - Settings hidden from end users  
🔒 **Administrative control** - Only authorized personnel can modify  
🔒 **Persistent storage** - Survives app updates  
🔒 **Private file location** - Inaccessible to other apps  
🔒 **Audit trail** - Configuration changes logged  

---

## 📦 Installation

### Prerequisites

- **Android Studio**: Arctic Fox or later
- **Android SDK**: API 24 (Android 7.0) or higher
- **Gradle**: 8.0 or higher
- **Kotlin**: 1.9.0 or higher

### Build Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/sv-reddy/GeoCamOff.git
   cd GeoCamOff
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle
   - Wait for dependency download to complete

4. **Configure restricted areas**
   - Edit `app/src/main/assets/restricted_areas.json`
   - Define your geofence polygons (see [Configuration](#-configuration))

5. **Configure security contacts** (Optional)
   - Default values will be used if not configured
   - Can be updated programmatically after installation

6. **Build the APK**
   ```bash
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`

7. **Install on device**
   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

---

## 🔑 Permissions

GeoCamOff requires several permissions for full functionality. All permissions are requested at runtime with clear explanations.

### Required Permissions

| Permission | Purpose | Criticality |
|------------|---------|-------------|
| `CAMERA` | Detect camera usage | **Essential** |
| `ACCESS_FINE_LOCATION` | GPS location tracking | **Essential** |
| `ACCESS_COARSE_LOCATION` | Network location | **Essential** |
| `ACCESS_BACKGROUND_LOCATION` | Monitor when app is closed | **Essential** |
| `SYSTEM_ALERT_WINDOW` | Display blocking overlay | **Essential** |
| `FOREGROUND_SERVICE` | Background service operation | **Essential** |
| `FOREGROUND_SERVICE_CAMERA` | Camera monitoring service | **Essential** |
| `FOREGROUND_SERVICE_LOCATION` | Location tracking service | **Essential** |
| `POST_NOTIFICATIONS` | Local notifications | **Important** |
| `SEND_SMS` | Security SMS alerts | **Important** |
| `READ_PHONE_STATE` | Device information | Optional |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on boot | Optional |
| `BIND_ACCESSIBILITY_SERVICE` | Enhanced app monitoring | Optional |

### Permission Request Flow

1. **Initial Launch**: Permission dialog with explanation
2. **Grant Permissions**: User approves required permissions
3. **Overlay Permission**: System settings for draw overlay
4. **Accessibility Service**: Optional but recommended
5. **Background Location**: Requested after foreground location

---

#### Configuration Rules

- **Polygon format**: Array of latitude/longitude coordinates
- **Minimum points**: 3 coordinates (triangle)
- **Coordinate order**: Clockwise or counter-clockwise
- **Unique IDs**: Each zone must have a unique identifier
- **Descriptive names**: Clear zone identification

### Security Configuration

#### Programmatic (Recommended)

```kotlin
class SecuritySetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure security settings
        SecurityConfigManager.updateSecuritySettings(
            context = this,
            phone = "+1-800-SECURITY",
            email = "alerts@yourcompany.com",
            enabled = true
        )
    }
}
```

## 📱 Usage Guide

### First Launch

1. **Grant Permissions**
   - Camera access
   - Location access (foreground)
   - Notification access
   - SMS access (for security alerts)

2. **Enable Overlay Permission**
   - Navigate to system settings
   - Grant "Draw over other apps" permission

3. **Enable Accessibility Service** (Optional but recommended)
   - Open Accessibility settings
   - Find "GeoCamOff" service
   - Enable the service

4. **Grant Background Location**
   - Prompted after initial setup
   - Select "Allow all the time"

### Status Tab

Monitor real-time status information:

- **Camera Status**: Active/Inactive indicator
- **Location Status**: Current GPS coordinates
- **Restriction Status**: Inside/Outside restricted zone
- **Current Zone**: Active geofence name (if any)
- **Service Status**: Background service indicators
- **Last Updated**: Timestamp of last status update

### Settings Tab

Configure application behavior:

- **Location Updates**: GPS refresh frequency
- **Notification Settings**: Alert preferences
- **Battery Optimization**: Power management settings
- **Debug Options**: Advanced diagnostic tools
- **About**: App version and information


## 🔒 Security & Privacy

### Data Collection

GeoCamOff collects the following data **locally only**:

- **Location Data**: GPS coordinates for geofence detection
- **Camera Status**: Whether camera is active/inactive
- **Device Information**: Model, manufacturer, Android version
- **Security Events**: Audit logs of camera access in restricted zones
- **Configuration**: Security contact information

### Data Storage

- **Local Storage**: All data stored in app's private directory
- **No Cloud Sync**: No data transmitted to external servers
- **Encrypted Storage**: Sensitive data encrypted at rest (Android Keystore)
- **Access Control**: Only app can access stored data

### Privacy Considerations

⚠️ **User Awareness**: Users should be informed about monitoring  
⚠️ **Consent**: Obtain appropriate consent for location/camera monitoring  
⚠️ **Compliance**: Ensure compliance with local privacy laws (GDPR, CCPA, etc.)  
⚠️ **Data Retention**: Implement appropriate data retention policies  
⚠️ **Access Logs**: Maintain logs of who accesses configuration  

### Security Best Practices

1. **Change Default Contacts**: Update security phone/email immediately
2. **Secure Configuration**: Protect access to configuration files
3. **Regular Audits**: Review security event logs regularly
4. **Update Regularly**: Keep app updated with latest security patches
5. **Test Thoroughly**: Regularly test all security features
6. **Incident Response**: Have procedures for security alert responses

---

## 👨‍💻 Development

### Project Structure

```
GeoCamOff/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/geocamoff/
│   │   │   │   ├── MainActivity.kt                    # Main UI
│   │   │   │   ├── StateManager.kt                    # State controller
│   │   │   │   ├── CameraDetectionService.kt          # Camera monitoring
│   │   │   │   ├── LocationMonitoringService.kt       # GPS tracking
│   │   │   │   ├── OverlayService.kt                  # Camera blocker
│   │   │   │   ├── SecurityNotificationService.kt     # Security alerts
│   │   │   │   ├── SecurityConfigManager.kt           # Config management
│   │   │   │   ├── GeoCamAccessibilityService.kt      # App monitoring
│   │   │   │   ├── PolygonGeofenceUtils.kt            # Geofence math
│   │   │   │   ├── RestrictedAreaLoader.kt            # JSON parser
│   │   │   │   ├── StatusFragment.kt                  # Status UI
│   │   │   │   ├── SettingsFragment.kt                # Settings UI
│   │   │   │   └── ... (other components)
│   │   │   ├── assets/
│   │   │   │   └── restricted_areas.json              # Geofence data
│   │   │   ├── res/
│   │   │   │   ├── layout/                            # UI layouts
│   │   │   │   ├── values/                            # Strings, colors
│   │   │   │   └── xml/                               # Configs
│   │   │   └── AndroidManifest.xml                    # App manifest
│   │   └── test/                                      # Unit tests
│   └── build.gradle.kts                               # App dependencies
├── gradle/                                            # Gradle configs
├── SECURITY_CONFIG.md                                 # Config documentation
├── SECURITY_NOTIFICATIONS.md                          # Notification docs
├── DEBUG_GUIDE.md                                     # Debug guide
├── README.md                                          # This file
└── build.gradle.kts                                   # Project build
```

## 🙏 Acknowledgments

- **BDL (Bharat Dynamics Limited)** - Project sponsorship and requirements
- **Android Open Source Project** - Framework and APIs
- **Google Play Services** - Location services

---
