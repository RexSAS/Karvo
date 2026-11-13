// Kevin, the Karvo penguin: the one drawing behind every export.
// Traced from the owner's pencil sketch of his head (knit beanie with diagonal ribs, braided band, plain cuff, big
// goggles with two reflection bars, strap, round beak under the lens, three-quarter view to the left). He comes out
// of powder snow, so there is no body.
//
//   node docs/mascot/kevin.mjs [path/to/karvo-web/img]
//
// writes docs/mascot/kevin-<pose>.svg (and, with the argument, the web copies) and the Android layers
// app/src/main/res/drawable/kevin_{back,head_idle,head_sleepy,front,zzz}.xml, which ui/components/Kevin.kt stacks and moves.
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('../..', import.meta.url).pathname;
const INK = '#0A1024', LINE = 5.5, THIN = 3;
const C = { ribA: '#F6C870', ribB: '#E9AE4C', ribDeep: '#C98A2C', cuff: '#FFDF9C', cuffDeep: '#EFBE62', band: '#1B2A5E', block: '#FFF1CF' };
const VIEW = { x: -28, y: -44, w: 600, h: 580 };
// Movement, in drawing units (the same numbers are in ui/components/Kevin.kt): the head starts RISE under the snow and
// comes up, then bobs by BOB. SNOW_LINE is where the snow in front begins to hide it.
const RISE = 400, BOB = 6, SNOW_LINE = 474;

const G = {
  dome: 'M84,150 C84,116 106,82 142,60 C178,40 222,29 268,28 C330,27 392,40 432,66 C458,84 472,108 476,134 L478,182 C440,176 400,172 375,168 C350,160 325,151 300,150 Z',
  band: 'M84,150 L300,150 C325,151 350,160 375,168 C400,172 440,176 478,182 L480,209 C440,203 400,199 375,195 C350,187 325,178 300,177 L86,178 Z',
  cuff: 'M86,178 L300,177 C325,178 350,187 375,195 C400,199 440,203 480,209 L481,239 C440,232 402,228 376,224 C350,216 325,207 300,206 L89,207 Z',
  cuffDeep: 'M86,178 L300,177 C325,178 350,187 375,195 C400,199 440,203 480,209 L480,217 C440,211 400,207 375,203 C350,195 325,186 300,185 L86,186 Z',
  domeShade: 'M84,150 L300,150 C325,151 350,160 375,168 C400,172 440,176 478,182 L478,160 C440,154 400,150 375,146 C350,138 325,129 300,128 L84,128 Z',
  head: 'M88,196 L88,520 L481,520 L481,226 Z',
  front: 'M88,300 L88,520 L393,520 L393,300 Z',
  side: 'M393,222 L481,230 L481,520 L393,520 Z',
  lens: 'M74,236 C80,226 96,221 120,220 L330,221 C356,221 378,226 390,236 C399,246 401,266 400,284 C399,304 396,320 388,328 C378,338 362,344 340,345 L286,345 C262,345 250,338 238,328 C230,321 222,320 212,322 C196,325 180,330 160,336 C136,343 108,342 88,334 C72,327 62,312 60,292 C58,270 62,250 74,236 Z',
  strap: 'M392,240 L481,246 L481,296 L396,302 Z',
  // The beak hangs under the lens: a point to the left, a round belly at the bottom right (as in the sketch).
  beak: 'M98,343 C134,357 164,371 184,383 C202,394 224,394 232,378 C240,362 242,350 238,338 L236,326 L104,332 Z',
  jaw: 'M151.3,365.8 C164,371.8 175,377.6 184,383 C202,394 224,394 232,378 C235.2,371.6 237.4,365.8 238.7,360.5 C212,371 180,372 151.3,365.8 Z',
  snowBack: 'M-4,470 C20,432 70,402 130,398 C170,380 230,378 270,388 C320,376 380,378 420,394 C480,398 526,430 548,470 C500,512 40,512 -4,470 Z',
  snowFront: 'M-8,474 C6,446 34,430 66,432 C84,414 120,410 146,424 C172,408 214,408 238,424 C262,408 304,408 328,422 C354,408 394,408 418,424 C444,412 482,414 500,432 C528,432 548,450 552,474 C504,524 40,524 -8,474 Z',
  // Everything above the snow line: below it the snow in front hides the head, and under the mound nothing may show.
  snowClip: `M-60,-500 H620 V${SNOW_LINE} H-60 Z`,
  snowCap: 'M150,62 C150,38 190,14 250,12 C318,8 384,26 404,56 C412,70 396,76 372,70 C340,84 300,80 276,70 C250,84 210,84 190,70 C170,76 150,74 150,62 Z',
};

