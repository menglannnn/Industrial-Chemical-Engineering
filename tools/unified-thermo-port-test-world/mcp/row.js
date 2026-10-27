#!/usr/bin/env node
// row.js: run one scenario row of the unified thermo port test world through the minecraft-mcp bridge: place the
// blocks with chat commands, configure devices through their screens by label (gen.js), wait on the engine's
// schedule, read back by screenshots of the device views and by the game log. Batch 2026-09-24-coolprop-low-temperature.
//
//   node row.js <rows/NN-name.json> [--from k] [--to k] [--out <dir>] [--dry]
//   node row.js setup [--out <dir>]          once per fresh world: control mode, game rules, noon, the floor
//   node row.js open-world <worldFolder>     from the title screen: Singleplayer, search the folder name, Play
//   node row.js reload <worldFolder> [--out <dir>]   pause, Save and Quit, reopen the world by its folder name
//
// A row file: {"id","title","blocks":{name:{"at":[x,y,z],"block":"generator|reservoir|pipe|void|pump|compressor|
// valve|glass","facing":"south","aim":{"stand":[x,y,z]}}},"steps":[...]}. Steps ("op"):
//   place {ids}                  /setblock each block (actuators with their facing); glass stands included
//   set {id, mixture, pressure, temperatureK, flow, head, ratio, diameter}   configure through the device screen
//   wait {s}                     online seconds (the engine publishes about every 5 s)
//   read {ids, views, tag}       screenshots of each device's views (gen.js read)
//   remove {ids}                 /setblock air (the device leaves the network; nothing drops)
//   break {ids}                  /setblock ... destroy, a screenshot, each dropped stack printed to chat (data get), then cleared
//   mark | log {grep, tag}       remember the log's length | print the lines added since the mark that match grep
//   note {text}                  printed as is
//   reload {world}               Save and Quit, then reopen the world (world: the save folder; default env MC_WORLD)
// Shots: <out>/<row id>/<step>-<id>-<tag>-<view>.png. The engine numbers each read is compared with are in
// documentation/2026-09-24-coolprop-low-temperature/TEST_WORLD_PLAN.md.
'use strict';
const fs = require('fs');
const path = require('path');
const gen = require('./gen.js');
const { cmd, chat, sleep } = gen;

const WORKTREE = process.env.MC_WORKTREE || 'D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0';
const GAME_DIR = process.env.MC_GAME_DIR || path.join(WORKTREE, 'run', 'mcp-client');
const LOG = path.join(GAME_DIR, 'logs', 'latest.log');
const BLOCK = { generator: 'icheme:fluid_generator', reservoir: 'icheme:fluid_reservoir', pipe: 'icheme:fluid_pipe',
  void: 'icheme:fluid_void', pump: 'icheme:fluid_pump', compressor: 'icheme:fluid_compressor',
  valve: 'icheme:pressure_control_valve', glass: 'minecraft:glass' };
const KIND = { generator: 'GENERATOR', reservoir: 'RESERVOIR', pipe: 'PIPE', void: 'VOID', pump: 'PUMP', compressor: 'COMPRESSOR', valve: 'VALVE' };
const log = (...a) => console.log('[row]', ...a);

function args() {
  const a = process.argv.slice(2), o = { pos: [] };
  for (let i = 0; i < a.length; i++) {
    if (a[i] === '--from') o.from = Number(a[++i]); else if (a[i] === '--to') o.to = Number(a[++i]);
    else if (a[i] === '--out') o.out = a[++i]; else if (a[i] === '--dry') o.dry = true; else o.pos.push(a[i]);
  }
  o.out = path.resolve(o.out || path.join(__dirname, '..', 'shots'));
  return o;
}
let DRY = false;
async function run(line) { if (DRY) { log('[dry] ' + line); return; } await chat(line); log(line); }

/** Where to stand to use a block: on its own top (looking down, use_item), or on the top of the block "stand" names,
 * looking at the target's centre (right_click). A pipe's top is its core, 11/16 of a block up. */
