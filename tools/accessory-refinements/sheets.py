from pathlib import Path
from PIL import Image, ImageOps, ImageDraw
root=Path(__file__).parents[2]/'documentation/2026-10-25-accessory-refinements/renders'
groups={'gauge-angles':[f'angle-{a}.png' for a in (0,35,75,105,145,180,215,255,285,325)],'pipe-mounts':[f'pipe-{a}-{b}-{t}.png' for a in range(3) for b in (0.001,0.05,1.0) for t in ('false','true')],'filters':[f'filter-{a}-{b}.png' for a in range(3) for b in (0.001,0.05,1.0)]}
for name,files in groups.items():
    cols=3 if name=='filters' else 4;cell=(280,280);sheet=Image.new('RGB',(cols*cell[0],((len(files)+cols-1)//cols)*cell[1]),'white');d=ImageDraw.Draw(sheet)
    for i,file in enumerate(files):
        im=Image.open(root/file);im.thumbnail((272,250));x=(i%cols)*cell[0];y=(i//cols)*cell[1];sheet.paste(im,(x+(cell[0]-im.width)//2,y+24));d.text((x+6,y+6),file,fill='#20313b')
    sheet.save(root/(name+'-sheet.png'))
