"""Generate estimated bundled crude colours from property NBP; explicit datapack overrides remain authoritative."""
from pathlib import Path
import argparse,json
from PIL import Image,ImageDraw,ImageFont

parser=argparse.ArgumentParser();parser.add_argument('--source-root',type=Path,required=True);parser.add_argument('--apply',action='store_true');args=parser.parse_args()
materials=args.source_root/'src/main/resources/data/icheme/materials'
output=Path(__file__).parents[2]/'documentation/2026-10-25-sight-glass-bubbles';output.mkdir(exist_ok=True)
anchors=[(60,'F8E8B0'),(180,'C79A47'),(320,'75451E'),(500,'2A1C13'),(825,'0C0A09')]
def colour(celsius):
    if celsius<=anchors[0][0]:return '#'+anchors[0][1]
    if celsius>=anchors[-1][0]:return '#'+anchors[-1][1]
    for (a,first),(b,last) in zip(anchors,anchors[1:]):
        if a<=celsius<=b:
            t=(celsius-a)/(b-a);x=bytes.fromhex(first);y=bytes.fromhex(last)
            return '#'+''.join(f'{round(u+(v-u)*t):02X}' for u,v in zip(x,y))
properties={}
for path in (materials/'properties').glob('*.json'):
    p=json.loads(path.read_text(encoding='utf-8'));properties.setdefault(p['component'],set()).add(p['normal_boiling_point_kelvin'])
rows=[]
for path in sorted((materials/'components').glob('*.json')):
    component=json.loads(path.read_text(encoding='utf-8'))
    if component['kind']!='petroleum_fraction':continue
    values=properties[component['id']];assert len(values)==1,(component['id'],values)
    kelvin=next(iter(values));rgb=colour(kelvin-273.15)
    rows.append(dict(component=component['id'],nbp_kelvin=kelvin,colour=rgb,transparency=component['appearance']['transparency']))
    if args.apply:
        component['appearance']['color']=rgb;path.write_text(json.dumps(component,indent=2)+'\n',encoding='utf-8')
rows.sort(key=lambda r:r['nbp_kelvin']);(output/'nbp-palette.json').write_text(json.dumps(dict(anchors_celsius=anchors,fractions=rows),indent=2)+'\n',encoding='utf-8')
image=Image.new('RGB',(1080,440),'#f2f4f5');draw=ImageDraw.Draw(image)
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',17);title=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',23)
draw.text((20,12),'Crude pseudo-components: colour from normal boiling point',font=title,fill='#20313b')
for i,row in enumerate(rows):
    x=20+(i%6)*175;y=58+(i//6)*180
    draw.rectangle((x,y,x+158,y+92),fill=row['colour'])
    draw.text((x,y+98),row['component'],font=font,fill='#20313b')
    draw.text((x,y+122),f"NBP {row['nbp_kelvin']-273.15:.1f} C",font=font,fill='#20313b')
    draw.text((x,y+145),row['colour'],font=font,fill='#20313b')
draw.text((20,420),'Estimated visual palette; scientific properties and opacity are unchanged.',font=font,fill='#4b5c66')
image.save(output/'nbp-palette.png')
print(json.dumps(rows))
