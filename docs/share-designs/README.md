# Share designs

The 4 designs a photo can be shared in (Moments → Share). These PNGs are starter templates to restyle in Photoshop.

| File | Size | Design |
|---|---|---|
| `share_banner.png` | 1080 × 1350 (4:5) | Dark fade at the bottom with the emblem and KARVO |
| `share_polaroid.png` | 1080 × 1350 (4:5) | Paper border, name and tagline on the wide bottom edge |
| `share_story.png` | 1080 × 1920 (9:16) | Night-sky card, photo with rounded corners in the middle |
| `share_stamp.png` | 1080 × 1350 (4:5) | Round gold-ringed Karvo seal in the top corner |

`karvo-emblem.svg` is the Karvo logo in white: place it as a smart object and recolour it with a Color Overlay.

## Rules

- **Photo window = everything that isn't 100% opaque.** The photo is scaled to fill the box around those pixels and
  cropped from the centre, then the PNG is drawn over it. Fades and shadows over the photo can be semi-transparent;
  everything outside the window must be fully opaque, or the window grows to include it.
- **Date spot = a solid magenta (#FF00FF) rectangle.** The app removes it and writes the date there, centred, as tall
  as the box. Keep exactly one per design, with no other #FF00FF pixels.
- Keep the file names and sizes. Text in the PNG (KARVO, "Made with Karvo") stays as you draw it, in one language.

## Export

*File → Export → Export As → PNG*, transparency on, 1×. Overwrite the file here and tell Claude the date's colour and
font for each design.

> The app still draws the designs in code (`data/moments/ShareCard.kt`); loading these PNGs is the next step.
