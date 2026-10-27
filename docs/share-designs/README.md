# Share designs

A photo can be shared (Moments → Share) in two Karvo designs:

| Design | Made from | Look |
|---|---|---|
| **Banner** | code (`data/moments/ShareCard.kt`) | The whole photo, any shape, with a dark fade at the bottom carrying the emblem, KARVO and the date |
| **Frame** | `share_2.png`, shipped as `app/src/main/res/drawable-nodpi/share_frame.png` | 9:16: the photo blurred across the background, and sharp inside a tilted paper frame |

`share_banner.png` is the Banner design as a PNG, for reference only: the app draws Banner in code.
`karvo-emblem.svg` is the Karvo logo in white: place it as a smart object and recolour it with a Color Overlay.

## Frame rules

- **1080 × 1920**, transparent background.
- **Outside the frame: transparent.** The app fills it with a blurred, slightly darkened copy of the photo.
- **Photo window: the transparent area the frame closes in on every side.** The app puts the sharp photo there,
  filling the window and cropped from the centre. The frame must close all the way round, or the window leaks out.
- **Date spot: one solid magenta (#FF00FF) rectangle**, tilted like the frame. The app paints it over with the
  frame colour just below it and writes the date there in dark navy, as tall as the box. No other #FF00FF pixels.
- **Tilt:** the app turns the photo by the date box's angle, so keep the box parallel to the frame.

## Updating the frame

Edit `share_2.png` in Photoshop, export it (*File → Export → Export As → PNG*, transparency on, 1×), and copy it over
`app/src/main/res/drawable-nodpi/share_frame.png`. `ShareTemplateTest` checks that the shipped PNG still follows these
rules (size, tilt, window and date spot).
