from pathlib import Path
from PIL import Image,ImageDraw
root=Path(__file__).parents[2]/'documentation/2026-10-25-sight-glass-ratio-seams-sound'
images=[Image.open(path).convert('RGB') for path in sorted((root/'frames').glob('frame-*.png'))]
palette=images[0].quantize(colors=128);frames=[image.quantize(palette=palette,dither=Image.Dither.NONE) for image in images]
frames[0].save(root/'gas-ratio.gif',save_all=True,append_images=frames[1:],duration=100,loop=0,optimize=True,disposal=2)
images[30].save(root/'gas-ratio.png')
out=Image.new('RGB',(1120,460),'white');d=ImageDraw.Draw(out)
for col,path in enumerate((root/'before/seams.png',root/'seams.png')):
    image=Image.open(path);image.thumbnail((550,410));out.paste(image,(col*560,38));d.text((col*560+14,12),'Before' if col==0 else 'Repaired',fill='#20313b')
out.save(root/'seam-comparison.png')
print(f'{len(frames)} ratio animation frames; before/after seam comparison')
