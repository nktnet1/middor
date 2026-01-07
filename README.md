# Middor

Middor is a free and open-source Android application for mirroring apps on your
device through an overlay, with optional horizontal flip and 180° rotation.

It can be used for

- displaying apps on a HUD
- projecting content onto a car windshield (e.g. Google Maps navigation)
- viewing mirrored images and videos

Middor only supports devices running on Android 14+ (SDK 34+), as it relies on
the single app screen sharing feature. For more details, see
[Android 14 - App Screen Sharing](https://developer.android.com/about/versions/14/features/app-screen-sharing).

## Permissions

- **SYSTEM_ALERT_WINDOW**: to draw a mirror overlay on top of other apps
- **FOREGROUND_SERVICE**: to run the mirror service continuously while the overlay is active
- **FOREGROUND_SERVICE_MEDIA_PROJECTION**: for capturing the screen content of the underlying app
- **POST_NOTIFICATIONS**: (optional) to display the mirror service notification with additional actions

## Contact

- support@middor.nktnet.org

## License

This project is licensed under the GNU Affero General Public License v3.0 or later.

See the [LICENSE](./LICENSE) file for details.
