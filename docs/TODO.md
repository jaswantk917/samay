# Samay: owner's asks

Every ask from the owner, so none get dropped. `[x]` = built and checked on the Pixel, `[~]` = built, not yet seen on the phone, `[ ]` = not built.

## Done earlier
- [x] Header stays fixed (no hiding on scroll)
- [x] Timer winder ticks like a texture (soft per mark, firmer on round values)
- [x] Winder is logarithmic and starts at 5 s
- [x] One settings sheet from every tab, in Tring's style; Privacy and Permissions removed
- [x] World Clock can be hidden and is off by default
- [x] Swipe between tabs; dragging the winder doesn't change tabs
- [x] Samay's own palette (was "Concept") is the default
- [~] Haptics on every control
- [~] Rolling timer digits no longer crop
- [~] Widget picker shows the real face (generated preview)
- [~] Tapping the widget grows the app out of the face, no ghost rectangle
- [~] Timer and stopwatch are Live Updates with a status-bar chip (MetricStyle on Android 17)

## In progress
- [~] Alarm grid: pairs split 2/3 + 1/3 when one tile carries more; a lone alarm is a wide card (clock left, switch top-right, days bottom-right); one-time says Today/Tomorrow, every day says so (lone card checked on the Pixel; pairs not yet)
- [~] Skip next alarm: editor toggle, hold menu, and an "Alarm at …" notification 10 min before with Skip; it clears itself when the alarm rings (exact schedule checked on the Pixel)
- [ ] More Expressive motion: morphs, bounces and filled states where they explain something

## Promised, not started
- [ ] More widgets: next alarm, running timer
- [ ] More than one timer at once (upstream issue #5)
- [ ] Own visual design, moving away from the copied Google concept
