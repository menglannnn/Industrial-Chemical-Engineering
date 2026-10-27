from pathlib import Path
from PIL import Image,ImageDraw
p=Path("D:/Minecraft/Modding/1.21/CreateChemE/documentation/2026-10-24-gauge-accessories/renders")
def sheet(files,name,cols=3,thumb=(400,300)):
    out=Image.new("RGB",(cols*thumb[0],((len(files)+cols-1)//cols)*(thumb[1]+28)),"#edf2f4");d=ImageDraw.Draw(out)
    for i,f in enumerate(files):
        im=Image.open(p/f);im.thumbnail(thumb);x=(i%cols)*thumb[0]+(thumb[0]-im.width)//2;y=(i//cols)*(thumb[1]+28)+28;out.paste(im,(x,y));d.text(((i%cols)*thumb[0]+8,y-22),f,fill="#263b49")
    out.save(p/name)
sheet(["dial--0.4.png","dial-0.0.png","dial-0.5.png","dial-1.0.png","dial-1.4.png"],"dial-sheet.png")
sheet([f"pipe-{a}-{b}-{s}.png" for a in (0,1,2) for b,s in (("0.001",0),("0.05",2),("1.0",4))],"pipe-sheet.png")
sheet([f"{k}-{d}-NORTH.png" for k,ds in (("DRUM",("0.5","2.0","10.0")),("TANK",("4.0","16.0","40.0")),("SPHERE",("4.0","12.0","24.0"))) for d in ds],"reservoir-sheet.png")
sheet([f"{k}-{d}-{h}-tap.png" for k,d in (("DRUM","0.5"),("TANK","4.0"),("SPHERE","4.0")) for h in ("NORTH","EAST","WEST")],"mount-sheet.png")

sheet(["structure-ghost-38779ba9.png","structure-ghost-5873a38e.png","structure-ghost-38ad7770.png"],"structure-preview-sheet.png")