function aim(rowBlocks, b) {
  const [x, y, z] = b.at;
  if (!b.aim || b.aim === 'top') return { tp: `${x}.5 ${y + 1} ${z}.5 0 89`, use: 'use_item' };
  const [sx, sy, sz] = b.aim.stand;
  const standBlock = Object.values(rowBlocks).find(o => o.at[0] === sx && o.at[1] === sy && o.at[2] === sz);
  if (!standBlock && sy !== 100) throw new Error(`nothing to stand on at ${sx},${sy},${sz} (the floor is y = 100)`);
  const feet = sy + (standBlock && standBlock.block === 'pipe' ? 0.6875 : 1), eye = feet + 1.62;
  const dx = (x + 0.5) - (sx + 0.5), dz = (z + 0.5) - (sz + 0.5), dy = (y + 0.5) - eye;
  const yaw = Math.atan2(-dx, dz) * 180 / Math.PI, pitch = Math.atan2(-dy, Math.hypot(dx, dz)) * 180 / Math.PI;
  return { tp: `${sx}.5 ${sy + 1} ${sz}.5 ${yaw.toFixed(1)} ${pitch.toFixed(1)}`, use: 'right_click' };
}
async function openBlock(row, id) {
  const b = row.blocks[id], kind = KIND[b.block];
  if (!kind) throw new Error(`${id} (${b.block}) has no screen`);
  const a = aim(row.blocks, b);
  if (DRY) { log(`[dry] open ${id} ${kind}: /tp @s ${a.tp}; ${a.use}`); return; }
  if (await gen.screenName()) { await cmd('close_screen'); await sleep(400); }
  await chat(`/tp @s ${a.tp}`); await sleep(700);
  await cmd(a.use); await sleep(1500);
  for (let i = 0; i < 10 && (await gen.screenName()) !== 'FluidDeviceScreen'; i++) await sleep(500);
  return gen.current([kind]);
}

/** The device configuration through its screen, in the order the screen needs it (mixture first: loading one
 * rebuilds the page; typed rail values survive it), then Apply, one engine bucket and a screenshot. */
async function set(row, step, dir, n) {
  const b = row.blocks[step.id], kind = KIND[b.block];
  if (DRY) { log(`[dry] set ${step.id} ${JSON.stringify(step)}`); return; }
  await openBlock(row, step.id);
  // 2026-10-18 (d40 session): a device placed a moment ago has no delivered state yet (FluidDeviceMenu.clientData is
  // null until its first bucket), and its screen offers only the Overview tab until the engine's next scheduled
  // publication (about 5 s); wait for the editable layout before configuring it.
  for (let i = 0; ; i++) {
    let id = null; try { id = await gen.current([kind]); } catch (e) { /* a layout in transition */ }
    if (id && id.s.canEdit) { if (i > 0) log(`${step.id}: editable after ${i} s`); break; }
    if (i >= 30) throw new Error(`${step.id}: no editable screen after 30 s`);
    await sleep(1000);
  }
  if (step.mixture) await gen.mixture(step.mixture);
  const fields = [['pressure', 'field:pressure'], ['flow', 'field:flow'], ['head', 'field:head'], ['ratio', 'field:ratio'], ['diameter', 'field:diameter']];
  for (const [k, key] of fields) if (step[k] !== undefined) await gen.field(key, step[k], [kind]);
  if (step.temperatureK !== undefined) await gen.field('field:temperature', gen.celsius(step.temperatureK), [kind]);
  const prefix = path.join(dir, `${String(n).padStart(2, '0')}-${step.id}-set`);
  if (!step.noShot) { await gen.park(); gen.shot(`${prefix}-typed.png`); }
  await gen.click('apply', [kind]);
  await sleep(step.waitMs ?? 9000);
  if (!step.noShot) gen.shot(`${prefix}-reply.png`);
  await cmd('close_screen'); await sleep(300);
}

async function read(row, step, dir, n) {
  for (const id of step.ids) {
    const b = row.blocks[id], kind = KIND[b.block];
    if (DRY) { log(`[dry] read ${id} ${(step.views || ['overview']).join(',')}`); continue; }
    await openBlock(row, id); await sleep(step.settleMs ?? 1500);
    const prefix = path.join(dir, `${String(n).padStart(2, '0')}-${id}-${step.tag || 'read'}`);
    for (const view of step.views || ['overview']) {
      const map = { organic: 'phase:0', aqueous: 'phase:1', vapor: 'phase:2', solids: 'phase:3' };
      if (map[view]) { const cur = await gen.current([kind]); if (cur.s.page !== 0) await gen.click('tab:overview', [kind]); await gen.click(map[view], [kind]); }
      else if (view === 'outlets') {
        // 2026-10-18 (d40 session): R3's first in-game reads showed page 0 after this click (not reproduced by hand);
        // check the page and click once more when the Connections by face page is not up.
        await gen.click('tab:outlets', [kind]); await sleep(300);
        const cur = await gen.current([kind]).catch(() => null);
        if (!cur || cur.s.page !== 4) { log(`${id}: the outlets page is not up (page ${cur && cur.s.page}); clicking again`); await gen.click('tab:outlets', [kind]); }
      }
      else if (view === 'connections') await gen.click('tab:connections', [kind]);
      else if (view !== 'overview') throw new Error('unknown view ' + view);
      await sleep(400); await gen.park().catch(() => {});
      gen.shot(`${prefix}-${view}.png`);
    }
    await cmd('close_screen'); await sleep(300);
  }
}