const n = v => String(Math.round(v * 10) / 10);
const circle = (cx, cy, r, ry = r) => `M${n(cx - r)},${n(cy)} a${n(r)},${n(ry)} 0 1,0 ${n(2 * r)},0 a${n(r)},${n(ry)} 0 1,0 ${n(-2 * r)},0 Z`;

// Diagonal knit ribs: boundaries sweep up to the right over the dome. One rib is the strip between two neighbours.
const ribs = [];
for (let k = -3; k < 9; k++) { const sx = 52 + k * 50; ribs.push([[sx, 188, sx + 30, 96, sx + 168, 4], [sx + 32, 188, sx + 62, 96, sx + 200, 4]]); }
const quad = ([x0, y0, cx, cy, x1, y1], dx = 0) => `M${n(x0 + dx)},${y0} Q${n(cx + dx)},${cy} ${n(x1 + dx)},${y1}`;
const ribBand = ([a, b]) => `M${a[0]},${a[1]} Q${a[2]},${a[3]} ${a[4]},${a[5]} L${b[4]},${b[5]} Q${b[2]},${b[3]} ${b[0]},${b[1]} Z`;
/** Stitches along a rib as short separate strokes (a dash pattern, written out: VectorDrawable has no dashes). */
function stitches(dx) {
  const out = [];
  for (const [[x0, y0, cx, cy, x1, y1]] of ribs) {
    const at = t => [(1 - t) ** 2 * x0 + 2 * t * (1 - t) * cx + t * t * x1 + dx, (1 - t) ** 2 * y0 + 2 * t * (1 - t) * cy + t * t * y1];
    let prev = at(0), run = 0, start = null;
    for (let i = 1; i <= 400; i++) {
      const p = at(i / 400); run += Math.hypot(p[0] - prev[0], p[1] - prev[1]);
      const on = run % 18 < 9;
      if (on && !start) start = prev;
      if (!on && start) { if (p[0] > 70 && p[0] < 492 && p[1] > 20) out.push(`M${n(start[0])},${n(start[1])} L${n(prev[0])},${n(prev[1])}`); start = null; }
      prev = p;
    }
  }
  return out.join(' ');
}
// The braided band: small blocks that follow the band as it wraps round the head.
const blocks = [];
{
  const pts = [[84, 164], [300, 163.5], [375, 181.5], [479, 195.5]];
  let carry = 17;
  for (let i = 0; i < pts.length - 1; i++) {
    const [x0, y0] = pts[i], [x1, y1] = pts[i + 1], len = Math.hypot(x1 - x0, y1 - y0), ang = Math.atan2(y1 - y0, x1 - x0);
    let d = carry;
    for (; d < len; d += 30.5) {
      const ox = x0 + (x1 - x0) * d / len, oy = y0 + (y1 - y0) * d / len, cos = Math.cos(ang), sin = Math.sin(ang);
      const p = (x, y) => `${n(ox + x * cos - y * sin)},${n(oy + x * sin + y * cos)}`;
      blocks.push(`M${p(-6, -9)} L${p(6, -9)} A5,5 0 0,1 ${p(11, -4)} L${p(11, 4)} A5,5 0 0,1 ${p(6, 9)} L${p(-6, 9)} A5,5 0 0,1 ${p(-11, 4)} L${p(-11, -4)} A5,5 0 0,1 ${p(-6, -9)} Z`);
    }
    carry = d - len;
  }
}

