package com.wormzjl.icheme.science.equipment;

import java.nio.file.*;
import java.util.*;
import static com.wormzjl.icheme.science.equipment.EquipmentShape.*;

/** Offline geometry/placement check; no Minecraft client or process simulation. */
public final class TankIndicatorAudit {
    private static void require(boolean yes,String reason){if(!yes)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception {
        boolean baseline=args[0].equals("baseline");var lines=new ArrayList<String>();int count=0,ports=0;
        for(int diameter=4;diameter<=40;diameter++)for(int height=4;height<=24;height++)for(var heading:Heading.values()){
            var s=EquipmentShape.storage(Kind.TANK,diameter,height,heading,0);var u=ProcessUnit.of(s);var ns=s.nozzles();
            require(ns.size()==3&&ns.get(0).role()==Role.BOTTOM&&ns.get(1).role()==Role.BOTTOM&&ns.get(2).role()==Role.OVERFLOW,s.toString());
            var node=s.transform().point(u.fluidPoint);
            lines.add(diameter+","+height+","+heading+","+(int)Math.floor(node[0])+","+(int)Math.floor(node[1])+","+(int)Math.floor(node[2]));
            if(!baseline){
                var a=ns.get(0);var b=ns.get(1);var overflow=ns.get(2);
                require(a.sign()==-1&&b.sign()==1&&overflow.sign()==-1,"port direction "+s);
                require(a.axis()==2&&b.axis()==2&&a.base()[0]==b.base()[0]&&a.base()[0]==overflow.base()[0],"centerline "+s);
                require(a.base()[1]==.5&&b.base()[1]==.5,"bottom row "+s);
                require(a.face()[2]<b.face()[2]&&Math.rint(a.face()[2])==a.face()[2]&&Math.rint(b.face()[2])==b.face()[2],"block-face alignment "+s);
                require(!a.crowds(b)&&!a.crowds(overflow)&&!b.crowds(overflow),"crowded ports "+s);
                var dir=s.transform().direction(a.direction());require(dir[0]==heading.dx&&dir[2]==heading.dz,"heading "+s);
                var back=s.transform().direction(b.direction());require(back[0]==-heading.dx&&back[2]==-heading.dz,"back heading "+s);
            }
            count++;
        }
        Files.write(Path.of(args[1]),lines);
        if(!baseline){
            var previous=Files.readAllLines(Path.of(args[2]));int reframed=0;
            for(int i=0;i<lines.size();i++)if(!lines.get(i).equals(previous.get(i))){
                var old=previous.get(i).split(",");var now=lines.get(i).split(",");
                var s=EquipmentShape.storage(Kind.TANK,Integer.parseInt(old[0]),Integer.parseInt(old[1]),Heading.valueOf(old[2]),0);
                var world=new HashMap<ShapeRasterizer.Cell,EquipmentRefresh.Holding>();
                for(var row:Files.readAllLines(Path.of(args[2]+"-"+old[0]+"-"+old[1]+"-"+old[2]+".layout"))){var q=row.split(",");world.put(new ShapeRasterizer.Cell(Integer.parseInt(q[1]),Integer.parseInt(q[2]),Integer.parseInt(q[3])),q[0].equals("S")?EquipmentRefresh.Holding.SHELL:EquipmentRefresh.Holding.INTERIOR);}
                var plan=EquipmentRefresh.plan(ShapeRasterizer.layout(s),world);
                require(Integer.parseInt(now[3])+plan.shift().x()==Integer.parseInt(old[3])&&Integer.parseInt(now[4])==Integer.parseInt(old[4])&&Integer.parseInt(now[5])+plan.shift().z()==Integer.parseInt(old[5]),"refresh moves world binding "+s+" "+plan.shift());
                require(plan.blocked().isEmpty(),"unexpected blocked refresh");reframed++;
            }
            System.out.println("REFRESH: "+reframed+" changed local boxes preserve world binding through EquipmentRefresh.plan");
            for(int d:new int[]{4,5,6,20,40})for(int h:new int[]{4,12,24})for(var heading:Heading.values()){
                var s=EquipmentShape.storage(Kind.TANK,d,h,heading,0);var ps=ShapeRasterizer.ports(s);var node=ShapeRasterizer.fluidCell(s);
                require(node!=null&&ShapeRasterizer.layout(s).shell().contains(node),"node not shell "+s);
                var gauge=ShapeRasterizer.floating(s);require(Arrays.equals(gauge.facing(),new double[]{heading.dx,0,heading.dz}),"gauge is not front-facing "+s);
                require(ps.size()==3&&ps.get(0).cell().y()==0&&ps.get(1).cell().y()==0,"compiled port row "+s);
                var occupied=new HashSet<ShapeRasterizer.Cell>(ShapeRasterizer.layout(s).shell());occupied.addAll(ShapeRasterizer.layout(s).interior());
                for(var port:ps){var c=port.cell();require(!occupied.contains(new ShapeRasterizer.Cell(c.x()+port.dx(),c.y()+port.dy(),c.z()+port.dz())),"no room for pipe "+s+" "+port);}

                require(ps.get(0).dx()==heading.dx&&ps.get(0).dz()==heading.dz&&ps.get(1).dx()==-heading.dx&&ps.get(1).dz()==-heading.dz,"compiled direction "+s);
                ports++;
            }
            var faces=TankGaugeGeometry.bandFaces();require(faces.size()==6,"gauge is not closed");var normals=new HashSet<String>();
            for(var f:faces){var p=f.positions();var n=f.normals();double ax=p[3]-p[0],ay=p[4]-p[1],az=p[5]-p[2],bx=p[6]-p[0],by=p[7]-p[1],bz=p[8]-p[2];
                double x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx;
                require(x*(p[0]-.5)+y*(p[1]-.5)+z*(p[2]-.5)>0,"inward gauge face");
                require(x*n[0]+y*n[1]+z*n[2]>0,"normal mismatch");normals.add(n[0]+","+n[1]+","+n[2]);}
            require(normals.size()==6,"missing direction");
            System.out.println("PASS: "+count+" tank size/height/heading plans, "+ports+" actual compiled layouts, all binding cells unchanged; six outward gauge faces including bottom");
        }else {
            for(int d:new int[]{4,5,6})for(int h=4;h<=24;h++)for(var heading:Heading.values()){
                if(d==6&&!(h>=19&&h<=21&&(heading==Heading.EAST||heading==Heading.SOUTH)))continue;
                var s=EquipmentShape.storage(Kind.TANK,d,h,heading,0);var layout=ShapeRasterizer.layout(s);var held=new ArrayList<String>();
                for(var c:layout.shell())held.add("S,"+c.x()+","+c.y()+","+c.z());for(var c:layout.interior())held.add("I,"+c.x()+","+c.y()+","+c.z());
                Files.write(Path.of(args[1]+"-"+d+"-"+h+"-"+heading+".layout"),held);
            }
            System.out.println("BASELINE: "+count+" tank binding cells recorded");
        }
    }
}
