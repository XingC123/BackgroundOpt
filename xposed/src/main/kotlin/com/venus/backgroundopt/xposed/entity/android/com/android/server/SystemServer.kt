/*
 * Copyright (C) 3-4 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.venus.backgroundopt.xposed.entity.android.com.android.server

/**
 * @author XingC
 * @date 4/8/26
 */
class SystemServer {
    companion object {
        /*
         * Implementation class names. TODO: Move them to a codegen class or load
         * them from the build system somehow.
         */
        const val BACKUP_MANAGER_SERVICE_CLASS =
            "com.android.server.backup.BackupManagerService\$Lifecycle"
        const val APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService"
        const val VOICE_RECOGNITION_MANAGER_SERVICE_CLASS =
            "com.android.server.voiceinteraction.VoiceInteractionManagerService"
        const val APP_HIBERNATION_SERVICE_CLASS =
            "com.android.server.apphibernation.AppHibernationService"
        const val PRINT_MANAGER_SERVICE_CLASS = "com.android.server.print.PrintManagerService"
        const val COMPANION_DEVICE_MANAGER_SERVICE_CLASS =
            "com.android.server.companion.CompanionDeviceManagerService"
        const val VIRTUAL_DEVICE_MANAGER_SERVICE_CLASS =
            "com.android.server.companion.virtual.VirtualDeviceManagerService"
        const val STATS_COMPANION_APEX_PATH =
            "/apex/com.android.os.statsd/javalib/service-statsd.jar"
        const val SCHEDULING_APEX_PATH =
            "/apex/com.android.scheduling/javalib/service-scheduling.jar"
        const val REBOOT_READINESS_LIFECYCLE_CLASS =
            "com.android.server.scheduling.RebootReadinessManagerService\$Lifecycle"
        const val CONNECTIVITY_SERVICE_APEX_PATH =
            "/apex/com.android.tethering/javalib/service-connectivity.jar"
        const val STATS_COMPANION_LIFECYCLE_CLASS =
            "com.android.server.stats.StatsCompanion\$Lifecycle"
        const val STATS_PULL_ATOM_SERVICE_CLASS =
            "com.android.server.stats.pull.StatsPullAtomService"
        const val STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS =
            "com.android.server.stats.bootstrap.StatsBootstrapAtomService\$Lifecycle"
        const val USB_SERVICE_CLASS = "com.android.server.usb.UsbService\$Lifecycle"
        const val MIDI_SERVICE_CLASS = "com.android.server.midi.MidiService\$Lifecycle"
        const val WIFI_APEX_SERVICE_JAR_PATH = "/apex/com.android.wifi/javalib/service-wifi.jar"
        const val WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService"
        const val WIFI_SCANNING_SERVICE_CLASS =
            "com.android.server.wifi.scanner.WifiScanningService"
        const val WIFI_RTT_SERVICE_CLASS = "com.android.server.wifi.rtt.RttService"
        const val WIFI_AWARE_SERVICE_CLASS = "com.android.server.wifi.aware.WifiAwareService"
        const val WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService"
        const val LOWPAN_SERVICE_CLASS = "com.android.server.lowpan.LowpanService"
        const val JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService"
        const val LOCK_SETTINGS_SERVICE_CLASS =
            "com.android.server.locksettings.LockSettingsService\$Lifecycle"
        const val RESOURCE_ECONOMY_SERVICE_CLASS = "com.android.server.tare.InternalResourceService"
        const val STORAGE_MANAGER_SERVICE_CLASS =
            "com.android.server.StorageManagerService\$Lifecycle"
        const val STORAGE_STATS_SERVICE_CLASS =
            "com.android.server.usage.StorageStatsService\$Lifecycle"
        const val SEARCH_MANAGER_SERVICE_CLASS =
            "com.android.server.search.SearchManagerService\$Lifecycle"
        const val THERMAL_OBSERVER_CLASS = "com.android.clockwork.ThermalObserver"
        const val WEAR_CONNECTIVITY_SERVICE_CLASS =
            "com.android.clockwork.connectivity.WearConnectivityService"
        const val WEAR_POWER_SERVICE_CLASS = "com.android.clockwork.power.WearPowerService"
        const val HEALTH_SERVICE_CLASS = "com.android.clockwork.healthservices.HealthService"
        const val WEAR_SIDEKICK_SERVICE_CLASS =
            "com.google.android.clockwork.sidekick.SidekickService"
        const val WEAR_DISPLAYOFFLOAD_SERVICE_CLASS =
            "com.android.clockwork.displayoffload.DisplayOffloadService"
        const val WEAR_DISPLAY_SERVICE_CLASS = "com.android.clockwork.display.WearDisplayService"
        const val WEAR_TIME_SERVICE_CLASS = "com.android.clockwork.time.WearTimeService"
        const val WEAR_GLOBAL_ACTIONS_SERVICE_CLASS =
            "com.android.clockwork.globalactions.GlobalActionsService"
        const val ACCOUNT_SERVICE_CLASS =
            "com.android.server.accounts.AccountManagerService\$Lifecycle"
        const val CONTENT_SERVICE_CLASS = "com.android.server.content.ContentService\$Lifecycle"
        const val WALLPAPER_SERVICE_CLASS =
            "com.android.server.wallpaper.WallpaperManagerService\$Lifecycle"
        const val AUTO_FILL_MANAGER_SERVICE_CLASS =
            "com.android.server.autofill.AutofillManagerService"
        const val CREDENTIAL_MANAGER_SERVICE_CLASS =
            "com.android.server.credentials.CredentialManagerService"
        const val CONTENT_CAPTURE_MANAGER_SERVICE_CLASS =
            "com.android.server.contentcapture.ContentCaptureManagerService"
        const val TRANSLATION_MANAGER_SERVICE_CLASS =
            "com.android.server.translation.TranslationManagerService"
        const val SELECTION_TOOLBAR_MANAGER_SERVICE_CLASS =
            "com.android.server.selectiontoolbar.SelectionToolbarManagerService"
        const val MUSIC_RECOGNITION_MANAGER_SERVICE_CLASS =
            "com.android.server.musicrecognition.MusicRecognitionManagerService"
        const val SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS =
            "com.android.server.systemcaptions.SystemCaptionsManagerService"
        const val TEXT_TO_SPEECH_MANAGER_SERVICE_CLASS =
            "com.android.server.texttospeech.TextToSpeechManagerService"
        const val IOT_SERVICE_CLASS = "com.android.things.server.IoTSystemService"
        const val SLICE_MANAGER_SERVICE_CLASS =
            "com.android.server.slice.SliceManagerService\$Lifecycle"
        const val CAR_SERVICE_HELPER_SERVICE_CLASS =
            "com.android.internal.car.CarServiceHelperService"
        const val TIME_DETECTOR_SERVICE_CLASS =
            "com.android.server.timedetector.TimeDetectorService\$Lifecycle"
        const val TIME_ZONE_DETECTOR_SERVICE_CLASS =
            "com.android.server.timezonedetector.TimeZoneDetectorService\$Lifecycle"
        const val LOCATION_TIME_ZONE_MANAGER_SERVICE_CLASS =
            "com.android.server.timezonedetector.location.LocationTimeZoneManagerService\$Lifecycle"
        const val GNSS_TIME_UPDATE_SERVICE_CLASS =
            "com.android.server.timedetector.GnssTimeUpdateService\$Lifecycle"
        const val ACCESSIBILITY_MANAGER_SERVICE_CLASS =
            "com.android.server.accessibility.AccessibilityManagerService\$Lifecycle"
        const val ADB_SERVICE_CLASS = "com.android.server.adb.AdbService\$Lifecycle"
        const val SPEECH_RECOGNITION_MANAGER_SERVICE_CLASS =
            "com.android.server.speech.SpeechRecognitionManagerService"
        const val WALLPAPER_EFFECTS_GENERATION_MANAGER_SERVICE_CLASS =
            "com.android.server.wallpapereffectsgeneration.WallpaperEffectsGenerationManagerService"
        const val APP_PREDICTION_MANAGER_SERVICE_CLASS =
            "com.android.server.appprediction.AppPredictionManagerService"
        const val CONTENT_SUGGESTIONS_SERVICE_CLASS =
            "com.android.server.contentsuggestions.ContentSuggestionsManagerService"
        const val SEARCH_UI_MANAGER_SERVICE_CLASS =
            "com.android.server.searchui.SearchUiManagerService"
        const val SMARTSPACE_MANAGER_SERVICE_CLASS =
            "com.android.server.smartspace.SmartspaceManagerService"
        const val DEVICE_IDLE_CONTROLLER_CLASS = "com.android.server.DeviceIdleController"
        const val BLOB_STORE_MANAGER_SERVICE_CLASS =
            "com.android.server.blob.BlobStoreManagerService"
        const val APPSEARCH_MODULE_LIFECYCLE_CLASS =
            "com.android.server.appsearch.AppSearchModule\$Lifecycle"
        const val ISOLATED_COMPILATION_SERVICE_CLASS =
            "com.android.server.compos.IsolatedCompilationService"
        const val ROLLBACK_MANAGER_SERVICE_CLASS =
            "com.android.server.rollback.RollbackManagerService"
        const val ALARM_MANAGER_SERVICE_CLASS = "com.android.server.alarm.AlarmManagerService"
        const val MEDIA_SESSION_SERVICE_CLASS = "com.android.server.media.MediaSessionService"
        const val MEDIA_RESOURCE_MONITOR_SERVICE_CLASS =
            "com.android.server.media.MediaResourceMonitorService"
        const val CONNECTIVITY_SERVICE_INITIALIZER_CLASS =
            "com.android.server.ConnectivityServiceInitializer"
        const val NETWORK_STATS_SERVICE_INITIALIZER_CLASS =
            "com.android.server.NetworkStatsServiceInitializer"
        const val IP_CONNECTIVITY_METRICS_CLASS =
            "com.android.server.connectivity.IpConnectivityMetrics"
        const val MEDIA_COMMUNICATION_SERVICE_CLASS =
            "com.android.server.media.MediaCommunicationService"
        const val APP_COMPAT_OVERRIDES_SERVICE_CLASS =
            "com.android.server.compat.overrides.AppCompatOverridesService\$Lifecycle"
        const val HEALTHCONNECT_MANAGER_SERVICE_CLASS =
            "com.android.server.healthconnect.HealthConnectManagerService"
        const val ROLE_SERVICE_CLASS = "com.android.role.RoleService"
        const val GAME_MANAGER_SERVICE_CLASS =
            "com.android.server.app.GameManagerService\$Lifecycle"
        const val UWB_APEX_SERVICE_JAR_PATH = "/apex/com.android.uwb/javalib/service-uwb.jar"
        const val UWB_SERVICE_CLASS = "com.android.server.uwb.UwbService"
        const val BLUETOOTH_APEX_SERVICE_JAR_PATH =
            "/apex/com.android.btservices/javalib/service-bluetooth.jar"
        const val BLUETOOTH_SERVICE_CLASS = "com.android.server.bluetooth.BluetoothService"
        const val SAFETY_CENTER_SERVICE_CLASS = "com.android.safetycenter.SafetyCenterService"

        const val SDK_SANDBOX_MANAGER_SERVICE_CLASS =
            "com.android.server.sdksandbox.SdkSandboxManagerService\$Lifecycle"
        const val AD_SERVICES_MANAGER_SERVICE_CLASS =
            "com.android.server.adservices.AdServicesManagerService\$Lifecycle"
        const val ON_DEVICE_PERSONALIZATION_SYSTEM_SERVICE_CLASS =
            "com.android.server.ondevicepersonalization." + "OnDevicePersonalizationSystemService\$Lifecycle"
        const val UPDATABLE_DEVICE_CONFIG_SERVICE_CLASS =
            "com.android.server.deviceconfig.DeviceConfigInit\$Lifecycle"

        const val TETHERING_CONNECTOR_CLASS = "android.net.ITetheringConnector"
    }
}