// Gradients, in drawing units. bb() turns "from the corner of this box, along (a, b) of its size" into two points,
// so a slanted gradient keeps its slant on shapes that are not square.
const bb = ([x, y, w, h], a, b) => { const gx = a / w, gy = b / h, k = (gx * gx + gy * gy) / (a * a + b * b); return [x, y, x + gx / k, y + gy / k]; };
const GRAD = {
  dome: { at: bb([60, 0, 440, 200], 1, .7), stops: [[0, '#FFFFFF', .34], [.5, '#FFFFFF', 0], [1, '#3A1C00', .34]] },
  lens: { at: bb([59, 220, 342, 125], .35, 1), stops: [[0, '#D2EEFF'], [.45, '#7FBDF8'], [1, '#2F62C4']] },
  night: { at: [0, 220, 0, 345], stops: [[0, '#26367A'], [1, '#0A1024']] },
  face: { at: [88, 0, 393, 0], stops: [[0, '#DDE7F7'], [.3, '#FFFFFF'], [1, '#E3ECF9']] },
  side: { at: [393, 0, 481, 0], stops: [[0, '#2A3D84'], [1, '#162048']] },
  headTop: { at: [88, 0, 481, 0], stops: [[0, '#2C3F86'], [1, '#1D2A5E']] },
  beak: { at: [0, 326, 0, 392], stops: [[0, '#FFC268'], [1, '#F08F2C']] },
  snow: { at: [0, 408, 0, 512], stops: [[0, '#FFFFFF'], [.55, '#EEF4FF'], [1, '#BFD1F0']] },
  snowB: { at: [0, 378, 0, 502], stops: [[0, '#DCE7FA'], [1, '#A9BFE6']] },
};

// The drawing: P = one path, Gp = a group (clip, turn, shift).
const P = (d, o = {}) => ({ d, ...o });
const Gp = (o, kids) => ({ ...o, kids });
const ink = (w = LINE) => ({ stroke: INK, sw: w, round: true });

