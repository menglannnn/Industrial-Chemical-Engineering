#!/usr/bin/env node
// gen.js: drive IChemE's fluid device screen (FluidDeviceScreen) through the minecraft-mcp bridge, finding every
// widget by its label. Unified thermo port test world, batch 2026-09-24-coolprop-low-temperature.
//
// Why a layout table: the bridge (minecraft-mcp 0.3.0) reports widgets without their text. `enumerate_widgets` answers
// {"i","c","x","y","w","h","press"} (class and GUI-scaled box), and `get_screen_buttons` answers "" for a Button and
// "_" for an EditBox. So the labels come from the screen's source: predict() restates FluidDeviceScreen.rebuild() at
// claude/unified-thermo-port 40a44732 (its widgets are unchanged since main 0.8.0, and through 87c106f6: WP3, 085676ea,
// only appends a data-range notice to the status text, and en_us.json is unchanged) with the en_us strings. A label is
// resolved only when the live widget list equals the prediction exactly (same count, order, class and box); any
// difference aborts with the live list printed, so no click lands on a guessed widget. If a later bridge build adds a
// text field to the dump (label, message or text), identify() compares it as well.
//
// Needs the runMcpClient lane: the icheme_mcp_compat mixins route `click` (framebuffer pixels) to
// Screen.mouseClicked at GUI coordinates, `type_text` to the focused EditBox and Ctrl+A to select-all. Never use desktop
// input. A pump and a compressor (and a void and a valve) have the same widget boxes: the block you open tells them apart.
//
//   node gen.js selftest [dir]                 offline: the table against the 0.8.0 widget dumps in fixtures/
//   node gen.js dump | labels                  the open screen's widgets with their labels | the labels it offers
//   node gen.js click <label>                  click a widget of the open screen by label (or key, see dump)
//   node gen.js field <label> <value>          focus an edit box by label, select all, type the value
//   node gen.js mixtures                       the generator's choice list as this script computes it
//   node gen.js mixture "<choice label>"       generator: Composition, Load mixture, the choice by label
//   node gen.js open <x> <y> <z> <kind> [aim]  aim at a block and open its screen; kind GENERATOR|RESERVOIR|PIPE|VOID|
//                                              PUMP|COMPRESSOR|VALVE; aim "top" (default), "east"|"west"|"north"|"south"
//                                              (stand on that neighbour's top, look at the block), "floor-south:<d>"
//   node gen.js configure '<json>'             open, set, apply, wait, screenshot: {"at":[x,y,z],"kind":"GENERATOR",
//                                              "mixture":"Pilot: N2 + 10 % CO2","pressure":300000,"temperatureK":150,
//                                              "flow":0.01,"head":500000,"ratio":3,"diameter":0.05,"shot":"<prefix>"}
//   node gen.js read '<json>'                  open a device and screenshot views: {"at":[...],"kind":"RESERVOIR",
//                                              "views":["overview","organic","vapor","solids","outlets"],"shot":"<prefix>"}
//
// Environment: MC_MCP_PORT (default 9876); MC_GUI_SCALE (framebuffer pixels per GUI unit, else read from a screenshot);
// MC_WORKTREE (bundled mixtures are read from its sources); MC_GAME_DIR (default <worktree>/run/mcp-client).
'use strict';
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const PORT = process.env.MC_MCP_PORT || '9876';
const BASE = `http://127.0.0.1:${PORT}`;
const WORKTREE = process.env.MC_WORKTREE || 'D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0';
const GAME_DIR = process.env.MC_GAME_DIR || path.join(WORKTREE, 'run', 'mcp-client');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const log = (...a) => console.log('[gen]', ...a);

async function cmd(name, params = {}, timeoutMs = 30000) {
  const r = await fetch(`${BASE}/api/cmd`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ cmd: name, ...params }), signal: AbortSignal.timeout(timeoutMs),
  });
  const text = await r.text();
  let v; try { v = JSON.parse(text); } catch { v = text; }
  if (!r.ok) throw new Error(`bridge ${name} -> HTTP ${r.status} ${text}`);
  return v;
}

