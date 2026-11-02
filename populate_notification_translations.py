import os
import glob
import re

res_dir = "/app/src/main/res"

new_strings = """
    <!-- Push Notifications & Activity Window Alerts -->
    <string name="notification_channel_name">Outdoor Window Alerts</string>
    <string name="notification_channel_desc">High priority alerts when outdoor activity windows open</string>
    <string name="notification_settings_title">OUTDOOR WINDOW PUSH ALERTS</string>
    <string name="notification_enable_toggle">Enable Activity Window Notifications</string>
    <string name="notification_threshold_label">Minimum Score Threshold: %1$d/100 (GO Status)</string>
    <string name="notification_favs_only_toggle">Alert for Favorite Locations Only</string>
    <string name="notification_test_button">TEST PUSH NOTIFICATION</string>
    <string name="notification_test_sent_toast">Test notification sent!</string>
    <string name="notification_permission_required">Push notification permission required on Android 13+ to receive window alerts.</string>
    <string name="notification_permission_grant_btn">GRANT NOTIFICATION PERMISSION</string>
    <string name="notification_window_title">🌿 Outdoor Window Open: %1$s</string>
    <string name="notification_window_body">Score reached %1$d/100 (%2$s). %3$s</string>
    <string name="notification_test_title">🔔 AeroPulse Alerts Configured</string>
    <string name="notification_test_body">Push notifications active! You will receive alerts when Go/No-Go conditions open for your saved locations.</string>
    <string name="cd_notifications">Notification Settings</string>
"""

values_dirs = glob.glob(os.path.join(res_dir, "values-*"))
for vdir in values_dirs:
    strings_path = os.path.join(vdir, "strings.xml")
    if os.path.exists(strings_path):
        with open(strings_path, "r", encoding="utf-8") as f:
            content = f.read()
        
        if "notification_channel_name" not in content:
            content = content.replace("</resources>", new_strings.strip() + "\n</resources>")
            with open(strings_path, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"Updated {strings_path}")