/** Kevin in four layers, back to front: the snow behind him, his head, the snow in front, and the z's of his sleep. */
function kevin(pose) {
  const sleepy = pose === 'sleepy';
  // Photobomb (a share design): snow on his beanie, and the shared photo in his goggles. The app draws the photo
  // between the layers under and over the lens, so here the lens only gets a light tint and two reflection bars.
  const photobomb = pose === 'photobomb';
  const lensArt = photobomb
    ? [P('M60,214 L404,214 L404,350 L60,350 Z', { fill: '#7FBDF8', op: .25 }),
      P('M92,304 L166,230', { stroke: '#FFFFFF', sw: 17, round: true, op: .92 }), P('M126,314 L172,270', { stroke: '#FFFFFF', sw: 13, round: true, op: .8 })]
    : sleepy
    ? [P('M318,256 a22,22 0 1,0 20,34 a17,17 0 1,1 -20,-34 Z', { fill: '#F8CD7E' }), // the moon in the dark lens
      P(circle(150, 262, 2.6), { fill: '#FFFFFF', op: .8 }), P(circle(214, 300, 2), { fill: '#FFFFFF', op: .6 }), P(circle(262, 250, 2.2), { fill: '#FFFFFF', op: .7 })]
    : [P('M196,352 L262,286 L288,312 L322,270 L404,352 Z', { fill: '#FFFFFF', op: .34 }), // summits and the sun, mirrored in the lens
      P(circle(352, 262, 11), { fill: '#FFDFA0', op: .95 }),
      P('M92,304 L166,230', { stroke: '#FFFFFF', sw: 17, round: true, op: .92 }), // the two reflection bars of the sketch (and a short third one)
      P('M126,314 L172,270', { stroke: '#FFFFFF', sw: 13, round: true, op: .8 }),
      P('M162,314 L170,306', { stroke: '#FFFFFF', sw: 7, round: true, op: .7 })];
  const head = [
    P(G.head, { fill: 'headTop', ...ink() }),
    Gp({ clip: G.head }, [
      P(G.side, { fill: 'side' }), P('M474,226 L481,227 L481,520 L474,520 Z', { fill: '#F8CD7E', op: .75 }), // warm first light on the edge
      P(G.front, { fill: 'face' }), P('M88,330 L393,330 L393,380 Q240,360 88,372 Z', { fill: '#7C95CC', op: .3 }),
      P('M393,300 L393,520', ink(THIN)),
      P(G.strap, { fill: INK }), P('M394,259 L481,264 L481,274 L395,271 Z', { fill: '#8CCBFF' }), P('M395,283 L481,284 L481,289 L395,290 Z', { fill: '#8CCBFF', op: .55 }),
      P('M428,244 L440,244 A4,4 0 0,1 444,248 L444,294 A4,4 0 0,1 440,298 L428,298 A4,4 0 0,1 424,294 L424,248 A4,4 0 0,1 428,244 Z', { fill: '#35477F' }),
    ]),
    P(G.head, ink()),
    P(G.beak, { fill: 'beak' }), P(G.jaw, { fill: '#C8721C', op: .85 }), P(G.beak, ink()),
    P(G.lens, { fill: sleepy ? 'night' : 'lens' }),
    photobomb && { split: true }, // the photo goes here
    Gp({ clip: G.lens }, [...lensArt, P('M60,214 L404,214 L404,246 Q240,232 60,250 Z', { fill: INK, op: .22 })]),
    P(G.lens, { stroke: INK, sw: 13, round: true }),
    P('M96,226 C110,222 124,220 140,220 L300,221', { stroke: '#5F78C0', sw: 3, round: true, op: .8 }),
    P(G.cuff, { fill: C.cuff, ...ink() }), P(G.cuffDeep, { fill: C.cuffDeep, op: .7 }),
    P(G.band, { fill: C.band, ...ink() }), P(blocks.join(' '), { fill: C.block }),
    P(G.dome, { fill: C.ribA }),
    Gp({ clip: G.dome }, [
      P(ribs.map(ribBand).join(' '), { fill: C.ribB, op: .9 }),
      P(stitches(14), { stroke: '#FFFFFF', sw: 6, round: true, op: .34 }), P(stitches(41), { stroke: C.ribDeep, sw: 6, round: true, op: .34 }),
      P(ribs.map(([a]) => quad(a)).join(' '), { stroke: INK, sw: THIN + .6, round: true }),
      P('M60,0 H500 V200 H60 Z', { fill: 'dome' }), P(G.domeShade, { fill: '#3A1C00', op: .16 }),
    ]),
    P(G.dome, ink()),
    (sleepy || photobomb) && [P(G.snowCap, { fill: '#FFFFFF', ...ink(THIN) }), P('M190,70 C210,80 250,82 276,70 C300,80 340,84 372,70', { stroke: '#BFD1F0', sw: 5, round: true })],
  ].filter(Boolean).flat();
  const puff = (x, y, r, op) => P(circle(x, y, r), { fill: '#FFFFFF', op });
  return {
    back: [P(circle(272, 500, 262, 20), { fill: '#000000', op: .22 }), P(G.snowBack, { fill: 'snowB' })],
    // Asleep he sits lower and leans the other way.
    head: [Gp(sleepy ? { rot: [4, 270, 420], ty: 64 } : { rot: [-2, 270, 430] }, head)],
    front: [P(G.snowFront, { fill: 'snow', stroke: INK, sw: THIN, round: true }),
      ...[[70, 426, 15], [118, 414, 12], [432, 412, 13], [474, 424, 15], [246, 414, 9]].map(p => puff(...p)),
      ...[[40, 392, 4], [506, 386, 4.5], [20, 420, 3]].map(p => puff(...p, .8))],
    zzz: [['M440,150 h26 l-26,28 h26', 1], ['M482,96 h19 l-19,21 h19', .7], ['M514,54 h13 l-13,15 h13', .45]]
      .map(([d, op]) => P(d, { stroke: '#8CCBFF', sw: 7, round: true, op })),
  };
}

