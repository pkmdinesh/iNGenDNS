# Changelog

## 11.1 - 2026-07-29

- Added hybrid Always-on VPN behavior: encrypted DNS on cellular and Wi-Fi-provided DNS on Wi-Fi.
- Added clear VPN mode and connection status to the dashboard.
- Added a warning and VPN Settings shortcut when Android's VPN lockdown blocks connectivity during tunnel reconfiguration.
- Improved VPN authorization and service discoverability.
- Reduced DoH and DoT failure detection time for faster resolver rollover.
- Fixed the Wi-Fi connection label displaying `Encrypted (null)`.
- Disabled **Stop DNS** while Android Always-on VPN controls the service.
- Removed the duplicate **Active DNS** value from the dashboard.
- Replaced internal History session UUIDs with readable timestamps.

## 10.1 - 2026-07-22

- Initial public release of iNGenDNS.
- Added DNS benchmarking, health scoring, managed and custom profiles, automatic selection, local DNS-only VPN operation, history, settings, and FAQs.
