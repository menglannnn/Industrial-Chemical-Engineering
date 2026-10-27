from pathlib import Path
import sys
from PIL import Image,ImageDraw
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parent
cases=[("bare-low.png","Uninstalled"),("ghost-low.png","Placement preview"),("filled-low.png","Installed"),("empty-low.png","Empty")]
sheet=Image.new("RGB",(1120,820),"white");d=ImageDraw.Draw(sheet)
for i,(name,label) in enumerate(cases):
 im=Image.open(root/"render"/name).convert("RGB");im.thumbnail((274,780))
 sheet.paste(im,(i*280+(280-im.width)//2,35));d.text((i*280+8,10),label,fill="#24333b")
sheet.save(root/"level-gauge-accessory.png")
