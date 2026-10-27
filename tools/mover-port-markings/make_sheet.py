from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
out=Path(__file__).resolve().parents[2]/'documentation/2026-10-23-mover-port-markings/renders'
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',24)
rows=[('pump','Centrifugal pump'),('split-pump','Split-case pump'),('reciprocating','Reciprocating compressor'),('centrifugal','Centrifugal compressor')]
canvas=Image.new('RGB',(1120,4*385+65),'#edf1f3');d=ImageDraw.Draw(canvas)
d.text((25,12),'Suction: toward machine                 Discharge: away from machine',font=font,fill='#243642')
for y,(stem,title) in enumerate(rows):
 d.text((20,70+y*385),title,font=font,fill='#243642')
 for x,role in enumerate(['suction','discharge']):
  im=Image.open(out/(stem+'-north-'+role+'.png')).convert('RGB');im.thumbnail((540,335))
  canvas.paste(im,(x*560+(560-im.width)//2,110+y*385))
canvas.save(out/'arrows-preview.png')
print(out/'arrows-preview.png')
