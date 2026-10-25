# Kevin — the Karvo penguin

`kevin.svg` is the source art, 512×512 on a transparent background. `kevin-preview.png` shows it on the app background.
Each part is its own group so Rive keeps them as separate layers: `shadow`, `foot_l/r`, `flipper_l/r`, `body`, `belly`, `face`, `cheeks`, `eye_l/r`, `lid_l/r` (hidden, for blinking), `beak`, `scarf`, `scarf_tail`, `goggles`.

## Making `kevin.riv`
1. Go to rive.app, create a new file and drag `kevin.svg` onto the artboard.
2. Set the origin of each flipper to its shoulder (about 155,250 and 357,250), and the scarf tail's origin to its top.
3. Animations planned for the app:
   - `idle` (loop): the body bobs 4px and the eyelids blink every ~3s (`lid_l/r` opacity 0 → 1 → 0).
   - `wave`: `flipper_r` rotates between −10° and −40°, 3 times.
   - `cheer`: Kevin jumps 30px, both flippers go up and the scarf tail swings.
4. Put the animations in a State Machine called `Kevin` with triggers `wave` and `cheer`.
5. Export → `kevin.riv` → `app/src/main/res/raw/kevin.riv`.

Colors come from the Alpine Night theme: navy `#1B2A52`, gold `#F2C879` / `#E8A73C`, glacier `#8FD3FF`.
