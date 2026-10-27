from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root=Path(__file__).resolve().parent
paths=sorted((root/'renders').glob('*.png'))
assert len(paths)==12,len(paths)
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18)
sheet=Image.new('RGB',(1600,1800),'white')
draw=ImageDraw.Draw(sheet)
for i,path in enumerate(paths):
    image=Image.open(path).convert('RGB')
    image.thumbnail((390,540))
    x=(i%4)*400+(400-image.width)//2
    y=(i//4)*600+38
    sheet.paste(image,(x,y))
    draw.text(((i%4)*400+10,(i//4)*600+8),path.stem,font=font,fill='#28323a')
sheet.save(root/'gauge-enclosure-sheet.png')
overview=Image.new('RGB',(1280,1000),'white')
draw=ImageDraw.Draw(overview)
for name,label,box in [
    ('small-filled-low','Filled gauge',(0,0,300,1000)),
    ('small-empty-front','Empty gauge',(300,0,300,1000)),
    ('medium-top-low','Top connection',(600,0,680,500)),
    ('medium-bottom-low','Bottom connection',(600,500,680,500)),
]:
    x,y,w,h=box
    image=Image.open(root/'renders'/f'{name}.png').convert('RGB')
    image.thumbnail((w-20,h-46))
    overview.paste(image,(x+(w-image.width)//2,y+38))
    draw.text((x+12,y+8),label,font=font,fill='#28323a')
overview.save(root/'gauge-enclosure-overview.png')
print('Assembled 12 native views; glass uses exact custom sprite alpha')