// ---------------------------------------------------------------------------------------------------------------
// The screen, restated from FluidDeviceScreen (rebuild, buildContents, buildComposition, buildSolids) and en_us.json
// ---------------------------------------------------------------------------------------------------------------
const TXT = {
  overview: 'Overview', composition: 'Composition', solids: 'Solids', close: 'Close', apply: 'Apply changes',
  connections: 'Connections', outlets: 'Connections by face', unit: u => `Temperature: ${u}`, mole: 'Mole % · kmol', mass: 'Mass % · kg',
  load: 'Load mixture', fresh: 'New mixture', back: 'Back', refresh: 'Refresh', folder: 'Open folder',
  pressure: 'Pressure (Pa abs)', temperature: u => `Temperature (${u})`, flow: 'Suction flow (m³/s)', head: 'Max pressure rise (Pa)',
  ratio: 'Max pressure ratio (out/in)', diameter: 'Internal diameter (m)', solidFraction: 'Solids (volume %)',
  phase: ['Organic phase', 'Aqueous phase', 'Vapor', 'Solids'], recover: 'Recover solids', remove: '×',
  solidCol: ['Solid material ID', 'Size (µm)', 'Mass share'],
};
const KINDS = ['GENERATOR', 'RESERVOIR', 'PIPE', 'VOID', 'PUMP', 'COMPRESSOR', 'VALVE', 'FILTER'];
const trunc = Math.trunc;

/** The screen's frame from the live widgets: Overview is always the first widget, at (10, 29) inside the frame; Close
 * sits at (10, imageHeight - 27); the temperature unit button ends 10 px inside the right edge. */
function frame(widgets) {
  const first = widgets[0];
  if (!first || first.c !== 'Button' || first.w !== 100) throw new Error('not a FluidDeviceScreen layout: the first widget is not the Overview button');
  const L = first.x - 10, T = first.y - 29;
  const bottom = Math.max(...widgets.filter(w => w.c === 'Button').map(w => w.y));
  const close = widgets.find(w => w.c === 'Button' && w.x === L + 10 && w.y === bottom);
  const right = widgets.filter(w => w.c === 'Button' && w.y === bottom).sort((a, b) => (b.x + b.w) - (a.x + a.w))[0];
  if (!close || !right) throw new Error('no Close / temperature unit button on the bottom row');
  return { L, T, IW: right.x + right.w + 10 - L, IH: close.y - T + 27 };
}
function bottomButtons(IW) {
  const widths = [65, 110, 118, 108], natural = 401, room = IW - 38;
  if (room < natural) for (let i = 0; i < 4; i++) widths[i] = trunc(widths[i] * room / natural);
  return widths;
}
const tableTop = (page, pipe) => page === 0 ? (pipe ? 110 : 130) : page === 1 ? 166 : page === 3 ? 104 : page === 4 ? 100 : 142;
const rowHeight = page => page === 0 || page === 3 ? 18 : page === 4 ? 24 : 25;
function visibleRows(f, page, pipe) { return Math.max(1, trunc((f.IH - 64 - tableTop(page, pipe)) / rowHeight(page))); }
function geometry(f) {
  const rail = Math.min(210, trunc(f.IW / 3)), tableX = rail + 24;
  return { rail, tableX, tableWidth: f.IW - tableX - 22 };
}

/** The widgets FluidDeviceScreen builds in state s, in its order. s: {kind, canEdit, page (0..4), templates, choices,
 * choiceScroll, rows, rowScroll, solidScroll, connections, unit ('C'|'K'), mass, railOffset}. Each entry: key (unique),
 * label (the text drawn on or beside it), c (class), x, y, w, h (screen coordinates, GUI units). */