let mark = 0;
function logSince(grep) {
  if (!fs.existsSync(LOG)) return [`(no log at ${LOG})`];
  const text = fs.readFileSync(LOG, 'utf8').slice(mark), re = new RegExp(grep);
  return text.split(/\r?\n/).filter(l => re.test(l));
}

async function setup(o) {
  DRY = !!o.dry;
  if (!DRY) { console.log(JSON.stringify(await cmd('enter_control_mode'))); await sleep(500); }
  for (const line of ['/gamerule doDaylightCycle false', '/gamerule doMobSpawning false', '/gamerule doWeatherCycle false', '/time set noon',
    '/fill 16 100 16 70 100 112 minecraft:white_concrete', '/tp @s 18.5 101 18.5 -45 30']) await run(line);
}

// Menus (pause, title, world list): not device screens, so the GUI scale comes from options.txt and a screenshot
// (gen.scaleFromOptions), and each button is told by its class, width and place; every step checks the screen's class
// and takes a screenshot before it acts.
const live = async () => gen.widgets();
async function expect(screen, tries = 40) {
  for (let i = 0; i < tries; i++) { const l = await live(); if (l.screen === screen) return l; await sleep(1000); }
  throw new Error(`waited for ${screen}, the screen is ${(await live()).screen}`);
}
const press = async w => gen.clickAt(null, w.x + Math.trunc(w.w / 2), w.y + Math.trunc(w.h / 2));
function shooter(o, what) {
  const dir = path.join(o.out, what); fs.mkdirSync(dir, { recursive: true });
  const stamp = Date.now(); return name => gen.shot(path.join(dir, `${stamp}-${name}.png`));
}
/** Pause, then Save and Quit to Title: the lowest of the pause screen's two full-width (204 px) buttons. */
async function quitToTitle(shotAt) {
  await gen.scaleFromOptions();
  if (await gen.screenName()) { await cmd('close_screen'); await sleep(400); }
  await cmd('pause_game'); const l = await expect('PauseScreen', 10); shotAt('1-pause');
  const quit = l.widgets.filter(w => w.c === 'Button' && w.w === 204).sort((a, b) => b.y - a.y)[0];
  if (!quit) throw new Error('no full-width button on the pause screen: ' + JSON.stringify(l.widgets));
  await press(quit); await expect('TitleScreen', 90); shotAt('2-title');
}
/** From the title screen: Singleplayer (its highest 200 px button), the world list's search box typed with the save
 * folder's name so only that world is listed (M4's way), its entry, Play Selected World (the upper-left 150 px button). */
