from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parent
frames=[Image.open(p).convert("RGB") for p in sorted((root/"preview"/"frames").glob("*.png"))]
assert len(frames)==56
palette=frames[0].quantize(colors=192)
images=[frame.quantize(palette=palette) for frame in frames]
images[0].save(root/"preview"/"flow-direction.gif",save_all=True,append_images=images[1:],duration=100,loop=0,optimize=False)
frames[15].save(root/"preview"/"flow-directions.png")
print("Rendered",len(frames),"frames; 12 orientations/directions per frame")