function predict(f, s) {
  const out = [], g = geometry(f), b = bottomButtons(f.IW), by = f.IH - 27;
  const add = (key, label, c, x, y, w, extra = {}) => out.push({ key, label, c, x: f.L + x, y: f.T + y, w, h: 18, ...extra });
  const pipe = s.kind === 'PIPE', gen = s.kind === 'GENERATOR', tank = s.kind === 'RESERVOIR', unit = s.unit || 'C';
  add('tab:overview', TXT.overview, 'Button', 10, 29, 100);
  if (gen && s.canEdit) { add('tab:composition', TXT.composition, 'Button', 116, 29, 112); add('tab:solids', TXT.solids, 'Button', 234, 29, 100); }
  add('unit', TXT.unit(unit), 'Button', f.IW - 10 - b[3], by, b[3]);
  add('basis', s.mass ? TXT.mass : TXT.mole, 'Button', f.IW - 16 - b[3] - b[2], by, b[2]);
  add('close', TXT.close, 'Button', 10, by, b[0]);
  if (s.canEdit && !tank && s.kind !== 'FILTER') add('apply', TXT.apply, 'Button', 16 + b[0], by, b[1]);
  if (pipe && s.connections > 1) add('tab:connections', TXT.connections, 'Button', 116, 29, 112);
  if (tank) add('tab:outlets', TXT.outlets, 'Button', 116, 29, 130);
  const railTop = pipe ? 78 : 90, railBottom = f.IH - 52, y = railTop - (s.railOffset || 0);
  const field = (key, label, fy) => add('field:' + key, label, 'EditBox', 12, fy + 12, g.rail - 4,
    { visible: fy + 12 >= railTop && fy + 30 <= railBottom });
  if (s.canEdit) switch (s.kind) {
    case 'GENERATOR': field('pressure', TXT.pressure, y); field('temperature', TXT.temperature(unit), y + 42); break;
    case 'PUMP': field('flow', TXT.flow, y); field('head', TXT.head, y + 42); break;
    case 'COMPRESSOR': field('flow', TXT.flow, y); field('ratio', TXT.ratio, y + 42); break;
    case 'VALVE': case 'VOID': field('pressure', TXT.pressure, y); break;
    case 'PIPE': field('diameter', TXT.diameter, y); break;
  }
  if (s.kind === 'FILTER' && s.canEdit) add('recover', TXT.recover, 'Button', 12, y, g.rail - 4);
  if (s.page === 0) {
    const w = trunc((g.tableWidth - 12) / 4);
    for (let i = 0; i < 4; i++) add('phase:' + i, TXT.phase[i], 'Button', g.tableX + i * (w + 4), pipe ? 62 : 82, w);
  } else if (s.page === 1 && s.templates) {
    add('back', TXT.back, 'Button', g.tableX, 82, 76); add('refresh', TXT.refresh, 'Button', g.tableX + 82, 82, 90);
    add('folder', TXT.folder, 'Button', g.tableX + 178, 82, 100);
    const V = visibleRows(f, 1), from = s.choiceScroll || 0;
    for (let i = from; i < Math.min(s.choices, from + V); i++) add('choice:' + i, null, 'Button', g.tableX, 166 + (i - from) * 25, g.tableWidth - 4);
  } else if (s.page === 1) {
    const bw = Math.max(60, trunc((g.tableWidth - 12) / 3));
    add('load', TXT.load, 'Button', g.tableX, 82, bw); add('new', TXT.fresh, 'Button', g.tableX + bw + 6, 82, bw);
    add('search', 'component search', 'ComponentDropdown', g.tableX, 112, Math.min(360, g.tableWidth - 4) - 20);
    const V = visibleRows(f, 1), from = s.rowScroll || 0, amount = g.tableX + trunc(g.tableWidth / 2);
    for (let i = from; i < Math.min(s.rows, from + V); i++) {
      const yy = 166 + (i - from) * 25;
      add('amount:' + i, 'Amount', 'EditBox', amount, yy, 64); add('remove:' + i, TXT.remove, 'Button', g.tableX + g.tableWidth - 26, yy, 22);
    }
  } else if (s.page === 2) {
    add('field:solidFraction', TXT.solidFraction, 'EditBox', g.tableX, 94, Math.min(160, g.tableWidth));
    const V = visibleRows(f, 2), from = s.solidScroll || 0, w = g.tableWidth;
    for (let i = from; i < Math.min(64, from + V); i++) for (let c = 0; c < 3; c++) {
      const x = g.tableX + (c === 0 ? 0 : c === 1 ? trunc(w / 2) : trunc(3 * w / 4)), width = c === 0 ? trunc(w / 2) - 6 : trunc(w / 4) - 6;
      add(`solid:${i}:${c}`, TXT.solidCol[c], 'EditBox', x, 142 + (i - from) * 25, width);
    }
  }
  return out;
}

