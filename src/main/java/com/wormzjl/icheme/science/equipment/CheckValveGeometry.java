package com.wormzjl.icheme.science.equipment;

import java.util.List;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/** Compact flanged check-valve casting with one word-free axial band of flow arrows. */
public final class CheckValveGeometry {
    private static final double[] CENTRE={.5,.5,.5};
    private CheckValveGeometry(){}

    public static List<Polygon> polygons(LineGeometry.Key key,boolean accessory) {
        var mesh=new Mesh();double od=PipeSizes.outsideOf(key.outside());
        if(accessory)od=Math.max(.40,od);
        int axis=key.facing()&~1,sides=Math.max(16,PipeSizes.sides(od));
        double radius=Math.min(.485,Math.max(.065,.64*od)),half=Math.min(.27,Math.max(.085,.48*od));
        double[] run=LineGeometry.step(key.facing()),up=axis==0?new double[]{0,0,-1}:new double[]{0,1,0};
        var body=new Frame(CENTRE,run,up);
        mesh.prism(body,radius,-.72*half,.72*half,sides,Material.FLANGE);
        mesh.frustum(body,.82*radius,radius,-half,-.72*half,sides,Material.FLANGE);
        mesh.frustum(body,radius,.82*radius,.72*half,half,sides,Material.FLANGE);
        double aperture=accessory?Math.min(.43*od,.75*radius):0;
        for(int sign:new int[]{-1,1})mesh.annulus(body,aperture,.82*radius,sign*half,sides,sign,Material.FLANGE);
        if(aperture>0)mesh.bore(body,aperture,-half,half,sides,Material.PIPE);
        mesh.hoop(body,radius,radius+.001,-.009,.009,sides,Material.OPENING);
        double bolt=Math.min(.025,Math.max(.006,.05*od));
        for(int sign:new int[]{-1,1})for(int i=0;i<4;i++)
            mesh.cylinder(new Frame(body.at(Math.PI/4+i*Math.PI/2,.90*radius,sign*half),new double[]{sign*run[0],sign*run[1],sign*run[2]},up),bolt,0,bolt,6,Material.BOLT);
        double[] side=cross(up,run);
        for(var normal:List.of(up,side,new double[]{-up[0],-up[1],-up[2]},new double[]{-side[0],-side[1],-side[2]}))
            MoverPortMarkings.fittingArrow(mesh,CENTRE,run,normal,radius+.001,1.25*half,.62*radius);
        if(!accessory)for(int face:new int[]{axis,axis^1}) {
            var end=LineGeometry.end(key,face,od);
            end=new LineGeometry.End(end.outside(),true,end.reducer(),end.reducer()+PipeSizes.flangeThickness(end.outside())+PipeSizes.hub(end.outside()),end.joined());
            LineGeometry.arm(mesh,face,half,od,end,LineGeometry.ref(face),sides,true);
        }
        return List.copyOf(mesh.cell(0,0,0));
    }
}
