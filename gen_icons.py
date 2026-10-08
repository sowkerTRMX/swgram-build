from PIL import Image, ImageChops, ImageDraw
import numpy as np, os
im=Image.open('logo.png').convert('RGB')
ys,xs=np.where(np.asarray(im).max(axis=2)>90)
x0,x1,y0,y1=xs.min(),xs.max(),ys.min(),ys.max()
pad=int(0.06*max(x1-x0,y1-y0))
crop=im.crop((x0-pad,y0-pad,x1+pad,y1+pad))
side=max(crop.size)
def fit(size,fill):
    k=size*fill/side
    return crop.resize((max(1,int(crop.width*k)),max(1,int(crop.height*k))),Image.LANCZOS)
def icon(size,fill):
    bg=Image.new('RGB',(size,size),(0,0,0));c=fit(size,fill)
    bg.paste(c,((size-c.width)//2,(size-c.height)//2));return bg
def fg(size,fill):
    c=fit(size,fill);r,g,b=c.split()
    a=ImageChops.lighter(ImageChops.lighter(r,g),b).point(lambda v:min(255,int(v*1.6)))
    c=c.convert('RGBA');c.putalpha(a)
    o=Image.new('RGBA',(size,size),(0,0,0,0));o.paste(c,((size-c.width)//2,(size-c.height)//2),c);return o
for d,m in {'mdpi':1,'hdpi':1.5,'xhdpi':2,'xxhdpi':3,'xxxhdpi':4}.items():
    p='icons/mipmap-'+d;os.makedirs(p,exist_ok=True)
    icon(int(48*m),0.86).save(p+'/ic_launcher.png')
    r=icon(int(48*m),0.80);mk=Image.new('L',r.size,0);ImageDraw.Draw(mk).ellipse((0,0,r.size[0]-1,r.size[1]-1),fill=255)
    r=r.convert('RGBA');r.putalpha(mk);r.save(p+'/ic_launcher_round.png')
    fg(int(108*m),0.62).save(p+'/ic_launcher_foreground.png')

from PIL import ImageOps
os.makedirs('icons_sw',exist_ok=True)
base=ImageOps.grayscale(icon(192,0.86)).point(lambda v:min(255,int(v*1.5)))
for n,c in {'blue':(60,170,255),'red':(255,60,70),'gold':(255,196,40),'purple':(170,90,255),'green':(60,225,130),'pink':(255,90,190)}.items():
    ImageOps.colorize(base,(0,0,0),c).save('icons_sw/sw_%s.png'%n)
