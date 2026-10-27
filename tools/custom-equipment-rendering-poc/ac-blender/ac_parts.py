"""Air cooler (forced-draft ACHE) parts for CreateChemE, scripted in Blender 5.2.

Batch 2026-10-15-custom-equipment-rendering, owner decision "Blender parts, scripted".

Builds the fan rotor, the fan ring with its guard and the fan motor as closed low-poly solids, paints their textures and
the three tileable sprites (fins, header, plenum) procedurally, exports the parts as OBJ (+Y up, triangles, UVs,
normals), checks the exported files (closed, consistently wound, outward, UVs in 0..1), and renders previews of each
part and of an assembled bay mock-up.

Run (from any folder):
  "C:/Program Files/Blender Foundation/Blender 5.2/blender.exe" -b --factory-startup --python ac_parts.py -- [--out DIR] [--no-render]

Everything is deterministic: no random module, textures use the integer hash of VesselTextures.java.
Geometry is written in game coordinates (metres, +Y up, x east, z south) and converted to Blender (z up) only when
a mesh is created: Blender (x, y, z) = game (x, -z, y). The OBJ exporter (forward -Z, up Y) maps it back exactly.
"""
import bpy
import json
import math
import os
import struct
import sys
import zlib

import numpy as np
from mathutils import Vector

HERE = os.path.dirname(os.path.abspath(__file__))
BLENDER_TAG = bpy.app.version_string


def parse_args():
    argv = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
    out, render = os.path.join(HERE, 'out'), True
    i = 0
    while i < len(argv):
        if argv[i] == '--out':
            out, i = argv[i + 1], i + 2
        elif argv[i] == '--no-render':
            render, i = False, i + 1
        else:
            raise SystemExit('unknown argument ' + argv[i])
    return os.path.abspath(out), render


# ----------------------------------------------------------------------------------------------------------------
# Textures: same noise as tools/custom-equipment-rendering-poc/textures/VesselTextures.java (muted, slightly noisy)
# ----------------------------------------------------------------------------------------------------------------

M64 = (1 << 64) - 1


def jhash(x, y, seed):
    """Java VesselTextures.hash, bit for bit (64-bit wrap, unsigned shifts)."""
    h = ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) & M64
    h ^= h >> 13
    h = (h * 0x5bd1e995) & M64
    h ^= h >> 15
    return (h & 0xffff) / 65535.0


def plate(x, y, size, seed):
    """Tileable plate surface: per-pixel grain plus a soft mottle periodic in the sprite size."""
    grain = (jhash(x, y, seed) - .5) * 12
    mottle = 3 * math.sin(2 * math.pi * (2 * x + y) / size + seed) + 2 * math.sin(2 * math.pi * (x - 3 * y) / size + 2 * seed)
    return grain + mottle


def sprite(size, fn):
    """Paint a size x size RGB sprite; fn(x, y) -> (r, g, b); row 0 is the top row of the PNG."""
    a = np.zeros((size, size, 3), dtype=np.uint8)
    for y in range(size):
        for x in range(size):
            a[y, x] = [max(0, min(255, int(round(c)))) for c in fn(x, y)]
    return a


def sh(base, s):
    return (base[0] + s, base[1] + s, base[2] + s)


def in_rect(rect, x, y):
    x0, y0, x1, y1 = rect
    return x0 <= x < x1 and y0 <= y < y1


def write_png(path, a):
    """Minimal deterministic PNG writer (8-bit RGB, no metadata chunks)."""
    h, w = a.shape[:2]
    raw = b''.join(b'\x00' + np.ascontiguousarray(a[y]).tobytes() for y in range(h))

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


# Colours (sRGB 0..255). Tinted sprites stay light and neutral: the mod multiplies them by a paint colour.
FIN = (198, 202, 204)       # aluminium fins (not tinted)
HEADER = (198, 200, 202)    # header plate (tinted)
PLENUM = (194, 197, 200)    # plenum sheet (tinted)
BLADE = (184, 188, 192)     # aluminium blades
HUB = (74, 79, 86)          # dark hub
RING = (208, 210, 212)      # fan ring (tinted by the mod)
GUARD = (180, 183, 186)     # galvanised guard wire (same material as the ring)
MOTOR = (86, 102, 122)      # medium blue-grey motor frame
MOTOR_FLANGE = (74, 88, 106)
STEEL = (186, 188, 190)     # bright shaft steel
GLAND = (58, 60, 64)

# UV regions in pixels (x0, y0, x1, y1), y down from the top row; shared by the painters and the UV mappers.
FAN_RECTS = {
    'blade_top': (0, 0, 28, 40), 'blade_bottom': (28, 0, 56, 40), 'blade_le': (56, 0, 60, 40),
    'blade_te': (60, 0, 64, 40), 'blade_cap': (56, 40, 64, 44), 'hub_side': (0, 44, 32, 64), 'hub_cap': (32, 44, 52, 64),
}
RING_RECTS = {'outer': (0, 0, 64, 20), 'inner': (0, 20, 64, 40), 'flange': (0, 40, 64, 46), 'lip': (0, 46, 64, 48),
              'guard': (0, 48, 64, 64)}
MOTOR_RECTS = {
    'body': (0, 0, 64, 28), 'cowl': (0, 28, 64, 34), 'fin': (0, 34, 32, 40), 'flange_side': (32, 34, 64, 40),
    'flange_cap': (0, 40, 24, 64), 'end_cap': (24, 40, 48, 64), 'tbox': (48, 40, 64, 48), 'tlid': (48, 48, 56, 56),
    'gland': (56, 48, 64, 56), 'shaft': (48, 56, 64, 64),
}