const same = (a, b) => a.c === b.c && a.x === b.x && a.y === b.y && a.w === b.w && a.h === b.h;
const liveText = w => w.label ?? w.message ?? w.text ?? w.msg;

/** The first candidate state (of the given kinds) whose prediction equals the live list, the counts read from it. */
function identify(widgets, kinds = KINDS) {
  const f = frame(widgets), g = geometry(f);
  const choices = widgets.filter(w => w.c === 'Button' && w.x === f.L + g.tableX && w.w === g.tableWidth - 4).length;
  const rows = widgets.filter(w => w.c === 'EditBox' && w.w === 64 && w.x === f.L + g.tableX + trunc(g.tableWidth / 2)).length;
  let tried = 0;
  for (const kind of kinds) for (const canEdit of [true, false]) for (const page of [0, 1, 2, 3, 4]) for (const templates of page === 1 ? [false, true] : [false])
    for (const connections of kind === 'PIPE' ? [1, 2] : [0]) {
      const s = { kind, canEdit, page, templates, choices, rows, connections, choiceScroll: 0, rowScroll: 0, solidScroll: 0 };
      const p = predict(f, s); tried++;
      if (p.length !== widgets.length || !p.every((e, i) => same(e, widgets[i]))) continue;
      for (let i = 0; i < p.length; i++) {
        const t = liveText(widgets[i]);
        if (t !== undefined && t !== '' && t !== '_' && p[i].label && !String(t).includes(p[i].label)) throw new Error(`live text "${t}" differs from the table's "${p[i].label}" at widget ${i}`);
      }
      return { f, s, widgets: p.map((e, i) => ({ ...e, i: widgets[i].i })) };
    }
  const err = new Error(`the live widgets match no predicted FluidDeviceScreen state of ${kinds.join('/')} (frame ${JSON.stringify(f)}, ${tried} states tried)`);
  err.live = widgets; throw err;
}

// ---------------------------------------------------------------------------------------------------------------
// Live operations
// ---------------------------------------------------------------------------------------------------------------
let SCALE = process.env.MC_GUI_SCALE ? Number(process.env.MC_GUI_SCALE) : null;

async function widgets() {
  const r = await cmd('enumerate_widgets');
  if (!r || r.error) return { screen: null, widgets: [] };
  return { screen: r.screen, widgets: r.widgets || [] };
}
async function screenName() { return (await widgets()).screen; }

/** The open device screen, identified; throws when no FluidDeviceScreen is open or it matches no state. */
async function current(kinds) {
  const live = await widgets();
  if (live.screen !== 'FluidDeviceScreen') throw new Error(`expected FluidDeviceScreen, the open screen is ${live.screen}`);
  return identify(live.widgets, kinds);
}

function pngSize(file) {
  const b = fs.readFileSync(file);
  if (b.toString('ascii', 12, 16) !== 'IHDR') throw new Error('not a PNG: ' + file);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) };
}
/** Framebuffer pixels per GUI unit, from a screenshot's width and the frame (GUI width = 2 L + IW, or one more). */
async function scale(f) {
  if (SCALE) return SCALE;
  const file = path.join(os.tmpdir(), `gen-scale-${Date.now()}.png`).replace(/\\/g, '/');
  await cmd('screenshot_to_file', { path: file }); await sleep(500);
  const { w } = pngSize(file); fs.rmSync(file, { force: true });
  const guiWidth = 2 * f.L + f.IW, s = Math.round(w / guiWidth);
  if (!(s >= 1) || Math.abs(w / s - guiWidth) > 1) throw new Error(`cannot read the GUI scale: framebuffer ${w} px, GUI ${guiWidth} units`);
  SCALE = s; log(`GUI scale ${s} (framebuffer ${w} px, GUI ${guiWidth} units)`); return s;
}

/** The GUI scale from the client's options.txt (guiScale; 0 is automatic: the largest scale that keeps the GUI at least
 * 320 x 240, as Minecraft computes it) and the framebuffer size of a screenshot. For screens other than the device
 * screen, whose frame tells nothing about the scale. */
