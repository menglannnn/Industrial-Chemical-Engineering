from pathlib import Path
from PIL import Image
root=Path(__file__).parents[2]/'documentation/2026-10-25-sight-glass-bubbles'
images=[Image.open(path).convert('RGB') for path in sorted((root/'frames').glob('frame-*.png'))]
palette=images[0].quantize(colors=128)
frames=[image.quantize(palette=palette,dither=Image.Dither.NONE) for image in images]
frames[0].save(root/'bubble-flow.gif',save_all=True,append_images=frames[1:],duration=100,loop=0,optimize=True,disposal=2)
images[30].save(root/'bubble-preview.png')
print(f'{len(frames)} frames; bubble-flow.gif and bubble-preview.png')
