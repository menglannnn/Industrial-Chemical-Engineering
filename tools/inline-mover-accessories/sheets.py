from pathlib import Path
from PIL import Image,ImageDraw
root=Path(__file__).parents[2]/'documentation/2026-10-25-inline-pump-accessory/renders'
for kind in ('pump','compressor'):
    out=Image.new('RGB',(960,1020),'white');d=ImageDraw.Draw(out)
    for row,bore in enumerate((0.001,0.05,1.0)):
        for col,view in enumerate((0,1,2)):
            im=Image.open(root/f'{kind}-2-{bore}-{view}.png');im.thumbnail((312,310));x=320*col;y=340*row;out.paste(im,(x+(320-im.width)//2,y+25));d.text((x+8,y+7),f'{kind}: {bore} m / view {view}',fill='#20313b')
    out.save(root/f'{kind}-sheet.png')

# Lower-casing closeups, the focus of the owner's refinement feedback.
out=Image.new('RGB',(1080,800),'white');d=ImageDraw.Draw(out)
for row,kind in enumerate(('pump','compressor')):
    for col,bore in enumerate((0.001,0.05,1.0)):
        im=Image.open(root/f'{kind}-2-{bore}-lower.png');im.thumbnail((352,365));x=360*col;y=400*row;out.paste(im,(x+(360-im.width)//2,y+28));d.text((x+8,y+8),f'{kind}: {bore} m / lower casing',fill='#20313b')
out.save(root/'lower-casing-details.png')

# Equipment-only comparison with both lower assemblies visible.
out=Image.new('RGB',(680,760),'white');d=ImageDraw.Draw(out)
for col,kind in enumerate(('pump','compressor')):
    im=Image.open(root/f'{kind}-item.png');im.thumbnail((330,710));x=340*col
    out.paste(im,(x+(340-im.width)//2,36+(710-im.height)//2));d.text((x+12,12),kind.capitalize(),fill='#20313b')
out.save(root/'revised-models.png')

# Short neighboring pipe stubs show the mating flanges and the foot resting on the support plane.
out=Image.new('RGB',(1000,820),'white');d=ImageDraw.Draw(out)
for row,kind in enumerate(('pump','compressor')):
    for col,bore in enumerate((0.05,0.4)):
        im=Image.open(root/f'{kind}-2-{bore}-connected.png');im.thumbnail((490,372));x=500*col;y=410*row
        out.paste(im,(x+(500-im.width)//2,y+32+(372-im.height)//2));d.text((x+12,y+10),f'{kind.capitalize()} / {bore} m bore',fill='#20313b')
out.save(root/'support-layout.png')