async function scaleFromOptions() {
  if (SCALE) return SCALE;
  const options = path.join(GAME_DIR, 'options.txt');
  const set = fs.existsSync(options) ? Number((/^guiScale:(\d+)/m.exec(fs.readFileSync(options, 'utf8')) || [])[1] || 0) : 0;
  const file = path.join(os.tmpdir(), `gen-scale-${Date.now()}.png`).replace(/\\/g, '/');
  await cmd('screenshot_to_file', { path: file }); await sleep(500);
  const { w, h } = pngSize(file); fs.rmSync(file, { force: true });
  let s = 1; while (s !== set && s < w && s < h && Math.trunc(w / (s + 1)) >= 320 && Math.trunc(h / (s + 1)) >= 240) s++;
  SCALE = s; log(`GUI scale ${s} (options guiScale:${set}, framebuffer ${w}x${h})`); return s;
}
function setScale(s) { SCALE = s; }

/** Click a GUI point through the compat path, only while a screen is open (a compat click with no screen open
 * attacks the block under the crosshair). */
async function clickAt(f, x, y) {
  const name = await screenName();
  if (!name) throw new Error('refusing to click: no screen is open (the click would attack a block)');
  const s = SCALE || await scale(f);
  const r = await cmd('click', { x: Math.round(x * s), y: Math.round(y * s), button: 0 });
  if (r && r.error) throw new Error('click: ' + JSON.stringify(r));
  await sleep(350);
  return r;
}
function labels(id) { return id.widgets.map(w => w.label ? `${w.label} [${w.key}]` : w.key); }
function find(id, label) {
  const hits = id.widgets.filter(w => w.key === label || w.label === label);
  if (hits.length !== 1) throw new Error(`label "${label}": ${hits.length} widgets on ${id.s.kind}/page${id.s.page}${id.s.templates ? '/templates' : ''}; offered: ${labels(id).join(' | ')}`);
  if (hits[0].visible === false) throw new Error(`label "${label}" lies outside the rail's visible area (scroll the rail first)`);
  return hits[0];
}
async function click(label, kinds) {
  const id = await current(kinds), w = find(id, label);
  log(`click "${label}" -> ${w.c} (${w.x},${w.y} ${w.w}x${w.h})`);
  return clickAt(id.f, w.x + trunc(w.w / 2), w.y + trunc(w.h / 2));
}
async function field(label, value, kinds) {
  const id = await current(kinds), w = find(id, label);
  if (w.c !== 'EditBox') throw new Error(`"${label}" is a ${w.c}, not an edit box`);
  await clickAt(id.f, w.x + trunc(w.w / 2), w.y + trunc(w.h / 2)); await sleep(250);
  await cmd('hotkey', { keys: 'ctrl,a' }); await sleep(400);
  await cmd('type_text', { text: String(value) }); await sleep(1000);
  log(`field "${w.label}" <- ${value}`);
}

// --- mixtures ------------------------------------------------------------------------------------------------
const AXIS = ['Nitrogen', 'Methane', 'Ethane', 'CarbonDioxide', 'Water'];
/** The generator's choice list in the screen's order (mixtureChoices): the network's fluid presets (the p6_pilot
 * networks/default.json order with their preset labels, then FluidPresetCatalog's demo slurry), the bundled client
 * mixtures whose components all lie on the axis (none on the pilot axis), then custom files sorted by name. */
