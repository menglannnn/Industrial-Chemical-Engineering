from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path('D:/Minecraft/Modding/1.21/CreateChemE/documentation/2026-10-24-floating-roof-indicator-ports/renders')
font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 20)

def sheet(name, rows):
    canvas = Image.new('RGB', (1320, 420 * len(rows)), '#eef2f4')
    draw = ImageDraw.Draw(canvas)
    for row, entries in enumerate(rows):
        for col, (file, label) in enumerate(entries):
            im = Image.open(root / file).convert('RGB')
            im.thumbnail((312, 365))
            x, y = 12 + col * 330, 44 + row * 420
            draw.text((x, y - 30), label, font=font, fill='#273b48')
            canvas.paste(im, (x + (312 - im.width) // 2, y))
    canvas.save(root / name)

views = ['front', 'low', 'side', 'under']
sheet('gauge-comparison.png', [[(f'gauge-{variant}-{v}.png', f'{variant.capitalize()} / {v}') for v in views] for variant in ['baseline', 'current']])
sheet('gauge-preview.png', [[(f'gauge-current-{v}.png', f'Revised / {v}') for v in views]])
sheet('ports-preview.png', [[(f'tank-{size}-{v}.png', f'D {size} m / {v}') for v in ['front', 'back', 'top', 'low']] for size in [4, 20]])