async function openWorld(world, shotAt) {
  await cmd('enter_control_mode'); await sleep(500);
  await gen.scaleFromOptions();
  let l = await expect('TitleScreen', 30);
  const single = l.widgets.filter(w => w.c === 'Button' && w.w === 200).sort((a, b) => a.y - b.y)[0];
  await press(single); await expect('SelectWorldScreen', 20); await sleep(1000);
  // 2026-10-18 (d40 session): the bridge's enumerate_widgets does not list the world list's EditBox, so fall back to
  // its vanilla place, 200 x 20 px centred at the top (x = width / 2 - 100, y = 22; the list widget spans the width).
  const shown = await live(), listed = shown.widgets.find(w => /SelectionList/.test(w.c));
  const search = shown.widgets.find(w => w.c === 'EditBox') || (listed && { c: 'EditBox', x: Math.trunc(listed.w / 2) - 100, y: 22, w: 200, h: 20 });
  if (!search) throw new Error('no search box on the world list');
  await press(search); await sleep(300); await cmd('type_text', { text: world }); await sleep(1200); shotAt('3-filtered');
  l = await live();
  const list = l.widgets.find(w => /SelectionList/.test(w.c));
  if (!list) throw new Error('no world list widget: ' + JSON.stringify(l.widgets));
  await gen.clickAt(null, list.x + Math.trunc(list.w / 2), list.y + 4 + 18); await sleep(600); shotAt('4-selected');
  l = await live();
  const play = l.widgets.filter(w => w.c === 'Button' && w.w === 150).sort((a, b) => a.y - b.y || a.x - b.x)[0];
  await press(play);
  for (let i = 0; i < 120; i++) { await sleep(1000); if (!(await gen.screenName())) break; }
  if (await gen.screenName()) throw new Error('the world did not load within 120 s');
  await sleep(3000); shotAt('5-in-world');
  log('in the world; the log since the mark:'); for (const line of logSince('fluid_world|fluid_island|Exception|crash')) console.log('  ' + line);
}
async function reload(world, o) { const shotAt = shooter(o, 'reload'); await quitToTitle(shotAt); await openWorld(world, shotAt); }

async function main() {
  const o = args();
  if (o.pos[0] === 'setup') return setup(o);
  if (o.pos[0] === 'reload') { mark = fs.existsSync(LOG) ? fs.statSync(LOG).size : 0; return reload(o.pos[1], o); }
  if (o.pos[0] === 'open-world') { mark = fs.existsSync(LOG) ? fs.statSync(LOG).size : 0; return openWorld(o.pos[1], shooter(o, 'open-world')); }
  DRY = !!o.dry;
  const row = JSON.parse(fs.readFileSync(o.pos[0], 'utf8'));
  const dir = path.join(o.out, row.id); fs.mkdirSync(dir, { recursive: true });
  log(`${row.id}: ${row.title}`);
  if (!DRY) mark = fs.existsSync(LOG) ? fs.statSync(LOG).size : 0;
  const steps = row.steps;
  for (let n = o.from ?? 0; n <= (o.to ?? steps.length - 1); n++) {
    const s = steps[n]; log(`step ${n}: ${s.op} ${s.id || (s.ids || []).join(',') || s.s || s.text || ''}`);
    switch (s.op) {
      case 'place': for (const id of s.ids) { const b = row.blocks[id]; await run(`/setblock ${b.at.join(' ')} ${BLOCK[b.block]}${b.facing ? `[facing=${b.facing}]` : ''}`); } await sleep(500); break;
      case 'set': await set(row, s, dir, n); break;
      case 'wait': if (DRY) log(`[dry] wait ${s.s} s`); else await sleep(s.s * 1000); break;
      case 'read': await read(row, s, dir, n); break;
      case 'remove': for (const id of s.ids) await run(`/setblock ${row.blocks[id].at.join(' ')} minecraft:air`); break;
      case 'break': for (const id of s.ids) {
        // Destroy as a player's break would, print every dropped stack's data to chat (the log keeps it), then clear them.
        const at = row.blocks[id].at.join(' ');
        await run(`/setblock ${at} minecraft:air destroy`); await sleep(1500);
        if (!DRY) gen.shot(path.join(dir, `${String(n).padStart(2, '0')}-${id}-broken.png`));
        await run(`/execute positioned ${at} as @e[type=item,distance=..4] run data get entity @s Item`); await sleep(800);
        await run(`/execute positioned ${at} run kill @e[type=item,distance=..4]`); await sleep(500);
      } break;
      case 'mark': if (!DRY) mark = fs.existsSync(LOG) ? fs.statSync(LOG).size : 0; break;
      case 'log': for (const line of DRY ? [`[dry] grep ${s.grep}`] : logSince(s.grep)) console.log(`  [log ${s.tag || ''}] ${line}`); break;
      case 'note': console.log(`  [note] ${s.text}`); break;
      case 'reload': { const world = s.world || process.env.MC_WORLD; if (!world) throw new Error('reload needs MC_WORLD (the save folder name)'); if (DRY) log(`[dry] reload ${world}`); else await reload(world, o); break; }
      default: throw new Error('unknown op ' + s.op);
    }
  }
  if (!DRY) await cmd('close_screen').catch(() => {});
}
main().catch(e => { console.error('[row] FAILED:', e.message); if (e.live) console.error(JSON.stringify(e.live)); process.exit(1); });
