# Kevin

Kevin is the Karvo penguin. He follows the owner's pencil sketch of his head: a knit beanie with a braided band, big
goggles with two reflection bars, a strap, and a round beak under the lens. He comes out of powder snow, so he has no body.

| Pose | Where | File |
|---|---|---|
| **Idle** | Welcome (first open) | `kevin-idle.svg` |
| **Sleepy** | Empty pages (no lessons yet) | `kevin-sleepy.svg` |

`kevin.mjs` is the one drawing behind every export. Change Kevin there, then run:

```
node docs/mascot/kevin.mjs ../karvo-web/img
```

It writes the two SVG files here, the web copies, and the Android layers (`app/src/main/res/drawable/kevin_*.xml`).

Kevin moves very little: he comes up out of the snow, his head bobs, and the z's breathe while he sleeps. On Android,
`ui/components/Kevin.kt` stacks four still layers (snow behind, head, snow in front, z's) and only slides and fades
them. The SVG files carry their own CSS for the web. With animations turned off (Android "Remove animations",
`prefers-reduced-motion`) he is still.
