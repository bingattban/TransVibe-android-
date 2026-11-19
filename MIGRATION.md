# Migration & Update Compatibility

- Production `applicationId`: `com.transvibe.app`
- New `versionCode`: `6` (strictly greater than legacy `5`)
- Room Database: Version `2` with `MIGRATION_1_2` adding `freeze_records` without dropping existing tables.
- **Signing Warning**: In-place upgrade over a previously installed TransVibe APK requires signing with the **exact same owner keystore/certificate lineage**. Never uninstall as a default fix, as uninstall deletes app-private models and Room history.
- Preview variant uses `com.transvibe.app.preview` for safe side-by-side testing.