// ---- SVG -------------------------------------------------------------------------------------------------------
function toSvg(pose) {
  const clips = [];
  const paint = v => v == null ? 'none' : v.startsWith('#') ? v : `url(#${v})`;
  const node = o => {
    if (o.kids) {
      const tf = [o.ty ? `translate(0 ${o.ty})` : '', o.rot ? `rotate(${o.rot.join(' ')})` : ''].filter(Boolean).join(' ');
      const clip = o.clip ? ` clip-path="url(#c${clips.push(o.clip)})"` : '';
      return `<g${o.cls ? ` class="${o.cls}"` : ''}${tf ? ` transform="${tf}"` : ''}${clip}>${o.kids.map(node).join('')}</g>`;
    }
    return `<path d="${o.d}" fill="${paint(o.fill)}"${o.stroke ? ` stroke="${o.stroke}" stroke-width="${o.sw}"${o.round ? ' stroke-linecap="round" stroke-linejoin="round"' : ''}` : ''}${o.op != null ? ` opacity="${o.op}"` : ''}/>`;
  };
  const k = kevin(pose);
  const [pose0] = k.head; // the head moves inside its pose, and stays hidden below the snow line
  const body = [...k.back, Gp({ clip: G.snowClip }, [{ ...pose0, kids: [Gp({ cls: 'k-head' }, pose0.kids)] }]), ...k.front,
    ...(pose === 'sleepy' ? [Gp({ cls: 'k-zzz' }, k.zzz)] : [])].map(node).join('\n  ');
  const used = new Set(body.match(/url\(#[a-zA-Z]+\)/g)?.map(u => u.slice(5, -1)));
  const grads = Object.entries(GRAD).filter(([id]) => used.has(id)).map(([id, { at, stops }]) =>
    `<linearGradient id="${id}" gradientUnits="userSpaceOnUse" x1="${n(at[0])}" y1="${n(at[1])}" x2="${n(at[2])}" y2="${n(at[3])}">${stops.map(([o, c, a]) =>
      `<stop offset="${o}" stop-color="${c}"${a != null ? ` stop-opacity="${a}"` : ''}/>`).join('')}</linearGradient>`).join('\n    ');
  // He comes up out of the snow, then his head bobs a little; the z's breathe. Nothing moves with reduced motion.
  const css = `.k-head{animation:k-rise .8s cubic-bezier(.2,.9,.3,1) both,k-bob 3.8s ease-in-out .8s infinite}.k-zzz{animation:k-zzz 3.2s ease-in-out infinite}@keyframes k-rise{from{transform:translateY(${RISE}px)}}@keyframes k-bob{50%{transform:translateY(${-BOB}px)}}@keyframes k-zzz{50%{opacity:.25}}@media (prefers-reduced-motion:reduce){.k-head,.k-zzz{animation:none}}`;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${VIEW.x} ${VIEW.y} ${VIEW.w} ${VIEW.h}" width="${VIEW.w}" height="${VIEW.h}">
  <!-- Kevin, the Karvo penguin (${pose}). Made by docs/mascot/kevin.mjs in the Karvo Android repo: change him there. -->
  <style>${css}</style>
  <defs>
    ${grads}
    ${clips.map((d, i) => `<clipPath id="c${i + 1}"><path d="${d}"/></clipPath>`).join('\n    ')}
  </defs>
  ${body}
</svg>
`;
}

// ---- Android VectorDrawable ------------------------------------------------------------------------------------
function toVector(what, nodes) {
  const argb = (c, a = 1) => '#' + Math.round(a * 255).toString(16).padStart(2, '0').toUpperCase() + c.slice(1);
  const node = (o, pad) => {
    if (o.kids) {
      const attrs = [o.rot && `android:rotation="${o.rot[0]}" android:pivotX="${o.rot[1]}" android:pivotY="${o.rot[2]}"`,
        o.tx && `android:translateX="${o.tx}"`, o.ty && `android:translateY="${o.ty}"`].filter(Boolean).join(' ');
      return `${pad}<group${attrs ? ' ' + attrs : ''}>\n${o.clip ? `${pad}    <clip-path android:pathData="${o.clip}" />\n` : ''}${o.kids.map(k => node(k, pad + '    ')).join('\n')}\n${pad}</group>`;
    }
    const grad = o.fill && !o.fill.startsWith('#') ? GRAD[o.fill] : null;
    const attrs = [`android:pathData="${o.d}"`,
      o.fill?.startsWith('#') && `android:fillColor="${o.fill}"`, o.fill && o.op != null && `android:fillAlpha="${o.op}"`,
      o.stroke && `android:strokeColor="${o.stroke}" android:strokeWidth="${o.sw}"`, o.stroke && o.round && 'android:strokeLineCap="round" android:strokeLineJoin="round"',
      o.stroke && o.op != null && `android:strokeAlpha="${o.op}"`].filter(Boolean).join(' ');
    if (!grad) return `${pad}<path ${attrs} />`;
    return `${pad}<path ${attrs}>\n${pad}    <aapt:attr name="android:fillColor">\n${pad}        <gradient android:type="linear" android:startX="${n(grad.at[0])}" android:startY="${n(grad.at[1])}" android:endX="${n(grad.at[2])}" android:endY="${n(grad.at[3])}">\n${
      grad.stops.map(([off, c, a]) => `${pad}            <item android:offset="${off}" android:color="${argb(c, a)}" />`).join('\n')}\n${pad}        </gradient>\n${pad}    </aapt:attr>\n${pad}</path>`;
  };
  return `<?xml version="1.0" encoding="utf-8"?>
<!-- Kevin, the Karvo penguin: ${what}. One of the layers ui/components/Kevin.kt stacks.
     Made by docs/mascot/kevin.mjs: change him there, not here. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="${VIEW.w / 4}dp"
    android:height="${VIEW.h / 4}dp"
    android:viewportWidth="${VIEW.w}"
    android:viewportHeight="${VIEW.h}">
${node(Gp({ tx: -VIEW.x, ty: -VIEW.y }, nodes), '    ')}
</vector>
`;
}

const drawable = (name, what, nodes) => writeFileSync(join(root, `app/src/main/res/drawable/kevin_${name}.xml`), toVector(what, nodes));
for (const pose of ['idle', 'sleepy']) {
  const svg = toSvg(pose);
  writeFileSync(join(root, `docs/mascot/kevin-${pose}.svg`), svg);
  if (process.argv[2]) writeFileSync(join(process.argv[2], `kevin-${pose}.svg`), svg);
  drawable(`head_${pose}`, `his head, ${pose}`, kevin(pose).head);
}
const k = kevin('sleepy');
drawable('back', 'the snow behind him', k.back);
drawable('front', 'the snow in front of him', k.front);
drawable('zzz', "the z's of his sleep", k.zzz);
// Photobomb: his head in two layers around the lens (the app draws the shared photo between them, clipped to the lens),
// with the snow behind and in front but no shadow. The lens outline and his tilt go to a values file for that clip.
{
  const pb = kevin('photobomb');
  const [tilted] = pb.head;
  const at = tilted.kids.findIndex(o => o.split);
  drawable('photobomb_under', 'photobomb, everything under the shared photo', [P(G.snowBack, { fill: 'snowB' }), Gp({ clip: G.snowClip }, [{ ...tilted, kids: tilted.kids.slice(0, at) }])]);
  drawable('photobomb_over', 'photobomb, everything over the shared photo', [Gp({ clip: G.snowClip }, [{ ...tilted, kids: tilted.kids.slice(at + 1) }]), ...pb.front]);
  writeFileSync(join(root, 'app/src/main/res/values/kevin_lens.xml'), `<?xml version="1.0" encoding="utf-8"?>
<!-- Made by docs/mascot/kevin.mjs: change Kevin there, not here. Used by the Photobomb share design. -->
<resources>
    <!-- His goggle lens, in his drawing units (the drawable's viewport, before its shift of ${-VIEW.x}, ${-VIEW.y}). -->
    <string name="kevin_lens_path" translatable="false">${G.lens}</string>
    <!-- His tilt in the photobomb pose: degrees, around ${tilted.rot[1]}, ${tilted.rot[2]}. -->
    <string name="kevin_photobomb_tilt" translatable="false">${tilted.rot.join(',')}</string>
</resources>
`);
}
console.log(`Kevin: idle and sleepy written${process.argv[2] ? `, web copies in ${process.argv[2]}` : ''}.`);
