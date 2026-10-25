package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.List;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/** Upright line-mounted pump (Grundfos TP) and geared compressor (Sundyne LMC) silhouettes. */
public final class InlineMoverGeometry {
    private static final double[] CENTRE={.5,.5,.5},UP={0,1,0};
    private InlineMoverGeometry(){}
    /** Straight, end or isolated sections, with a horizontal run axis. */
    public static boolean supports(int mask,int facing){
        if(!SightGlassGeometry.supports(mask))return false;
        int axis=mask==0?facing:Integer.numberOfTrailingZeros(mask);
        return axis>=2&&axis<6;
    }
    public static List<Polygon> polygons(LineGeometry.Key key,boolean compressor,boolean item){
        if(!supports(key.connections(),key.facing()))throw new IllegalArgumentException("Inline movers require horizontal straight pipe");
        double od=PipeSizes.outsideOf(key.outside()),r=Math.max(compressor?.15:.12,(compressor?.64:.57)*od+.04),h=Math.min(.29,Math.max(.12,.12+.16*od));
        int n=Math.max(16,PipeSizes.sides(2*r)),axis=key.facing()&~1;var along=LineGeometry.step(axis|1);
        var body=new Mesh();var run=new Frame(CENTRE,along,UP);
        body.frustum(run,.72*r,r,-h,-.55*h,n,Material.FLANGE);
        body.prism(run,r,-.55*h,.55*h,n,Material.FLANGE);
        body.frustum(run,r,.72*r,.55*h,h,n,Material.FLANGE);
        double hole=item?Math.min(.44*od,.60*r):0;
        for(int sign:new int[]{-1,1})body.annulus(run,hole,.72*r,sign*h,n,sign,Material.FLANGE);
        if(item)body.bore(run,hole,-h,h,n,Material.OPENING);
        var arrows=MoverPortMarkings.fittingOverhanging(LineGeometry.overhanging(body),key.facing(),-.46*h,.46*h);
        var up=new Frame(CENTRE,UP,along);double motorBase;
        if(compressor){
            // Broad geared casing and bolted horizontal split, then the LMC's tall coupling stand.
            double split=.68*r,gearTop=r+.18+.16*od;
            body.frustum(up,.68*r,.85*r,.50*r,split,n,Material.MACHINE);
            body.ring(up,.70*r,1.02*r,split-.025,split+.025,n,3,Material.FLANGE);
            body.bolts(up,od,.74*r,1.02*r,split+.025,1,Material.BOLT);
            body.frustum(up,.85*r,.55*r,split+.025,gearTop,n,Material.MACHINE);
            body.annulus(up,0,.55*r,gearTop,n,1,Material.MACHINE);
            double neck=.15+.20*od;motorBase=gearTop+neck;
            stand(body,up,.43*r,gearTop,motorBase,along);
            // Oil-filter canister is a visible secondary feature of the LMC reference, purely decorative.
            var oil=up.at(Math.PI/2,1.03*r,split+.14);var can=new Frame(oil,UP,along);
            body.cylinder(can,Math.max(.025,.12*r),-.12-.08*od,.035,12,Material.MACHINE);
        }else{
            double shoulder=.78*r,neck=.10+.15*od;motorBase=shoulder+neck;
            body.frustum(up,.65*r,.40*r,.55*r,shoulder,n,Material.MACHINE);
            stand(body,up,.38*r,shoulder,motorBase,along);
        }
        double rm=Math.max(compressor?.11:.09,(compressor?.32:.27)*od+.06),height=(compressor?.42:.30)+(compressor?.66:.45)*od;
        motor(body,up,rm,motorBase,height,along,n);
        if(!item)for(int face:new int[]{axis,axis^1}){
            var end=LineGeometry.end(key,face,od);
            end=new LineGeometry.End(end.outside(),true,end.reducer(),end.reducer()+PipeSizes.flangeThickness(end.outside())+PipeSizes.hub(end.outside()),end.joined());
            LineGeometry.arm(body,face,h,od,end,LineGeometry.ref(face),PipeSizes.sides(od),true);
        }
        var result=new ArrayList<>(LineGeometry.overhanging(body));result.addAll(arrows);return List.copyOf(result);
    }
    private static void stand(Mesh m,Frame up,double r,double low,double high,double[] along){
        m.ring(up,0,1.22*r,low-.012,low+.012,16,3,Material.FLANGE);
        m.cylinder(up,.24*r,low,high,10,Material.BOLT);
        for(int i=0;i<4;i++)m.bar(up.at(i*Math.PI/2,r,low),up.at(i*Math.PI/2,r,high),Math.max(.018,.18*r),along,Material.MACHINE);
        m.ring(up,0,1.22*r,high-.012,high+.012,16,3,Material.FLANGE);
    }
    private static void motor(Mesh m,Frame up,double radius,double low,double height,double[] along,int sides){
        double top=low+height,cowl=Math.max(.045,.18*radius);
        m.cylinder(up,radius,low,top,sides,Material.MOTOR);
        for(int i=0;i<16;i++)m.bar(up.at(i*Math.PI/8,radius+.002,low+.025),up.at(i*Math.PI/8,radius+.002,top-.015),Math.max(.009,.07*radius),along,Material.MOTOR);
        m.cylinder(up,1.045*radius,top-.02,top+cowl,sides,Material.MOTOR);
        m.cylinder(up,.92*radius,top+cowl,top+cowl+.018,sides,Material.MOTOR);
        var box=up.at(Math.PI/2,radius+.045,low+.50*height);double w=Math.max(.045,.33*radius);
        m.box(new double[]{box[0]-w,box[1]-w,box[2]-w},new double[]{box[0]+w,box[1]+w,box[2]+w},ALL_FACES,Material.MOTOR);
    }
}