function mixtureList(opts = {}) {
  const server = opts.server || ['Water', 'Nitrogen', 'Methane', 'Ethane', 'Carbon dioxide', 'Water slurry (10% solids, 100 µm)'];
  const list = server.map(label => ({ label, components: null }));
  const bundledDir = path.join(WORKTREE, 'src/main/resources/assets/icheme/mixturepreset');
  try {
    for (const file of JSON.parse(fs.readFileSync(path.join(bundledDir, 'index.json'), 'utf8'))) {
      const j = JSON.parse(fs.readFileSync(path.join(bundledDir, file), 'utf8'));
      if (j.components.every(c => AXIS.includes(c.id))) list.push({ label: `${j.name} · Bundled`, components: j.components });
    }
  } catch (e) { log('bundled mixtures not read (' + e.message + '); assuming none lies on the pilot axis'); }
  const dir = path.join(GAME_DIR, 'IChemE', 'mixturepreset');
  const files = fs.existsSync(dir) ? fs.readdirSync(dir).filter(n => n.endsWith('.json')).sort() : [];
  for (const file of files) {
    try {
      const j = JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8'));
      if (j.components.every(c => AXIS.includes(c.id))) list.push({ label: `${j.name} · Custom`, components: j.components, file });
    } catch (e) { log(`custom mixture ${file} unreadable: ${e.message} (the screen skips it too)`); }
  }
  return list;
}
async function mixture(choiceLabel) {
  let id = await current(['GENERATOR']);
  if (id.s.page !== 1 || id.s.templates) { await click('tab:composition', ['GENERATOR']); id = await current(['GENERATOR']); }
  await click('load', ['GENERATOR']); await sleep(300);
  id = await current(['GENERATOR']);
  if (!id.s.templates) throw new Error('Load mixture did not open the mixture list');
  const list = mixtureList(), k = list.findIndex(m => m.label === choiceLabel || m.label === `${choiceLabel} · Custom`);
  if (k < 0) throw new Error(`no choice "${choiceLabel}"; the list is: ${list.map(m => m.label).join(' | ')}`);
  const V = visibleRows(id.f, 1), total = list.length;
  if (id.s.choices !== Math.min(total, V)) throw new Error(`the screen shows ${id.s.choices} choices, the computed list ${total} (visible ${V}): the lists differ`);
  let from = 0;
  if (k >= V) {
    // The list's scrollbar seeks (ProcessScroll.seek): value = round((y - top - thumb / 2) * max / (height - thumb)).
    const top = id.f.T + 166, height = Math.max(20, id.f.IH - 64 - 166), max = total - V;
    const thumb = Math.min(height, Math.max(18, trunc(height * V / total)));
    from = Math.min(k, max);
    const y = top + thumb / 2 + from * (height - thumb) / max, x = id.f.L + id.f.IW - 16 + 4;
    log(`scroll the mixture list to ${from} of ${max} (choice ${k})`);
    await clickAt(id.f, x, Math.round(y)); await sleep(300);
  }
  const g = geometry(id.f), y = id.f.T + 166 + (k - from) * 25 + 9, x = id.f.L + g.tableX + trunc((g.tableWidth - 4) / 2);
  log(`choose "${list[k].label}" (choice ${k}, row ${k - from})`);
  await clickAt(id.f, x, y); await sleep(400);
  id = await current(['GENERATOR']);
  const expected = Math.min(visibleRows(id.f, 1), list[k].components ? list[k].components.filter(c => c.amount > 0).length : 1);
  if (id.s.templates || id.s.page !== 1) throw new Error('the choice did not load: the mixture list is still open');
  if (id.s.rows !== expected) throw new Error(`loaded ${id.s.rows} composition rows, "${list[k].label}" has ${expected}: another choice was clicked`);
  log(`mixture loaded: ${id.s.rows} component row(s)`);
}

// --- aiming and opening ---------------------------------------------------------------------------------------
async function chat(line) {
  await cmd('open_chat'); await sleep(400);
  await cmd('type_text', { text: line }); await sleep(400);
  await cmd('press_key', { key: 'enter' }); await sleep(700);
}
/** Where to stand and look to use the block at (x, y, z). "top": on it, looking down (use_item); a side: on that
 * neighbour's top, looking at the block's centre (pitch atan(2.12 / 1) = 64.7); "floor-south:d": d blocks south of the
 * block's south face, feet on the floor at the block's level, looking north at the face's middle. */
