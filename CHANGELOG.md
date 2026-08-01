# Changelog

## 12.2 - 2026-08-01

- Removed automatic daily update checks; updates are now checked only when the user selects **Check for updates** in About.
- Reduced per-query allocations in the VPN DNS forwarding path by processing reusable packet-buffer slices directly.
- Improved DoH connection reuse by keeping successful HTTPS connections eligible for Android's connection pool.
- Removed unnecessary per-response stream flushing from the VPN forwarding loop.
- Kept existing DoH/DoT timeouts and resolver failover behavior unchanged.
- Reduced the adaptive launcher foreground to Android's safe zone so the logo fits normal and round launcher masks.

## 12.1 - 2026-07-30

- Added hybrid Always-on VPN behavior: encrypted DNS on cellular and Wi-Fi-provided DNS on Wi-Fi.
- Added clear VPN mode and connection status to the dashboard.
- Added a warning and VPN Settings shortcut when Android's VPN lockdown blocks connectivity during tunnel reconfiguration.
- Improved VPN authorization and service discoverability.
- Reduced DoH and DoT failure detection time for faster resolver rollover.
- Fixed the Wi-Fi connection label displaying `Encrypted (null)`.
- Disabled **Stop DNS** while Android Always-on VPN controls the service.
- Removed the duplicate **Active DNS** value from the dashboard.
- Replaced internal History session UUIDs with readable timestamps.
- Fixed Start DNS and Auto Connect becoming unresponsive after the Android VPN profile is deleted.
- Fixed the mobile-data provider label not updating after switching the default-data SIM.
- Compacted saved DNS profile cards by placing the IP address and profile actions on one row.
- Moved custom DNS profiles to the top of the Saved Profiles list.
- Added a daily GitHub release update prompt with an Update button for F-Droid builds; the check resets at local midnight.
- Updated About to display the app version without its distribution suffix.
- Added a manual Check for updates button to About.
- Changed the Dashboard DoH battery note to orange for clearer visibility.
- Refined Dashboard protocol and Auto Connect controls, added compact status chips and improved dark-theme success contrast.
- Added confirmation before removing a custom DNS profile.
- Adjusted the adaptive launcher icon safe area so the logo is not cropped by device icon masks.
- Added the app logo beside the iNGenDNS heading in About and replaced the text menu symbol with a proper accessible icon.
- Compacted DNS Analytics with grouped measurements and progress indicators.
- Changed FAQs to expandable question rows and added compact date/time and status chips to History.
- Refined the app logo with reduced white space, subtle rounded edges, and improved launcher sizing.

## 10.1 - 2026-07-22

- Initial public release of iNGenDNS.
- Added DNS benchmarking, health scoring, managed and custom profiles, automatic selection, local DNS-only VPN operation, history, settings, and FAQs.
