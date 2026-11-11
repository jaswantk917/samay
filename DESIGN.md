# Design philosophy

> Made for the love of it. Simple. Intuitive. No nonsense. Beautiful.

For small, personal Android apps that fill one everyday need, built by people and AI agents alike. When a choice isn't covered here, pick the simpler, quieter one.

## What the app is

It does one job well, nothing more. No ads, no accounts, no tracking, no servers, no upsell. Free and open source.

## Principles

1. **Fewer options.** Every setting is a question the user has to answer. Add one only when people really differ. Remove any that has an obvious right answer.
2. **Don't show what's already done.** A card asking for a permission disappears once it's granted. A status line speaks only when there's something to say.
3. **Let people discover.** No tutorials, no gesture lists, no "tip:" lines, no coach marks. Swipe, hold and tap work where a person would try them. If a gesture needs explaining, the design is wrong.
4. **One look everywhere.** Every screen uses the same header, blur, type scale, spacing and motion. Ask yourself whether a new screen would look like it belongs if you put it next to the existing ones. Different chrome on one screen is a major bug, not polish.
5. **Quiet by default.** No badges for their own sake, no loud colours, no red unless something is wrong. The interface steps back so the content comes forward.
6. **Clever, not flashy.** Motion explains something about what's happening. If an animation doesn't tell you anything, cut it.
7. **Safe when wrong.** Whatever the app guesses, it fails toward the safe behaviour, and there is always a way through. Aim for good enough and safe, not exact.
8. **Everything on the phone.** Nothing leaves the device unless the user sends it.

## Look

- Material 3 Expressive in Jetpack Compose, close to Google's own apps, with Google Sans Flex.
- **Header (every main screen):**
  - The title is `displaySmallEmphasized` at 24 dp from the left edge, 8 dp under the status bar.
  - Under it is one quiet `labelLarge` line in `onSurfaceVariant`.
  - Behind it sits a fading surface scrim, with a soft fade under the clock.
  - The title tucks away (shrinks and fades) as the page scrolls, and comes back on the way up.
- **Edges:** content blurs as it slides under the header and into the band above the navigation bar. The bottom blur eases in (cubic), so there's no visible line where it starts.
- **Shapes:** groups of rows have big outer corners and small inner ones. Use material shapes (cookie, clover, sunny) for the few things that deserve character, like a settings button.
- **Theme:** every screen follows the user's palette (app colours or wallpaper), pure black and blur settings.
- **Bottom sheets:** content that barely overflows shouldn't scroll, or the sheet stutters. Scroll only past a small slack, with no overscroll.

## Motion

- Every screen gets small, purposeful motion: a staged arrival, press feedback, a reaction to success, and a living loader while something is waiting.
- Use the theme's expressive motion scheme. Springs over durations.
- Nothing jumps. A list that shrinks animates its height; an element that moves animates its position, not its size first.

## Haptics

- **Sparse.** Phones with cheap motors buzz unpleasantly.
- **Switches:** a crisp click when one turns on, a lighter tick when it turns off.
- **Swipes:** one tick as a swipe crosses the point where letting go acts. Nothing while it slides.
- **Strong pulses only for big, final moments.**
- **Haptics the user feels away from the screen get a switch.** Example: a buzz when an alarm is dismissed.

## Words

- Write plainly, the way a person would say it: "Rings in 7 hours", not "Alarm scheduled successfully".
- Name things by what people recognise.
- Release notes say only what changed since the last version, in one or two human sentences.
- Never claim certainty the app doesn't have.

## How decisions get made

- **The owner picks** names, version numbers, default screens and anything users will see first. Suggest; don't decide.
- **Find the cause before fixing.** Get evidence from the device (logs, dumpsys) first. No speculative fixes.
- **Explaining is not changing.** When asked "why" or "how", answer; don't edit code.
- **Change only what was objected to.** Don't redesign the neighbourhood.
- **Screenshots and store assets:** blur family members, contacts and phone numbers. Check the status bar too.

## Deliberately not here

- **Ads, accounts, analytics, paid tiers.**
- **Settings for things with one right answer.**
- **Hint text, onboarding tours and gesture cheat sheets.**