function aimFor(x, y, z, aim = 'top') {
  if (aim === 'top') return { tp: `${x}.5 ${y + 1} ${z}.5 0 89`, use: 'use_item' };
  const side = { east: [1, 0, 90], west: [-1, 0, -90], south: [0, 1, 180], north: [0, -1, 0] }[aim];
  if (side) return { tp: `${x + side[0]}.5 ${y + 1} ${z + side[1]}.5 ${side[2]} 64.7`, use: 'right_click' };
  const m = /^floor-south:(\d+(?:\.\d+)?)$/.exec(aim);
  if (m) { const d = Number(m[1]); return { tp: `${x}.5 ${y} ${z + 1 + d} 180 ${(Math.atan(1.12 / d) * 180 / Math.PI).toFixed(1)}`, use: 'right_click' }; }
  throw new Error('unknown aim ' + aim);
}
async function open(x, y, z, kind, aim = 'top') {
  if (!KINDS.includes(kind)) throw new Error('unknown kind ' + kind);
  if (await screenName()) { await cmd('close_screen'); await sleep(400); }
  const a = aimFor(x, y, z, aim);
  await chat(`/tp @s ${a.tp}`); await sleep(600);
  if (await screenName()) throw new Error('a screen is still open after the teleport');
  await cmd(a.use); await sleep(1500);
  for (let i = 0; i < 10 && (await screenName()) !== 'FluidDeviceScreen'; i++) await sleep(500);
  let id;
  try { id = await current([kind]); }
  catch (e) { const live = await widgets(); throw new Error(`opening ${kind} at ${x},${y},${z} (aim ${aim}): ${e.message}; screen ${live.screen}`); }
  log(`opened ${kind} at ${x},${y},${z}: page ${id.s.page}, ${id.widgets.length} widgets`);
  return id;
}
function shot(file) {
  if (!file) return;
  execFileSync('node', [path.join(__dirname, 'bridge.js'), 'shot', file], { stdio: 'inherit' });
}
/** Park the pointer on the frame's empty title strip, so no hover tooltip covers a row in the next screenshot. */
async function park() { const id = await current(); await clickAt(id.f, id.f.L + id.f.IW - 30, id.f.T + 14); }

const celsius = k => (Math.round((k - 273.15) * 1000) / 1000).toString();

async function configure(o) {
  const [x, y, z] = o.at, kind = o.kind;
  await open(x, y, z, kind, o.aim);
  // 2026-10-18 (d40 session): a device placed a moment ago has no published snapshot yet; its screen offers only the
  // Overview tab until the engine's next scheduled publication (about 5 s), so wait for the editable layout.
  for (let i = 0; ; i++) {
    let id = null; try { id = await current([kind]); } catch (e) { /* a layout in transition */ }
    if (id && id.s.canEdit) { if (i > 0) log(`editable after ${i} s`); break; }
    if (i >= 30) throw new Error(`${kind} at ${x},${y},${z}: no editable screen after 30 s`);
    await sleep(1000);
  }
  if (o.mixture) { if (kind !== 'GENERATOR') throw new Error('a mixture needs a generator'); await mixture(o.mixture); }
  const set = [];
  if (o.pressure !== undefined) set.push(['field:pressure', o.pressure]);
  if (o.temperatureK !== undefined) set.push(['field:temperature', celsius(o.temperatureK)]);
  if (o.flow !== undefined) set.push(['field:flow', o.flow]);
  if (o.head !== undefined) set.push(['field:head', o.head]);
  if (o.ratio !== undefined) set.push(['field:ratio', o.ratio]);
  if (o.diameter !== undefined) set.push(['field:diameter', o.diameter]);
  for (const [k, v] of set) await field(k, v, [kind]);
  if (o.shot) { await park(); shot(`${o.shot}-typed.png`); }
  await click('apply', [kind]);
  await sleep(o.waitMs ?? 9000);
  if (o.shot) shot(`${o.shot}-applied.png`);
  if (!o.keepOpen) { await cmd('close_screen'); await sleep(300); }
}

const VIEW = { organic: 'phase:0', aqueous: 'phase:1', vapor: 'phase:2', solids: 'phase:3' };
/** Screenshots of a device's views: "overview" (the screen as opened), a phase ("organic", "aqueous", "vapor",
 * "solids": its button on page 0), "outlets" (a tank's Connections by face), "connections" (a junction pipe's page),
 * "rail-end" (the rail scrolled to its end: needed at GUI scale 2, where a tank's status block lies below the rail). */
async function read(o) {
  const [x, y, z] = o.at, kind = o.kind, prefix = o.shot;
  await open(x, y, z, kind, o.aim);
  await sleep(o.settleMs ?? 1500);
  for (const view of o.views || ['overview']) {
    if (view === 'overview') { /* as opened */ }
    else if (VIEW[view]) { const id = await current([kind]); if (id.s.page !== 0) await click('tab:overview', [kind]); await click(VIEW[view], [kind]); }
    else if (view === 'outlets') await click('tab:outlets', [kind]);
    else if (view === 'connections') await click('tab:connections', [kind]);
    else if (view === 'rail-end') { const id = await current([kind]), g = geometry(id.f); await clickAt(id.f, id.f.L + g.rail + 14, id.f.T + id.f.IH - 54); }
    else throw new Error('unknown view ' + view);
    await sleep(400);
    await park().catch(() => {});
    shot(`${prefix}-${view}.png`);
  }
  if (!o.keepOpen) { await cmd('close_screen'); await sleep(300); }
}