def tex_fins():
    """1 m x 1 m of finned tubes seen from below or above, 32 px/m. Tube axis along V (image columns); tube pitch
    2 px = 6.25 cm along U: crest column (fin discs edge-on) then the shadowed gap to the next tube. Fins are the fine
    lines across U (every other row a brighter fin edge)."""
    def px(x, y):
        s = plate(x, y, 32, 21) * .45 + (jhash(x // 2, 0, 22) - .5) * 4
        s += 7 if x % 2 == 0 else -13
        s += 4 if y % 2 == 0 else -2
        return sh(FIN, s)
    return sprite(32, px)


def tex_header():
    """0.5 m x 0.5 m of plug-type header plate, 64 px/m (tinted). One row of hex plugs at rows 14-16, one plug per tube
    (pitch 4 px = 6.25 cm, plug 3 px), lit from the top left; plate weld bead on row 0, its shadow on row 1."""
    def px(x, y):
        s = plate(x, y, 32, 23) * .7
        cx, cy = x % 4, y - 14
        if 1 <= cx <= 3 and 0 <= cy <= 2:
            dx, dy = cx - 2, cy - 1
            s += -4 if (dx != 0 and dy != 0) else 14 - 6 * (dx + dy)   # chamfered corners read as a hex head
        elif (cx == 0 and 1 <= cy <= 3) or (cy == 3 and 2 <= cx <= 3):
            s -= 12                                                      # plug shadow, bottom right
        if y == 0:
            s += 12 + (4 if x % 2 == 0 else -2)
        elif y == 1:
            s -= 14
        return sh(HEADER, s)
    return sprite(32, px)


def tex_plenum():
    """1 m x 1 m of plenum side sheet, 32 px/m (tinted): faint lap seams on column 0 and row 0, a vertical stiffener
    rib on columns 15-18 (lit edge, face, shadow)."""
    def px(x, y):
        s = plate(x, y, 32, 24) * .7
        s += {0: 7, 1: -8}.get(x, 0)
        s += {0: 5, 1: -6}.get(y, 0)
        s += {15: 14, 16: 4, 17: -16, 18: -6}.get(x, 0)
        return sh(PLENUM, s)
    return sprite(32, px)


def tex_fan():
    r = FAN_RECTS

    def px(x, y):
        if in_rect(r['blade_top'], x, y) or in_rect(r['blade_bottom'], x, y):
            top = in_rect(r['blade_top'], x, y)
            x0, y0 = (r['blade_top'] if top else r['blade_bottom'])[:2]
            lx, ly = x - x0, y - y0                  # lx: chord, 0 = leading edge; ly: span, 0 = tip
            s = plate(x, y, 64, 31) * .4 + (jhash(lx, 0, 32) - .5) * 6 + (0 if top else -14)
            s += 8 if lx == 0 else (-6 if lx == 27 else 0)
            s += 5 if ly <= 1 else (-12 if ly >= 36 else 0)  # tip highlight, root clamp zone
            return sh(BLADE, s)
        if in_rect(r['blade_le'], x, y):
            return sh(BLADE, 10 + plate(x, y, 64, 33) * .3)
        if in_rect(r['blade_te'], x, y) or in_rect(r['blade_cap'], x, y):
            return sh(BLADE, -4 + plate(x, y, 64, 34) * .3)
        if in_rect(r['hub_side'], x, y):
            ly = y - r['hub_side'][1]
            s = plate(x, y, 64, 35) * .6 + (10 if ly < 4 else 0) + (8 if ly == 4 else 0) - (6 if ly == 19 else 0)
            return sh(HUB, s)
        if in_rect(r['hub_cap'], x, y):
            lx, ly = x - r['hub_cap'][0] - 9.5, y - r['hub_cap'][1] - 9.5
            d = math.hypot(lx, ly)
            s = plate(x, y, 64, 36) * .5 + (8 if d > 8.5 else 0)
            for k in range(6):                       # six clamp bolts on a 6 px circle
                bx, by = 6 * math.cos(k * math.pi / 3), 6 * math.sin(k * math.pi / 3)
                if abs(lx - bx) < .75 and abs(ly - by) < .75:
                    s += 46
                elif abs(lx - bx - 1) < .75 and abs(ly - by - 1) < .75:
                    s -= 18
            if d < 2:
                return (34, 36, 40)                  # shaft bore
            return sh(HUB, s)
        return sh(BLADE, plate(x, y, 64, 37) * .3)   # unused area
    return sprite(64, px)


def tex_ring():
    r = RING_RECTS

    def px(x, y):
        if in_rect(r['outer'], x, y):
            ly = y - r['outer'][1]
            s = plate(x, y, 64, 41) * .6 + {0: 7, 1: -9}.get(x % 16, 0)   # four bolted ring sections
            if x % 16 in (2, 15) and ly % 6 == 3:
                s -= 14                                               # section bolts
            s += -12 if ly == 0 else (6 if ly == 19 else 0)           # shadow under the flange, rolled bottom edge
            return sh(RING, s)
        if in_rect(r['inner'], x, y):
            return sh(RING, -5 + plate(x, y, 64, 42) * .5 + {0: 4, 1: -5}.get(x % 16, 0))
        if in_rect(r['flange'], x, y):
            ly = y - r['flange'][1]
            s = plate(x, y, 64, 43) * .5
            if ly >= 3 and x % 4 == 1 and ly == 4:
                s -= 20                                               # bolt holes in the top flange face
            elif ly >= 3 and x % 4 == 2 and ly == 4:
                s += 6
            return sh(RING, s + (-6 if ly < 2 else 0))
        if in_rect(r['lip'], x, y):
            return sh(RING, 4 + plate(x, y, 64, 44) * .4)
        return sh(GUARD, plate(x, y, 64, 45) * .8 + (10 if jhash(x, y, 46) > .85 else 0))
    return sprite(64, px)


def tex_motor():
    r = MOTOR_RECTS

    def px(x, y):
        if in_rect(r['body'], x, y):
            ly = y - r['body'][1]
            s = plate(x, y, 64, 51) * .6 + (-14 if ly == 22 else (8 if ly == 23 else 0))  # fan-cowl joint
            if ly == 0:
                s -= 10
            return sh(MOTOR, s)
        if in_rect(r['cowl'], x, y):
            return sh(MOTOR, -8 + plate(x, y, 64, 52) * .5 + (-22 if (x % 3 != 0 and 1 <= y - r['cowl'][1] <= 4) else 0))
        if in_rect(r['fin'], x, y):
            return sh(MOTOR, 6 + plate(x, y, 64, 53) * .5)
        if in_rect(r['flange_side'], x, y):
            return sh(MOTOR_FLANGE, 6 + plate(x, y, 64, 54) * .5)
        if in_rect(r['flange_cap'], x, y):
            lx, ly = x - r['flange_cap'][0] - 11.5, y - r['flange_cap'][1] - 11.5
            s = plate(x, y, 64, 55) * .5
            for k in range(8):                        # eight flange bolt holes on the 0.36 m circle
                bx, by = 10 * math.cos((k + .5) * math.pi / 4), 10 * math.sin((k + .5) * math.pi / 4)
                if abs(lx - bx) < .75 and abs(ly - by) < .75:
                    s -= 34
            return sh(MOTOR_FLANGE, s)
        if in_rect(r['end_cap'], x, y):
            lx, ly = x - r['end_cap'][0] - 11.5, y - r['end_cap'][1] - 11.5
            d = math.hypot(lx, ly)
            s = plate(x, y, 64, 56) * .5 - 8
            if 3 <= d <= 10 and int(d) % 2 == 1:
                s -= 24                               # fan-cowl intake grille
            return sh(MOTOR, s)
        if in_rect(r['tbox'], x, y):
            return sh(MOTOR, plate(x, y, 64, 57) * .5 + 4)
        if in_rect(r['tlid'], x, y):
            lx, ly = x - r['tlid'][0], y - r['tlid'][1]
            if 2 <= lx <= 5 and 2 <= ly <= 4:   # nameplate, about 85 x 65 mm on the 0.17 m lid
                return sh((196, 198, 194), (-30 if ly == 3 and lx % 2 == 0 else 0) + plate(x, y, 64, 58) * .3)
            return sh(MOTOR, 6 + plate(x, y, 64, 59) * .5)
        if in_rect(r['gland'], x, y):
            return sh(GLAND, plate(x, y, 64, 60) * .4)
        return sh(STEEL, plate(x, y, 64, 61) * .4)
    return sprite(64, px)


# ----------------------------------------------------------------------------------------------------------------
# Geometry builder (game coordinates). Every solid is one closed shell; its winding is fixed outward by the sign of
# its volume. Faces carry a smoothing group: 0 = flat, otherwise faces of one group share vertex normals.
# ----------------------------------------------------------------------------------------------------------------

def rect_uv(size, rect, a, b, inset=.5):
    """(a, b) in [0, 1]^2 inside a pixel rect (b = 0 at the rect's top row) -> OBJ/Blender UV (v = 0 at the bottom)."""
    x0, y0, x1, y1 = rect
    x = x0 + inset + a * (x1 - x0 - 2 * inset)
    y = y0 + inset + b * (y1 - y0 - 2 * inset)
    return (x / size, 1.0 - y / size)


class Part:
    def __init__(self, name):
        self.name = name
        self.verts, self.tris, self.uvs, self.groups = [], [], [], []
        self.shells = 0
        self._next_group = 1

    def group(self):
        g = self._next_group
        self._next_group += 1
        return g

    def vert(self, p):
        self.verts.append(tuple(float(c) for c in p))
        return len(self.verts) - 1

    def tri(self, a, b, c, ua, ub, uc, g):
        self.tris.append((a, b, c))
        self.uvs.append((ua, ub, uc))
        self.groups.append(g)

    def quad(self, a, b, c, d, ua, ub, uc, ud, g):
        self.tri(a, b, c, ua, ub, uc, g)
        self.tri(a, c, d, ua, uc, ud, g)

    def close_shell(self, first_tri):
        """Flip the shell's triangles if its signed volume is negative (outward normals, CCW seen from outside)."""
        vol = 0.0
        for a, b, c in self.tris[first_tri:]:
            vol += Vector(self.verts[a]).dot(Vector(self.verts[b]).cross(Vector(self.verts[c])))
        if vol < 0:
            for i in range(first_tri, len(self.tris)):
                a, b, c = self.tris[i]
                ua, ub, uc = self.uvs[i]
                self.tris[i], self.uvs[i] = (a, c, b), (ua, uc, ub)
        self.shells += 1


def loft(part, sections, uv, group, cap_uv=None, wrap_s=False, xf=None):
    """Closed solid through `sections` (each a convex loop of k points). Faces between section s and s+1, strip j
    (point j to j+1). uv(s, j, fs, fj) gives the UV of corner (s, j) on face (fs, fj), indices unwrapped (s may be S,
    j may be k). group(fs, fj) gives the smoothing group. Ends are capped with fans unless wrap_s (a ring)."""
    first = len(part.tris)
    S, k = len(sections), len(sections[0])
    idx = [[part.vert(xf(p) if xf else p) for p in sec] for sec in sections]
    for s in range(S if wrap_s else S - 1):
        for j in range(k):
            a, b = idx[s][j], idx[s][(j + 1) % k]
            c, d = idx[(s + 1) % S][(j + 1) % k], idx[(s + 1) % S][j]
            part.quad(a, b, c, d, uv(s, j, s, j), uv(s, j + 1, s, j), uv(s + 1, j + 1, s, j), uv(s + 1, j, s, j),
                      group(s, j))
    if not wrap_s:
        for end, s in ((0, 0), (1, S - 1)):
            ring = idx[s]
            for i in range(1, k - 1):
                order = (0, i + 1, i) if end == 0 else (0, i, i + 1)
                part.tri(*(ring[o] for o in order), *(cap_uv(end, o, sections[s][o]) for o in order), 0)
    part.close_shell(first)


def lathe(part, profile, n, band_uv, cap_uv=None, smooth=None, centre=(0.0, 0.0), phase=0.0, wrap=False):
    """Solid of revolution about the vertical line through `centre` (x, z). profile: (radius, y) points; an open
    polyline gets flat end caps, a closed loop (wrap) gives a ring. smooth[i] marks profile segment i as a round
    surface. band_uv(s, j, fs) -> UV of corner (profile point s, angle j) on band fs."""
    cx, cz = centre
    sections = [[(cx + r * math.cos(phase + 2 * math.pi * j / n), y, cz - r * math.sin(phase + 2 * math.pi * j / n))
                 for j in range(n)] for r, y in profile]
    segs = len(profile) if wrap else len(profile) - 1
    groups = [part.group() if (smooth is None or smooth[i]) else 0 for i in range(segs)]
    loft(part, sections, lambda s, j, fs, fj: band_uv(s, j, fs), lambda fs, fj: groups[fs], cap_uv, wrap_s=wrap)


def box(part, lo, hi, face_uv, xf=None):
    """Flat-shaded box. face_uv(face, a, b) with face in -z +x +z -x -y +y and (a, b) in [0, 1]^2 (b = 0 at the top
    for side faces)."""
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    loop = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
    sections = [[(x, y, z) for x, z in loop] for y in (y0, y1)]
    sides = ['-z', '+x', '+z', '-x']
    corners = [(0, 0), (1, 0), (1, 1), (0, 1)]
    loft(part, sections, lambda s, j, fs, fj: face_uv(sides[fj], j - fj, 1 - s), lambda fs, fj: 0,
         lambda end, i, p: face_uv('-y' if end == 0 else '+y', *corners[i]), xf=xf)


def rot_y(angle, offset=(0.0, 0.0, 0.0)):
    """Positive rotation about +Y (right hand: +X turns towards -Z), then a translation."""
    c, s = math.cos(angle), math.sin(angle)
    return lambda p: (c * p[0] + s * p[2] + offset[0], p[1] + offset[1], -s * p[0] + c * p[2] + offset[2])


# ----------------------------------------------------------------------------------------------------------------
# Parts
# ----------------------------------------------------------------------------------------------------------------

FAN_TIP = 0.49                 # blade tip radius of the 1.0 m rotor (ring inner radius 0.515)
GUARD_R0, GUARD_R1 = 0.11, 0.528  # radial bar extent; central opening for the motor


def build_fan():
    """Axial rotor, D = 1.0 m, axis +Y, 4 twisted blades. Turning positively about +Y (counter-clockwise seen from
    above) it pushes air towards +Y: leading edges point along the motion and sit lower than the trailing edges."""
    p = Part('ac_fan')
    R = FAN_RECTS
    radii = [0.07, 0.15, 0.24, 0.33, 0.41, FAN_TIP]       # root section buried in the hub
    span = [(r - radii[0]) / (radii[-1] - radii[0]) for r in radii]

    def pitch(r):                                         # 25 deg at the hub (r = 0.1) to 12 deg at the tip
        return math.radians(25 + (12 - 25) * (r - 0.10) / (FAN_TIP - 0.10))

    def chord(r):
        return 0.10 if r < 0.1 else 0.14

    def thick(r):
        return 0.022 - 0.006 * (r - radii[0]) / (radii[-1] - radii[0])

    blade_rects = ['blade_top', 'blade_te', 'blade_bottom', 'blade_le']
    for b in range(4):
        phi0 = b * math.pi / 2
        sections = []
        for r in radii:
            th, ch, t = pitch(r), chord(r), thick(r)
            sec = []
            # corners: leading edge up, trailing edge up, trailing edge low, leading edge low
            for c, n in ((ch / 2, t / 2), (-ch / 2, t / 2), (-ch / 2, -t / 2), (ch / 2, -t / 2)):
                phi = phi0 + c * math.cos(th) / r             # chord wrapped on the cylinder of radius r
                em = Vector((-math.sin(phi), 0, -math.cos(phi)))  # direction of motion
                er = Vector((math.cos(phi), 0, -math.sin(phi)))
                y = -c * math.sin(th)                         # leading edge (c > 0) lower
                nrm = em * math.sin(th) + Vector((0, 1, 0)) * math.cos(th)
                pt = er * r + Vector((0, y, 0)) + nrm * n
                sec.append(tuple(pt))
            sections.append(sec)
        groups = [p.group() for _ in range(4)]

        def uv(s, j, fs, fj):
            a = j - fj
            if fj == 2:
                a = 1 - a                                    # lower surface: leading edge on the rect's left too
            return rect_uv(64, R[blade_rects[fj]], a, 1 - span[s])

        cap = [(0, 0), (1, 0), (1, 1), (0, 1)]
        loft(p, sections, uv, lambda fs, fj: groups[fj], lambda end, i, pt: rect_uv(64, R['blade_cap'], *cap[i]))

    # hub: D 0.2, y -0.06..0.06, chamfered bottom (faces the motor)
    hub_b = [1.0, 0.8, 0.0]
    lathe(p, [(0.075, -0.06), (0.10, -0.035), (0.10, 0.06)], 16,
          lambda s, j, fs: rect_uv(64, R['hub_side'], j / 16, hub_b[s]),
          lambda end, i, pt: rect_uv(64, R['hub_cap'], .5 + pt[0] / (2 * math.hypot(pt[0], pt[2])),
                                     .5 + pt[2] / (2 * math.hypot(pt[0], pt[2]))))
    return p


def guard_y(r):
    """Centre line of the guard bars: dished, 0.304 below the ring top at the rim (bar top 2 mm inside the ring
    wall) to 0.344 at the central opening."""
    return -0.304 - 0.040 * (0.535 - r) / (0.535 - GUARD_R0)


def build_ring():
    """Fan ring for the 1.0 m rotor plus its guard (3 rings, 8 radial bars, ~12 mm sections). Its widest parts, the
    top flange the plenum casing meets and the bellmouth rim at the inlet, are 1.12 rotor diameters across
    (`ProcessUnit.RING_OD`), so fans side by side in a bay keep apart (owner report 2026-10-16: rings overlapped)."""
    p = Part('ac_fan_ring')
    R = RING_RECTS
    n = 24
    profile = [(0.515, -0.30), (0.56, -0.30), (0.56, -0.285), (0.535, -0.25), (0.535, -0.02), (0.56, -0.02), (0.56, 0.0),
               (0.515, 0.0)]
    # band -> (rect, b at its first point, b at its second point)
    bands = [('lip', 0, 1), ('lip', 0, 1), ('outer', 1, .85), ('outer', .85, 0), ('flange', 0, .35), ('flange', .35, .5),
             ('flange', .5, 1), ('inner', 0, 1)]
    smooth = [False, False, False, True, False, True, False, True]

    # Around a band, u runs with arc length (j / n at the band's outer radius, less towards its inner one): a flat or
    # conical band's quad is then an affine image of its texture, so the mod draws its two triangles as one quad.
    def band_uv(s, j, fs):
        rect, b0, b1 = bands[fs]
        r = profile[s % len(profile)][0]
        outer = max(profile[fs][0], profile[(fs + 1) % len(profile)][0])
        return rect_uv(64, R[rect], j / n * r / outer, b0 if s == fs else b1)

    lathe(p, profile, n, band_uv, smooth=smooth, wrap=True)

    g_rect = R['guard']
    # three guard rings, triangular wire section (flat side down), resting on the bars
    tri_sec = [(-0.007, -0.005), (0.007, -0.005), (0.0, 0.007)]   # (radial, vertical) offsets
    for ri, (rr, m) in enumerate(((0.12, 12), (0.31, 16), (0.49, 24))):
        yc = guard_y(rr) + 0.009
        sections = [[(math.cos(2 * math.pi * s / m) * (rr + dr), yc + dy, -math.sin(2 * math.pi * s / m) * (rr + dr))
                     for dr, dy in tri_sec] for s in range(m)]
        groups = [p.group() for _ in range(3)]
        # u with arc length across each face of the wire, as the ring's bands.
        loft(p, sections, lambda s, j, fs, fj, m=m, ri=ri, rr=rr: (
            rect_uv(64, (g_rect[0], g_rect[1] + 4 * ri, g_rect[2], g_rect[1] + 4 * ri + 4),
                    s / m * (rr + tri_sec[j % 3][0]) / (rr + max(tri_sec[fj % 3][0], tri_sec[(fj + 1) % 3][0])), (j - fj))),
             lambda fs, fj, groups=groups: groups[fj], wrap_s=True)
    # eight radial bars, square 12 mm section, sloped with the dish
    for b in range(8):
        phi = b * math.pi / 4 + math.pi / 8
        er = Vector((math.cos(phi), 0, -math.sin(phi)))
        et = Vector((-math.sin(phi), 0, -math.cos(phi)))
        sections = [[tuple(er * r + et * t + Vector((0, guard_y(r) + h, 0)))
                     for t, h in ((-0.006, -0.006), (0.006, -0.006), (0.006, 0.006), (-0.006, 0.006))]
                    for r in (GUARD_R0, GUARD_R1)]
        bar_rect = (g_rect[0], g_rect[1] + 12, g_rect[2], g_rect[3])
        loft(p, sections, lambda s, j, fs, fj: rect_uv(64, bar_rect, s, (j - fj)), lambda fs, fj: 0,
             lambda end, i, pt: rect_uv(64, bar_rect, end, (i % 2)))
    return p


def build_motor():
    """Vertical flange motor (V1). Origin: centre of the flange's top face; body down -Y; terminal box on +X;
    shaft up +Y. Not scaled with the fan."""
    p = Part('ac_motor')
    R = MOTOR_RECTS

    def disc(rect, cx=0.0, cz=0.0):
        """Cap UV: the cap polygon projected from above, inscribed in the rect."""
        def uv(end, i, pt):
            d = math.hypot(pt[0] - cx, pt[2] - cz)
            return rect_uv(64, rect, .5 + (pt[0] - cx) / (2 * d), .5 + (pt[2] - cz) / (2 * d))
        return uv

    # mounting flange: D 0.40, y -0.03..0
    lathe(p, [(0.20, -0.03), (0.20, 0.0)], 16, lambda s, j, fs: rect_uv(64, R['flange_side'], j / 16, 1 - s),
          disc(R['flange_cap']))
    # frame: D 0.32 from under the flange to -0.52, chamfered fan-cowl end down to -0.58
    lathe(p, [(0.16, -0.02), (0.16, -0.52), (0.14, -0.58)], 16,
          lambda s, j, fs: rect_uv(64, R['body'] if fs == 0 else R['cowl'], j / 16, s - fs),
          disc(R['end_cap']))
    # twelve cooling fins, clear of +X where the terminal box sits
    for k in range(12):
        box(p, (0.15, -0.50, -0.006), (0.19, -0.06, 0.006), lambda f, a, b: rect_uv(64, R['fin'], a, b),
            xf=rot_y(math.radians(15 + 30 * k)))
    # terminal box on +X with its lid (nameplate on the outer face) and a cable gland underneath
    box(p, (0.14, -0.40, -0.08), (0.27, -0.24, 0.08), lambda f, a, b: rect_uv(64, R['tbox'], a, b))
    box(p, (0.27, -0.405, -0.085), (0.285, -0.235, 0.085),
        lambda f, a, b: rect_uv(64, R['tlid'] if f == '+x' else R['tbox'], a, b))
    lathe(p, [(0.022, -0.45), (0.022, -0.395)], 8, lambda s, j, fs: rect_uv(64, R['gland'], j / 8, 1 - s),
          disc(R['gland'], 0.215, 0.0), centre=(0.215, 0.0))
    # shaft: D 0.05, 0.12 above the flange face (1 cm more inside the flange keeps the solid seated)
    lathe(p, [(0.025, -0.01), (0.025, 0.12)], 10, lambda s, j, fs: rect_uv(64, R['shaft'], j / 10, 1 - s),
          disc(R['shaft']))
    return p


# ----------------------------------------------------------------------------------------------------------------
# Blender objects, export and checks
# ----------------------------------------------------------------------------------------------------------------

def g2b(p):
    return Vector((p[0], -p[2], p[1]))


def make_object(part, material, collection=None):
    mesh = bpy.data.meshes.new(part.name)
    mesh.from_pydata([tuple(g2b(v)) for v in part.verts], [], part.tris)
    mesh.update()
    layer = mesh.uv_layers.new(name='UVMap')
    layer.data.foreach_set('uv', [c for tri in part.uvs for corner in tri for c in corner])
    mesh.polygons.foreach_set('use_smooth', [g != 0 for g in part.groups])
    faces_of = {}
    for f, (a, b, c) in enumerate(part.tris):
        for e in ((a, b), (b, c), (c, a)):
            faces_of.setdefault((min(e), max(e)), []).append(f)
    sharp = []
    for e in mesh.edges:
        gs = {part.groups[f] for f in faces_of[(min(e.vertices), max(e.vertices))]}
        sharp.append(len(gs) > 1 or 0 in gs)
    mesh.edges.foreach_set('use_edge_sharp', sharp)
    mesh.materials.append(material)
    obj = bpy.data.objects.new(part.name, mesh)
    (collection or bpy.context.scene.collection).objects.link(obj)
    return obj


def material(name, image=None, colour=(0.8, 0.8, 0.8), tint=None, rough=0.6, metal=0.0, cull=True, repeat=False):
    mat = bpy.data.materials.new(name)
    if mat.node_tree is None:          # Blender 5 creates the node tree already; use_nodes goes away in 6.0
        mat.use_nodes = True
    nt = mat.node_tree
    nt.nodes.clear()
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    bsdf = nt.nodes.new('ShaderNodeBsdfPrincipled')
    nt.links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    bsdf.inputs['Roughness'].default_value = rough
    bsdf.inputs['Metallic'].default_value = metal
    if image is not None:
        tex = nt.nodes.new('ShaderNodeTexImage')
        tex.image = image
        tex.interpolation = 'Closest'
        tex.extension = 'REPEAT' if repeat else 'EXTEND'
        col = tex.outputs['Color']
        if tint is not None:
            mul = nt.nodes.new('ShaderNodeVectorMath')
            mul.operation = 'MULTIPLY'
            nt.links.new(col, mul.inputs[0])
            mul.inputs[1].default_value = tint
            col = mul.outputs['Vector']
        nt.links.new(col, bsdf.inputs['Base Color'])
    else:
        bsdf.inputs['Base Color'].default_value = (*colour, 1.0)
    mat.use_backface_culling = cull
    return mat


def export_obj(obj, path):
    for o in bpy.context.scene.objects:
        o.select_set(False)
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.wm.obj_export(filepath=path, export_selected_objects=True, forward_axis='NEGATIVE_Z', up_axis='Y',
                          global_scale=1.0, apply_modifiers=True, export_uv=True, export_normals=True,
                          export_colors=False, export_materials=True, export_pbr_extensions=False, path_mode='STRIP',
                          export_triangulated_mesh=True, export_object_groups=False, export_material_groups=False,
                          export_vertex_groups=False, export_smooth_groups=False, export_animation=False)


def check_obj(path):
    """Parse an exported OBJ back and check what the game loader relies on."""
    V, VT, VN, F, mats = [], [], [], [], []
    with open(path) as fh:
        for line in fh:
            t = line.split()
            if not t:
                continue
            if t[0] == 'v':
                V.append(Vector(tuple(map(float, t[1:4]))))
            elif t[0] == 'vt':
                VT.append(tuple(map(float, t[1:3])))
            elif t[0] == 'vn':
                VN.append(Vector(tuple(map(float, t[1:4]))))
            elif t[0] == 'usemtl':
                mats.append(t[1])
            elif t[0] == 'f':
                F.append([tuple(int(x) - 1 if x else -1 for x in c.split('/')) for c in t[1:]])
    res = {'vertices': len(V), 'triangles': len(F), 'usemtl': mats,
           'non_triangles': sum(1 for f in F if len(f) != 3),
           'corners_without_uv_or_normal': sum(1 for f in F for c in f if len(c) < 3 or c[1] < 0 or c[2] < 0)}
    used_vt = {c[1] for f in F for c in f}
    res['uv_outside_0_1'] = sum(1 for i in used_vt if not (-1e-6 <= VT[i][0] <= 1 + 1e-6 and -1e-6 <= VT[i][1] <= 1 + 1e-6))
    directed, undirected = {}, {}
    for f in F:
        vs = [c[0] for c in f]
        for a, b in zip(vs, vs[1:] + vs[:1]):
            directed[(a, b)] = directed.get((a, b), 0) + 1
            undirected[(min(a, b), max(a, b))] = undirected.get((min(a, b), max(a, b)), 0) + 1
    res['edges'] = len(undirected)
    res['non_manifold_edges'] = sum(1 for c in undirected.values() if c != 2)
    res['inconsistent_winding_edges'] = sum(1 for c in directed.values() if c != 1)
    # shells (connected components) and their signed volumes
    parent = list(range(len(V)))

    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i
    for f in F:
        for c in f[1:]:
            ra, rb = find(f[0][0]), find(c[0])
            if ra != rb:
                parent[ra] = rb
    vol = {}
    flips = degenerate = 0
    for f in F:
        a, b, c = (V[x[0]] for x in f)
        n = (b - a).cross(c - a)
        if n.length < 1e-12:
            degenerate += 1
            continue
        vol[find(f[0][0])] = vol.get(find(f[0][0]), 0.0) + a.dot(b.cross(c)) / 6
        for x in f:
            if VN[x[2]].dot(n) <= 0:
                flips += 1
    res['shells'] = len(vol)
    res['inward_shells'] = sum(1 for v in vol.values() if v <= 0)
    res['degenerate_triangles'] = degenerate
    res['normals_against_winding'] = flips
    res['bounds_min'] = [round(min(v[i] for v in V), 4) for i in range(3)]
    res['bounds_max'] = [round(max(v[i] for v in V), 4) for i in range(3)]
    return res


# ----------------------------------------------------------------------------------------------------------------
# Previews
# ----------------------------------------------------------------------------------------------------------------

TINT = (0.86, 0.88, 0.90)     # preview paint colour for the tinted sprites (the mod supplies the real one)
FAN_SCALE = 1.9               # mock-up fan diameter
FAN_IN_RING = -0.15           # rotor centre below the ring top, in rotor diameters


def box_uv_metres(part, lo, hi, tile, v_centre=None):
    """Mock-up slab with UVs in tiles (metres / tile), box-projected per face; preview only (UVs repeat).
    v_centre puts the header sprite's plug row (v = 0.516) at that height on the side faces."""
    (x0, y0, z0), (x1, y1, z1) = lo, hi

    def face_uv(face, a, b):
        y = y1 - b * (y1 - y0)
        if face in ('-y', '+y'):
            return ((x0 + a * (x1 - x0)) / tile, (z0 + b * (z1 - z0)) / tile)
        u = {'-z': x0 + a * (x1 - x0), '+x': z0 + a * (z1 - z0), '+z': x1 - a * (x1 - x0), '-x': z1 - a * (z1 - z0)}[face]
        v = y / tile if v_centre is None else (y - v_centre) / tile + .516
        return (u / tile, v)
    box(part, lo, hi, face_uv)


def quads_part(name, quads, tile):
    """Open quads (preview only): each quad (p0..p3) with planar UVs in metres along its two edge directions."""
    p = Part(name)
    for q in quads:
        idx = [p.vert(v) for v in q]
        e1 = Vector(q[1]) - Vector(q[0])
        up = Vector((0, 1, 0))
        uvs = []
        for v in q:
            d = Vector(v) - Vector(q[0])
            uvs.append((d.dot(e1.normalized()) / tile, d.dot(up) / tile))
        p.quad(*idx, *uvs, 0)
    return p


def build_mockup(images, parts_objs, coll):
    """Bay 3.0 m x 6.0 m: headers 0.6 m deep, bundle 0.5 m thick at 5 m, tapered plenum to the fan deck 1.2 m
    lower, two 1.9 m fans with rings, guards and motors, four legs."""
    mats = {
        'fins': material('mock_fins', images['ac_fins'], repeat=True, rough=.55),
        'header': material('mock_header', images['ac_header'], tint=TINT, repeat=True),
        'plenum': material('mock_plenum', images['ac_plenum'], tint=TINT, repeat=True, cull=False),
        'steel': material('mock_steel', colour=(0.20, 0.22, 0.25), rough=.7),
    }
    objs = []
    y0, y1 = 5.0, 5.5
    bundle = Part('mock_bundle')
    box_uv_metres(bundle, (-1.45, y0, -2.4), (1.45, y1, 2.4), 1.0)
    objs.append(make_object(bundle, mats['fins'], coll))
    frames = Part('mock_frames')
    for xa, xb in ((-1.55, -1.45), (1.45, 1.55)):
        box_uv_metres(frames, (xa, y0 - .02, -2.4), (xb, y1 + .02, 2.4), 1.0)
    objs.append(make_object(frames, mats['plenum'], coll))
    headers = Part('mock_headers')
    for za, zb in ((-3.0, -2.4), (2.4, 3.0)):
        box_uv_metres(headers, (-1.55, y0 - .05, za), (1.55, y1 + .05, zb), 0.5, v_centre=(y0 + y1) / 2)
    objs.append(make_object(headers, mats['header'], coll))
    # tapered plenum (four sides) from the bundle footprint down to the deck
    yd = 3.82
    top = [(-1.45, y0, -2.4), (1.45, y0, -2.4), (1.45, y0, 2.4), (-1.45, y0, 2.4)]
    bot = [(-1.2, yd, -2.3), (1.2, yd, -2.3), (1.2, yd, 2.3), (-1.2, yd, 2.3)]
    quads = [(bot[i], bot[(i + 1) % 4], top[(i + 1) % 4], top[i]) for i in range(4)]
    objs.append(make_object(quads_part('mock_plenum', quads, 1.0), mats['plenum'], coll))
    # fan deck with two holes (boolean, preview only)
    deck = Part('mock_deck')
    box_uv_metres(deck, (-1.2, 3.80, -2.3), (1.2, yd, 2.3), 1.0)
    deck_obj = make_object(deck, mats['plenum'], coll)
    fan_z = (-1.15, 1.15)
    cutters = []
    for z in fan_z:
        c = Part('mock_cut')
        lathe(c, [(0.515 * FAN_SCALE, 3.7), (0.515 * FAN_SCALE, 3.9)], 48, lambda s, j, fs: (0, 0),
              lambda e, i, pt: (0, 0), centre=(0.0, z))
        co = make_object(c, mats['steel'], coll)
        mod = deck_obj.modifiers.new('hole', 'BOOLEAN')
        mod.operation, mod.object, mod.solver = 'DIFFERENCE', co, 'EXACT'
        cutters.append(co)
    dg = bpy.context.evaluated_depsgraph_get()
    cut_mesh = bpy.data.meshes.new_from_object(deck_obj.evaluated_get(dg))
    deck_obj.modifiers.clear()
    deck_obj.data = cut_mesh
    for co in cutters:
        bpy.data.objects.remove(co)
    objs.append(deck_obj)
    # fans, rings (with guards) and motors from the exported OBJ files
    fan_y = 3.80 + FAN_IN_RING * FAN_SCALE
    motor_y = fan_y - 0.06 * FAN_SCALE - 0.06            # shaft 6 cm into the hub
    for k, z in enumerate(fan_z):
        for name, y, scale, spin in (('ac_fan_ring', 3.80, FAN_SCALE, 0.0), ('ac_fan', fan_y, FAN_SCALE, 0.3 + 0.5 * k),
                                     ('ac_motor', motor_y, 1.0, math.pi * k)):
            o = parts_objs[name].copy()
            o.location = g2b((0.0, y, z))
            o.scale = (scale, scale, scale)
            o.rotation_euler = (math.pi / 2, 0, spin)   # the OBJ importer keeps Y-up data under a 90 deg X rotation
            coll.objects.link(o)
            objs.append(o)
    # steel: four legs under the headers; under each motor a cross beam hung from the deck edge
    steel = Part('mock_steel')
    for x in (-1.45, 1.45):
        for z in (-2.7, 2.7):
            box_uv_metres(steel, (x - .1, 0.0, z - .1), (x + .1, y0 - .05, z + .1), 1.0)
    beam_lo, beam_hi = motor_y - 0.72, motor_y - 0.58
    for z in fan_z:
        box_uv_metres(steel, (-1.18, beam_lo, z - .07), (1.18, beam_hi, z + .07), 1.0)
        for x in (-1.12, 1.12):
            box_uv_metres(steel, (x - .04, beam_hi, z - .04), (x + .04, 3.80, z + .04), 1.0)
    objs.append(make_object(steel, mats['steel'], coll))
    return objs


def setup_scene(sc):
    sc.render.engine = 'BLENDER_EEVEE'
    sc.render.resolution_x, sc.render.resolution_y, sc.render.resolution_percentage = 1024, 768, 100
    sc.render.image_settings.file_format = 'PNG'
    sc.render.image_settings.color_mode = 'RGB'
    sc.render.image_settings.color_depth = '8'
    sc.render.film_transparent = False
    for attr in dir(sc.render):
        if attr.startswith('use_stamp'):
            try:
                setattr(sc.render, attr, False)
            except (AttributeError, TypeError):
                pass
    sc.view_settings.view_transform = 'Standard'
    sc.view_settings.look = 'None'
    sc.eevee.taa_render_samples = 32
    world = bpy.data.worlds.new('sky')
    if world.node_tree is None:
        world.use_nodes = True
    bg = next(n for n in world.node_tree.nodes if n.type == 'BACKGROUND')
    bg.inputs['Color'].default_value = (0.52, 0.58, 0.66, 1.0)
    bg.inputs['Strength'].default_value = 0.8
    sc.world = world
    for name, direction, energy, shadow in (('sun', (-0.45, -0.75, -0.5), 2.6, True), ('fill', (0.3, 0.9, 0.3), 0.6, False)):
        light = bpy.data.lights.new(name, 'SUN')
        light.energy = energy
        light.angle = math.radians(3)
        light.use_shadow = shadow
        o = bpy.data.objects.new(name, light)
        o.rotation_euler = g2b(direction).to_track_quat('-Z', 'Y').to_euler()
        sc.collection.objects.link(o)
    cam = bpy.data.objects.new('camera', bpy.data.cameras.new('camera'))
    sc.collection.objects.link(cam)
    sc.camera = cam
    ground = Part('ground')
    box(ground, (-400, -0.2, -400), (400, 0.0, 400), lambda f, a, b: (0, 0))
    gobj = make_object(ground, material('ground', colour=(0.30, 0.31, 0.32), rough=.95))
    return cam, gobj


def aim(cam, target, el_deg, az_deg, dist, lens):
    """Camera at elevation el and azimuth az (0 = from +Z, 90 = from +X) around a game-space target."""
    el, az = math.radians(el_deg), math.radians(az_deg)
    pos = (target[0] + dist * math.cos(el) * math.sin(az), target[1] + dist * math.sin(el),
           target[2] + dist * math.cos(el) * math.cos(az))
    cam.location = g2b(pos)
    cam.rotation_euler = (g2b(target) - g2b(pos)).to_track_quat('-Z', 'Y').to_euler()
    cam.data.lens = lens
    cam.data.clip_start, cam.data.clip_end = 0.01, 2000


def render(sc, path):
    sc.render.filepath = path
    bpy.ops.render.render(write_still=True)


def load_rgb(path):
    img = bpy.data.images.load(path)
    w, h = img.size
    buf = np.empty(w * h * 4, dtype=np.float32)
    img.pixels.foreach_get(buf)
    bpy.data.images.remove(img)
    return (np.flipud(buf.reshape(h, w, 4))[:, :, :3] * 255 + .5).astype(np.uint8)


def contact_sheet(paths, textures, path):
    tiles = []
    for p in paths:
        a = load_rgb(p).astype(np.float32)
        tiles.append(a.reshape(384, 2, 512, 2, 3).mean(axis=(1, 3)))
    sheet = np.full((768 + 16 + 256 + 8, 2048, 3), 96, dtype=np.float32)
    for i, t in enumerate(tiles):
        r, c = divmod(i, 4)
        sheet[r * 384:(r + 1) * 384, c * 512:(c + 1) * 512] = t
    for i, (name, a) in enumerate(textures):
        k = 256 // a.shape[0]
        big = np.kron(a, np.ones((k, k, 1)))
        x = 16 + i * (256 + 80)
        sheet[784:784 + 256, x:x + 256] = big
    write_png(path, (sheet + .5).astype(np.uint8))


# ----------------------------------------------------------------------------------------------------------------

def main():
    out, do_render = parse_args()
    prev = os.path.join(out, 'previews')
    os.makedirs(prev, exist_ok=True)
    for coll in (bpy.data.objects, bpy.data.meshes, bpy.data.materials, bpy.data.lights, bpy.data.cameras):
        for block in list(coll):
            coll.remove(block)
    sc = bpy.context.scene

    # 1. textures
    textures = {'ac_fan': tex_fan(), 'ac_ring': tex_ring(), 'ac_motor': tex_motor(),
                'ac_fins': tex_fins(), 'ac_header': tex_header(), 'ac_plenum': tex_plenum()}
    images = {}
    for name, a in textures.items():
        path = os.path.join(out, name + '.png')
        write_png(path, a)
        images[name] = bpy.data.images.load(path)

    # 2. parts -> OBJ
    # The ring's budget rose from 700 to 800 with the bellmouth rim (2026-10-16, owner report: rings overlapped).
    specs = [('ac_fan', build_fan, 'ac_fan', 300), ('ac_fan_ring', build_ring, 'ac_ring', 800),
             ('ac_motor', build_motor, 'ac_motor', 450)]
    report = {'blender': BLENDER_TAG, 'parts': {}}
    for obj_name, builder, mat_name, budget in specs:
        part = builder()
        obj = make_object(part, material(mat_name, images[mat_name]))
        path = os.path.join(out, obj_name + '.obj')
        export_obj(obj, path)
        res = check_obj(path)
        res.update({'material': mat_name, 'texture': mat_name + '.png', 'budget': budget,
                    'built_triangles': len(part.tris), 'built_shells': part.shells})
        res['ok'] = (res['triangles'] <= budget and res['non_triangles'] == 0 and res['non_manifold_edges'] == 0
                     and res['inconsistent_winding_edges'] == 0 and res['inward_shells'] == 0
                     and res['uv_outside_0_1'] == 0 and res['degenerate_triangles'] == 0
                     and res['normals_against_winding'] == 0 and res['corners_without_uv_or_normal'] == 0
                     and res['usemtl'] == [mat_name] and res['shells'] == part.shells)
        report['parts'][obj_name] = res
        print('CHECK', obj_name, json.dumps(res))
    with open(os.path.join(out, 'report.json'), 'w') as fh:
        json.dump(report, fh, indent=1, sort_keys=True)
    if not all(r['ok'] for r in report['parts'].values()):
        raise SystemExit('part check failed, see report.json')
    if not do_render:
        return

    # 3. previews: re-import the exported OBJ files (what the mod will load), then render
    for o in list(bpy.data.objects):
        bpy.data.objects.remove(o)
    cam, ground = setup_scene(sc)
    parts_coll = bpy.data.collections.new('parts')
    sc.collection.children.link(parts_coll)
    parts_objs = {}
    for obj_name, _, mat_name, _ in specs:
        before = set(bpy.data.objects)
        bpy.ops.wm.obj_import(filepath=os.path.join(out, obj_name + '.obj'), forward_axis='NEGATIVE_Z', up_axis='Y')
        o = next(x for x in bpy.data.objects if x not in before)
        for c in list(o.users_collection):
            c.objects.unlink(o)
        parts_coll.objects.link(o)
        o.data.materials.clear()
        o.data.materials.append(material('prev_' + mat_name, images[mat_name], tint=TINT if mat_name == 'ac_ring' else None,
                                         rough=.45 if mat_name == 'ac_fan' else .6,
                                         metal=.3 if mat_name == 'ac_fan' else 0.0))
        parts_objs[obj_name] = o
    mock_coll = bpy.data.collections.new('mockup')
    sc.collection.children.link(mock_coll)
    mock = build_mockup(images, parts_objs, mock_coll)

    def show(names, mockup, ground_on):
        for name, o in parts_objs.items():
            o.hide_render = name not in names
        for o in mock:
            o.hide_render = not mockup
        ground.hide_render = not ground_on
        ground.location = g2b((0, 0 if mockup else -0.65, 0))

    views = [  # file, visible part, mock-up, ground, target, elevation, azimuth, distance, lens
        ('fan_below', 'ac_fan', False, False, (0, 0, 0), -18, 30, 1.85, 50),
        ('fan_above', 'ac_fan', False, True, (0, 0, 0), 38, 30, 1.95, 50),
        ('ring_below', 'ac_fan_ring', False, False, (0, -0.17, 0), -32, 30, 2.4, 50),
        ('ring_above', 'ac_fan_ring', False, True, (0, -0.17, 0), 28, 30, 2.4, 50),
        ('motor_side', 'ac_motor', False, True, (0.04, -0.24, 0), 12, 65, 1.75, 50),
        ('motor_below', 'ac_motor', False, False, (0.04, -0.24, 0), -38, 40, 1.75, 50),
        ('bay_below', None, True, True, (0, 4.0, 0), -24, 74, 7.6, 22),
        ('bay_above_front', None, True, True, (0, 3.0, 0), 27, 33, 11.0, 28),
    ]
    paths = []
    for name, part_name, mockup, ground_on, target, el, az, dist, lens in views:
        show({part_name} if part_name else set(), mockup, ground_on)
        aim(cam, target, el, az, dist, lens)
        path = os.path.join(prev, name + '.png')
        render(sc, path)
        paths.append(path)
    contact_sheet(paths, [(n, textures[n]) for n in ('ac_fan', 'ac_ring', 'ac_motor', 'ac_fins', 'ac_header', 'ac_plenum')],
                  os.path.join(prev, 'contact_sheet.png'))
    print('PREVIEWS', json.dumps(paths + [os.path.join(prev, 'contact_sheet.png')]))


main()