// ---------------------------------------------------------------------------------------------------------------
// Offline self-test: the table against recorded widget dumps (main 0.8.0's in-game check, GUI scale 1 at 854x480)
// ---------------------------------------------------------------------------------------------------------------
function selftest(dir = path.join(__dirname, 'fixtures')) {
  let ok = 0, bad = 0;
  for (const name of fs.readdirSync(dir).filter(n => n.endsWith('.widgets.json')).sort()) {
    const d = JSON.parse(fs.readFileSync(path.join(dir, name), 'utf8'));
    if (d.screen !== 'FluidDeviceScreen') continue;
    try {
      const id = identify(d.widgets);
      const tag = `${id.s.kind}/page${id.s.page}${id.s.templates ? `/templates(${id.s.choices})` : ''}${id.s.page === 1 && !id.s.templates ? `/rows(${id.s.rows})` : ''}`;
      console.log(`PASS ${name.padEnd(42)} ${tag.padEnd(28)} frame ${id.f.L},${id.f.T} ${id.f.IW}x${id.f.IH}`);
      ok++;
    } catch (e) { console.log(`FAIL ${name}: ${e.message}`); bad++; }
  }
  // Generated frames (GUI scale 2 and 1 at 854x480, and 960x540): every kind's first view is told apart.
  for (const [W, H] of [[427, 240], [854, 480], [960, 540]]) {
    const IW = Math.max(300, Math.min(790, W - 16)), IH = Math.max(250, H - 16), f = { L: trunc((W - IW) / 2), T: trunc((H - IH) / 2), IW, IH };
    for (const kind of KINDS) for (const page of kind === 'GENERATOR' ? [0, 1, 2] : [0]) {
      const p = predict(f, { kind, canEdit: true, page, rows: 1, connections: 1 });
      const id = identify(p.map((w, i) => ({ i, c: w.c, x: w.x, y: w.y, w: w.w, h: w.h })), [kind]);
      if (id.s.kind !== kind || id.s.page !== page) { console.log(`FAIL generated ${W}x${H} ${kind} page ${page}`); bad++; } else ok++;
    }
  }
  console.log(`selftest: ${ok} pass, ${bad} fail`);
  if (bad) process.exit(1);
}

module.exports = { scaleFromOptions, setScale, pngSize, cmd, chat, widgets, screenName, current, identify, predict, frame, mixtureList, mixture, click, field, open, configure, read, shot, sleep, clickAt, scale, park, celsius, aimFor };

async function main() {
  const [op, ...rest] = process.argv.slice(2);
  switch (op) {
    case 'selftest': return selftest(rest[0]);
    case 'dump': { const id = await current(); console.log(JSON.stringify({ state: id.s, frame: id.f })); for (const w of id.widgets) console.log(`${w.i}\t${w.c}\t${w.x},${w.y} ${w.w}x${w.h}\t${w.key}\t${w.label ?? ''}${w.visible === false ? '\t(hidden)' : ''}`); return; }
    case 'labels': { const id = await current(); console.log(labels(id).join('\n')); return; }
    case 'click': return click(rest[0]);
    case 'field': return field(rest[0], rest[1]);
    case 'mixtures': return console.log(mixtureList().map((m, i) => `${i}\t${m.label}`).join('\n'));
    case 'mixture': return mixture(rest[0]);
    case 'open': return open(Number(rest[0]), Number(rest[1]), Number(rest[2]), rest[3], rest[4]);
    case 'configure': return configure(JSON.parse(rest[0]));
    case 'read': return read(JSON.parse(rest[0]));
    default: console.log(fs.readFileSync(__filename, 'utf8').split('\n').filter(l => l.startsWith('//')).join('\n'));
  }
}
if (require.main === module) main().catch(e => { console.error('[gen] FAILED:', e.message); if (e.live) console.error(JSON.stringify(e.live)); process.exit(1); });
