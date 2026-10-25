package com.wormzjl.icheme.runtime.fluid;

import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import java.util.*;

/** Pure adapter from registered block identities to scientific islands; never reads chunks or block entities. */
public final class PhysicalFluidTopology {
    private PhysicalFluidTopology() {}
    public enum Direction {
        DOWN(0,-1,0),UP(0,1,0),NORTH(0,0,-1),SOUTH(0,0,1),WEST(-1,0,0),EAST(1,0,0);
        final int x,y,z;
        Direction(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
    }
    public record Position(String dimension,int x,int y,int z) {
        public Position {Objects.requireNonNull(dimension);if(dimension.isBlank())throw new IllegalArgumentException("Missing dimension");}
        Position offset(Direction d){return new Position(dimension,Math.addExact(x,d.x),Math.addExact(y,d.y),Math.addExact(z,d.z));}
    }
    /** A reservoir's phase outlet, or a mover's explicitly directed process connection. Stable checkpoint codes. */
    public enum ConnectionRole {
        VESSEL(0),SUCTION(1),DISCHARGE(2);
        private final int code;
        ConnectionRole(int code){this.code=code;}
        public int code(){return code;}
        public static ConnectionRole fromCode(int code){return switch(code){case 0->VESSEL;case 1->SUCTION;case 2->DISCHARGE;default->throw new IllegalArgumentException("Unknown connection role "+code);};}
    }
    /** A multi-cell device's nozzle: the face of {@code cell} where a pipe meets it, its phase class and connection role. */
    public record Port(Position cell,Direction face,PassiveNetwork.PhasePort port,ConnectionRole role) {
        public Port(Position cell,Direction face,PassiveNetwork.PhasePort port){this(cell,face,port,ConnectionRole.VESSEL);}
        public Port{Objects.requireNonNull(cell);Objects.requireNonNull(face);Objects.requireNonNull(port);Objects.requireNonNull(role);}
    }
    /** Where a device meets the next block: one of its cells and the direction out of it. */
    public record Contact(Position cell,Direction direction) {}
    /** An installed pipe cutoff. Codes are part of the checkpoint, independent of enum ordering. */
    public enum Cutoff {
        NONE(0),OPEN(1),CLOSED(2),CHECK(3);
        private final int code;
        Cutoff(int code){this.code=code;}
        public int code(){return code;}
        public Cutoff toggled(){return switch(this){case OPEN->CLOSED;case CLOSED->OPEN;case CHECK->CHECK;case NONE->throw new IllegalStateException("No pipe cutoff installed");};}
        public static Cutoff fromCode(int code){return switch(code){case 0->NONE;case 1->OPEN;case 2->CLOSED;case 3->CHECK;default->throw new IllegalArgumentException("Unknown pipe cutoff "+code);};}
    }
    public static FlowControl.Mover defaultMover(TopologyCompiler.Kind kind){return switch(kind){case PUMP->new FlowControl.Pump(.01,500000);case COMPRESSOR->new FlowControl.Compressor(.05,3);default->throw new IllegalArgumentException("Not a mover");};}
    /** The most ports a vessel registers. */
    public static final int MAXIMUM_PORTS=64;
    /**
     * A registered device. One block occupies {@code position} and meets its neighbours through its faces; a vessel with
     * {@code ports} occupies its node cell {@code position} (where its level head is measured from, its lowest inside
     * cell, or a mover's suction cell) and its ports' cells, and meets pipes only at its ports.
     */
    public record Device(long id,Position position,TopologyCompiler.Kind kind,Direction facing,
                         PipeResistance.Geometry geometry,FlowControl control,List<Port> ports,boolean generatorEnabled,Cutoff cutoff,boolean inlineFilter) {
        public Device(long id,Position position,TopologyCompiler.Kind kind,Direction facing,PipeResistance.Geometry geometry,FlowControl control,List<Port> ports,boolean generatorEnabled,Cutoff cutoff){this(id,position,kind,facing,geometry,control,ports,generatorEnabled,cutoff,false);}
        public Device(long id,Position position,TopologyCompiler.Kind kind,Direction facing,PipeResistance.Geometry geometry,FlowControl control,List<Port> ports,boolean generatorEnabled){this(id,position,kind,facing,geometry,control,ports,generatorEnabled,Cutoff.NONE);}
        /** Authoring convenience; world placement explicitly creates generators switched off. */
        public Device(long id,Position position,TopologyCompiler.Kind kind,Direction facing,PipeResistance.Geometry geometry,FlowControl control,List<Port> ports){this(id,position,kind,facing,geometry,control,ports,true);}
        public Device(long id,Position position,TopologyCompiler.Kind kind,Direction facing,PipeResistance.Geometry geometry,FlowControl control){this(id,position,kind,facing,geometry,control,List.of());}
        public Device {
            if(id<=0)throw new IllegalArgumentException("Physical identities must be positive");
            Objects.requireNonNull(position);Objects.requireNonNull(kind);Objects.requireNonNull(facing);Objects.requireNonNull(geometry);Objects.requireNonNull(control);
            Objects.requireNonNull(cutoff);
            if(inlineFilter&&(kind!=TopologyCompiler.Kind.PIPE||cutoff!=Cutoff.NONE||!(control instanceof FlowControl.Passive)))throw new IllegalArgumentException("Invalid pipe filter accessory");
            if(kind!=TopologyCompiler.Kind.PIPE&&cutoff!=Cutoff.NONE)throw new IllegalArgumentException("Only a pipe has a cutoff accessory");
            boolean valid=switch(kind) {case PUMP->control instanceof FlowControl.Pump;case COMPRESSOR->control instanceof FlowControl.Compressor;case VALVE->control instanceof FlowControl.PressureValve;case PIPE->control instanceof FlowControl.Passive||control instanceof FlowControl.PressureValve||control instanceof FlowControl.Mover;default->control instanceof FlowControl.Passive;};
            if(!valid)throw new IllegalArgumentException("Device/control mismatch");
            if(control instanceof FlowControl.PressureValve&&cutoff!=Cutoff.NONE)throw new IllegalArgumentException("A pressure valve cannot carry a cutoff accessory");
            if(kind==TopologyCompiler.Kind.PIPE&&control instanceof FlowControl.Mover&&(cutoff!=Cutoff.NONE||facing.y!=0))throw new IllegalArgumentException("An inline mover needs a horizontal pipe without a cutoff");
            if(kind!=TopologyCompiler.Kind.GENERATOR&&!generatorEnabled)throw new IllegalArgumentException("Only a generator has a source switch");
            ports=List.copyOf(ports);
            if(ports.isEmpty()&&control instanceof FlowControl.Mover mover&&!mover.enabled())throw new IllegalArgumentException("Only constructed movers have a power switch");
            if(ports.size()>MAXIMUM_PORTS)throw new IllegalArgumentException("Too many device ports: "+ports.size());
            var seen=new HashSet<Contact>();
            int suction=0,discharge=0;
            for(var port:ports) {
                if(!port.cell.dimension().equals(position.dimension())||!seen.add(new Contact(port.cell,port.face)))throw new IllegalArgumentException("Invalid device port");
                if(kind==TopologyCompiler.Kind.RESERVOIR) {
                    if(port.role!=ConnectionRole.VESSEL)throw new IllegalArgumentException("A reservoir needs vessel ports");
                } else if(kind==TopologyCompiler.Kind.PUMP||kind==TopologyCompiler.Kind.COMPRESSOR) {
                    if(port.port!=PassiveNetwork.PhasePort.BULK)throw new IllegalArgumentException("A mover needs bulk ports");
                    switch(port.role) {
                        case SUCTION->{suction++;if(!port.cell.equals(position))throw new IllegalArgumentException("A mover's node must be its suction cell");}
                        case DISCHARGE->discharge++;
                        case VESSEL->throw new IllegalArgumentException("A mover needs suction and discharge ports");
                    }
                } else throw new IllegalArgumentException("Only reservoirs, pumps and compressors have ports");
            }
            if(!ports.isEmpty()&&kind!=TopologyCompiler.Kind.RESERVOIR&&(ports.size()!=2||suction!=1||discharge!=1))
                throw new IllegalArgumentException("A mover needs exactly one suction and one discharge port");
        }
        boolean boundary(){return kind==TopologyCompiler.Kind.RESERVOIR||kind==TopologyCompiler.Kind.GENERATOR||kind==TopologyCompiler.Kind.VOID;}
        /** Includes an installed filter without changing the mechanical host identity. */
        public boolean filter(){return inlineFilter||kind==TopologyCompiler.Kind.FILTER;}
        /** Mechanical pipes compile through the existing valve or filter law when fitted. */
        public TopologyCompiler.Kind hydraulicKind(){
            if(inlineFilter)return TopologyCompiler.Kind.FILTER;
            if(kind==TopologyCompiler.Kind.PIPE)return switch(control){case FlowControl.Pump p->TopologyCompiler.Kind.PUMP;case FlowControl.Compressor c->TopologyCompiler.Kind.COMPRESSOR;case FlowControl.PressureValve v->TopologyCompiler.Kind.VALVE;default->kind;};
            return kind;
        }
        boolean actuator(){var hydraulic=hydraulicKind();return hydraulic==TopologyCompiler.Kind.PUMP||hydraulic==TopologyCompiler.Kind.COMPRESSOR||hydraulic==TopologyCompiler.Kind.VALVE;}
        /** Copy edits preserve accessories, ports and the independent generator switch. */
        public Device withGeometry(PipeResistance.Geometry value){return new Device(id,position,kind,facing,value,control,ports,generatorEnabled,cutoff,inlineFilter);}
        public Device withFacing(Direction value){return new Device(id,position,kind,value,geometry,control,ports,generatorEnabled,cutoff,inlineFilter);}
        public Device withCutoff(Direction axis,Cutoff value){return new Device(id,position,kind,axis,geometry,control,ports,generatorEnabled,value,inlineFilter);}
        public Device withInlineFilter(Direction axis,boolean installed){return new Device(id,position,kind,axis,geometry,control,ports,generatorEnabled,cutoff,installed);}
        /** Every cell the registry finds this device at: its own, and its ports'. */
        public Set<Position> cells() {
            var cells=new LinkedHashSet<Position>();cells.add(position);for(var port:ports)cells.add(port.cell);return cells;
        }
        /** Where it meets the next block: each face it connects through, or a vessel's ports. */
        public List<Contact> contacts() {
            var contacts=new ArrayList<Contact>(ports.isEmpty()?6:ports.size());
            if(ports.isEmpty()){for(var direction:Direction.values())if(connects(direction))contacts.add(new Contact(position,direction));}
            else for(var port:ports)contacts.add(new Contact(port.cell,port.face));
            return contacts;
        }
        /** Whether it meets the next block out of {@code cell} towards {@code outward}. */
        boolean meets(Position cell,Direction outward) {
            if(ports.isEmpty())return cell.equals(position)&&connects(outward);
            for(var port:ports)if(port.face==outward&&port.cell.equals(cell))return true;
            return false;
        }
        /** Static fit, independent of power/cutoff state or whether the neighbouring chunk is loaded. */
        boolean mechanicallyMeets(Position cell,Direction outward) {
            if(ports.isEmpty())return cell.equals(position)&&mechanicallyConnects(outward);
            for(var port:ports)if(port.face==outward&&port.cell.equals(cell))return true;
            return false;
        }
        /**
         * Step 1 of the network reduction stage, connectivity (owner decision D20: only pipes connect): this device, out of
         * its cell {@code from} towards {@code direction}, and {@code other} in the next cell are joined when each meets
         * the other there and at least one of them is a pipe block. Two pieces of equipment side by side (a tank, generator,
         * void, pump, compressor, valve or filter against another) exchange nothing.
         */
        boolean joins(Device other,Position from,Direction direction){return meets(from,direction)&&other.meets(from.offset(direction),opposite(direction))&&(kind==TopologyCompiler.Kind.PIPE||other.kind==TopologyCompiler.Kind.PIPE);}
        /** A generator or a void (owner decision D17): islands meet at it without being joined by it. */
        boolean fixed(){return TopologyCompiler.fixed(kind);}
        /** Off closes a generator's hydraulic contacts; its registered identity and fixed source specification stay put. */
        boolean connects(Direction direction){return generatorEnabled&&cutoff!=Cutoff.CLOSED&&mechanicallyConnects(direction);}
        /** Fitting joints and bore inheritance remain axial even when hydraulic contacts are closed. */
        public boolean mechanicallyConnects(Direction direction){return !actuator()&&!filter()&&cutoff==Cutoff.NONE||direction.x*facing.x+direction.y*facing.y+direction.z*facing.z!=0;}
        /** The driven side is the actual discharge nozzle, independent of the node's position and the model's heading. */
        boolean outletAt(Position cell,Direction face) {
            if(!actuator())return false;
            if(ports.isEmpty())return cell.equals(position)&&face==facing;
            for(var port:ports)if(port.cell.equals(cell)&&port.face==face)return port.role==ConnectionRole.DISCHARGE;
            return false;
        }
        /**
         * The port a pipe end met out of {@code cell} towards {@code face} compiles to (plan 4.1, default A1 of the
         * phase-ports batch): a vessel's port its declared class; a one-block tank's {@code UP} face the top outlet
         * (VAPOR), {@code DOWN} the bottom outlet (LIQUID), a horizontal face the side outlet (BULK); every other end
         * (generator, void, junction, actuator, filter) BULK.
         */
        PassiveNetwork.PhasePort portAt(Position cell,Direction face) {
            if(kind!=TopologyCompiler.Kind.RESERVOIR)return PassiveNetwork.PhasePort.BULK;
            for(var port:ports)if(port.face==face&&port.cell.equals(cell))return port.port;
            return face==Direction.UP?PassiveNetwork.PhasePort.VAPOR:face==Direction.DOWN?PassiveNetwork.PhasePort.LIQUID:PassiveNetwork.PhasePort.BULK;
        }
    }
    public record View(int islandIndex,long pipeId,boolean forward,double tap) { public View(int islandIndex,long pipeId,boolean forward){this(islandIndex,pipeId,forward,.5);} }
    public record Island(Set<Long> physicalIds,PassiveNetwork graph,Optional<String> error) {
        public Island {physicalIds=Set.copyOf(physicalIds);Objects.requireNonNull(graph);Objects.requireNonNull(error);}
    }
    /**
     * {@code tankFaces}: for every tank (RESERVOIR device id), the face each of its compiled connections leaves it by
     * (compiled pipe id to the direction from the tank to its neighbour), the face its port was compiled from; the tank
     * page names its connections with it.
     */
    public record Compiled(List<Island> islands,Map<Long,List<View>> pipeViews,Map<Long,String> diagnostics,Map<Long,Map<Long,Direction>> tankFaces) {
        public Compiled {
            islands=List.copyOf(islands);var copied=new HashMap<Long,List<View>>();pipeViews.forEach((id,views)->copied.put(id,List.copyOf(views)));pipeViews=Map.copyOf(copied);diagnostics=Map.copyOf(diagnostics);
            var faces=new HashMap<Long,Map<Long,Direction>>();tankFaces.forEach((id,byPipe)->faces.put(id,Map.copyOf(byPipe)));tankFaces=Map.copyOf(faces);
        }
    }
    private record Point(double x,double y,double z) {}
    public static long filterIdentity(long physicalId){return Long.MIN_VALUE+physicalId;}
    public static Compiled compile(Collection<Device> devices,Map<Long,PassiveNetwork.Reservoir> boundaryStates) {
        return compile(devices,boundaryStates,Map.of(),boundaryStates.values().stream().findFirst().map(PassiveNetwork.Reservoir::state).orElse(null));
    }
    public static Compiled compile(Collection<Device> devices,Map<Long,PassiveNetwork.Reservoir> boundaryStates,Map<Long,InlineFilter> filterStock,com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics.State idleSeed) {
        var physical=new TreeMap<Long,Device>();var positions=new HashMap<Position,Device>();var points=new HashMap<Long,Point>();
        var nodes=new ArrayList<TopologyCompiler.Node>();var links=new ArrayList<TopologyCompiler.Link>();var linkOwner=new HashMap<Long,Long>();
        for(var d:devices) {
            if(physical.putIfAbsent(d.id,d)!=null)throw new IllegalArgumentException("Duplicate physical identity or position");
            for(var cell:d.cells())if(positions.putIfAbsent(cell,d)!=null)throw new IllegalArgumentException("Duplicate physical identity or position");
            var p=d.position;nodes.add(new TopologyCompiler.Node(d.id,d.hydraulicKind(),p.y));points.put(d.id,new Point(p.x,p.y,p.z));
            if(d.boundary()) {
                var state=boundaryStates.get(d.id);
                var expected=switch(d.kind){case RESERVOIR->PassiveNetwork.NodeKind.RESERVOIR;case GENERATOR->PassiveNetwork.NodeKind.GENERATOR;case VOID->PassiveNetwork.NodeKind.VOID;default->throw new AssertionError();};
                if(state==null||state.id()!=d.id||state.kind()!=expected||state.elevation()!=p.y)throw new IllegalArgumentException("Missing/incompatible authoritative boundary state");
            }
        }
        long virtual=-1,linkId=1;
        // The face of every link with a tank (RESERVOIR) end, as the direction from the tank to its neighbour, and the port
        // a pipe end at that tank compiles to (Device.portAt). Generators and voids stay BULK.
        var tankFaces=new HashMap<Long,Direction>();var tankPorts=new HashMap<Long,PassiveNetwork.PhasePort>();var outletLinks=new HashSet<Long>();
        for(var d:physical.values())for(var contact:d.contacts()) {
            var direction=contact.direction();var next=contact.cell().offset(direction);
            var other=positions.get(next);
            if(other==null||other.id<=d.id||!d.joins(other,contact.cell(),direction))continue;
            if(d.boundary()||other.boundary()) {
                var owner=d.boundary()?other:d;links.add(new TopologyCompiler.Link(linkId,d.id,other.id,scaled(owner.geometry,.5)));
                if(d.kind==TopologyCompiler.Kind.RESERVOIR){tankFaces.put(linkId,direction);tankPorts.put(linkId,d.portAt(contact.cell(),direction));}
                else if(other.kind==TopologyCompiler.Kind.RESERVOIR){tankFaces.put(linkId,opposite(direction));tankPorts.put(linkId,other.portAt(next,opposite(direction)));}
                if(d.outletAt(contact.cell(),direction)||other.outletAt(next,opposite(direction)))outletLinks.add(linkId);
                linkOwner.put(linkId++,owner.id);
            } else {
                long middle=virtual--;var a=points.get(d.id);var b=points.get(other.id);var p=new Point((a.x+b.x)/2,(a.y+b.y)/2,(a.z+b.z)/2);
                points.put(middle,p);nodes.add(new TopologyCompiler.Node(middle,TopologyCompiler.Kind.PIPE,p.y));
                links.add(new TopologyCompiler.Link(linkId,d.id,middle,half(d,other)));if(d.outletAt(contact.cell(),direction))outletLinks.add(linkId);linkOwner.put(linkId++,d.id);
                links.add(new TopologyCompiler.Link(linkId,middle,other.id,half(other,d)));if(other.outletAt(next,opposite(direction)))outletLinks.add(linkId);linkOwner.put(linkId++,other.id);
            }
        }
        var checks=new HashSet<Long>();for(var d:physical.values())if(d.cutoff==Cutoff.CHECK)checks.add(d.id);
        var compiled=TopologyCompiler.compile(nodes,links,checks);var byLink=new HashMap<Long,TopologyCompiler.Link>();for(var link:links)byLink.put(link.id(),link);
        var diagnostics=new HashMap<Long,String>();
        for(long link:compiled.deadEndLinks())diagnostics.put(linkOwner.get(link),"NO FLOW: dead-end pipe");
        for(long link:compiled.unanchoredLinks())diagnostics.put(linkOwner.get(link),"NO FLOW: no reservoir or boundary");
        for(long pump:compiled.invalidPumpCycles())diagnostics.put(pump,"ERROR: pump needs two ports without a zero-storage bypass");
        var groups=new ArrayList<>(compiled.islands());var included=new HashSet<Long>();groups.forEach(included::addAll);
        for(var d:physical.values())if(d.boundary()&&!included.contains(d.id))groups.add(Set.of(d.id));
        var retained=new HashMap<Long,TopologyCompiler.Node>();for(var node:compiled.retainedNodes())retained.put(node.id(),node);
        var islands=new ArrayList<Island>();var views=new HashMap<Long,List<View>>();var pipeIdentities=new HashSet<Long>();var faces=new HashMap<Long,Map<Long,Direction>>();
        // Groups share only fixed boundaries (a generator or a void belongs to every group that reaches it, owner decision
        // D17), so each run belongs to the group of its first node that is not a fixed boundary: an end, or else the node
        // its first segment leads to (a run between two fixed boundaries passes through at least one pipe block, since two
        // boundaries side by side never connect). Bucketed once, in run order, so assembling the islands reads every run once.
        var groupOf=new HashMap<Long,Integer>();for(int g=0;g<groups.size();g++)for(long id:groups.get(g))if(!fixedBoundary(physical.get(id)))groupOf.put(id,g);
        var runsOf=new ArrayList<List<TopologyCompiler.Run>>(groups.size());for(int g=0;g<groups.size();g++)runsOf.add(new ArrayList<>());
        for(var run:compiled.runs()) {
            long owner=!fixedBoundary(physical.get(run.first()))?run.first():!fixedBoundary(physical.get(run.second()))?run.second():other(byLink.get(run.segments().getFirst().linkId()),run.first());
            Integer g=groupOf.get(owner);if(g!=null)runsOf.get(g).add(run);
        }
        for(int group=0;group<groups.size();group++) {
            var members=groups.get(group);
            var boundaries=members.stream().filter(boundaryStates::containsKey).sorted().toList();
            if(boundaries.isEmpty()){for(long id:members)if(physical.containsKey(id))diagnostics.put(id,"NO FLOW: no reservoir or boundary");continue;}
            // A junction is built with the island's first boundary state as a placeholder; its start
            // guess is decided where it is minted, the island's fixed point (PassiveNetwork.mintJunctions,
            // decision D54), and never with any boundary's stock.
            // Solids are stock: carrying them over mints the generator's whole charge of particles on
            // every junction the compiler creates, on every topology edit, including the junction on
            // the far side of a filter edge, where no connection can ever deliver them. See
            // documentation/FULL_TANK_SOLIDS_EVENT.md.
            var boundarySeed=boundaryStates.get(boundaries.getFirst()).state();
            var seed=boundarySeed.solids().empty()?boundarySeed:boundarySeed.withSolids(com.wormzjl.icheme.science.fluid.state.SolidInventory.EMPTY);
            var reservoirs=new ArrayList<PassiveNetwork.Reservoir>();var indices=new HashMap<Long,Integer>();
            for(long id:new TreeSet<>(members))if(retained.containsKey(id)) {
                indices.put(id,reservoirs.size());var node=retained.get(id);var boundary=boundaryStates.get(id);
                reservoirs.add(boundary!=null?boundary:new PassiveNetwork.Reservoir(id,node.elevation(),seed,PassiveNetwork.NodeKind.JUNCTION));
            }
            var pipes=new ArrayList<PassiveNetwork.Pipe>();int islandIndex=islands.size();var positiveFilterEdges=new HashMap<Long,Set<Long>>();
            for(var run:runsOf.get(group)) {
                // A passive return to the same zero-holdup node has no driving pressure or owned stock.
                if(run.first()==run.second()){for(var segment:run.segments())diagnostics.put(linkOwner.get(segment.linkId()),"NO FLOW: passive return loop");continue;}
                var start=physical.get(run.first());var end=physical.get(run.second());
                var startLink=byLink.get(run.segments().getFirst().linkId());var endLink=byLink.get(run.segments().getLast().linkId());
                boolean fromStart=start!=null&&start.actuator()&&outletLinks.contains(startLink.id()),fromEnd=end!=null&&end.actuator()&&outletLinks.contains(endLink.id());
                var startPort=endPort(start,startLink,tankPorts);var endPort=endPort(end,endLink,tankPorts);
                var sections=run.segments().stream().map(TopologyCompiler.Segment::geometry).toList();
                int a=indices.get(run.first()),b=indices.get(run.second());
                long pipeId=runIdentity(run,linkOwner,0);if(!pipeIdentities.add(pipeId))throw new IllegalStateException("Compiled pipe identity collision");
                if(fromStart&&fromEnd) {
                    int middle=reservoirs.size();reservoirs.add(new PassiveNetwork.Reservoir(virtual--,.5*(reservoirs.get(a).elevation()+reservoirs.get(b).elevation()),seed,PassiveNetwork.NodeKind.JUNCTION));
                    var halves=sections.stream().map(g->scaled(g,.5)).toList();
                    pipes.add(new PassiveNetwork.Pipe(pipeId,a,middle,halves,control(start,compiled.invalidPumpCycles())).withPorts(startPort,PassiveNetwork.PhasePort.BULK));mapViews(views,run,linkOwner,physical,islandIndex,pipeId,true,true);
                    pipeId=runIdentity(run,linkOwner,1);if(!pipeIdentities.add(pipeId))throw new IllegalStateException("Compiled pipe identity collision");
                    pipes.add(new PassiveNetwork.Pipe(pipeId,b,middle,halves,control(end,compiled.invalidPumpCycles())).withPorts(endPort,PassiveNetwork.PhasePort.BULK));mapViews(views,run,linkOwner,physical,islandIndex,pipeId,false,true);
                } else {
                    // The ports follow the ends through the swap: a run driven from its end is stated from b to a.
                    pipes.add(new PassiveNetwork.Pipe(pipeId,fromEnd?b:a,fromEnd?a:b,sections,fromStart?control(start,compiled.invalidPumpCycles()):fromEnd?control(end,compiled.invalidPumpCycles()):new FlowControl.Passive())
                            .withPorts(fromEnd?endPort:startPort,fromEnd?startPort:endPort)
                            .withForbiddenDirections(checkRestrictions(start,end,startLink,endLink,points,fromEnd)));
                    mapViews(views,run,linkOwner,physical,islandIndex,pipeId,!fromEnd);
                    // A tank is never an actuator, so only a run with at most one driven end has a tank end.
                    if(start!=null&&start.kind==TopologyCompiler.Kind.RESERVOIR)faces.computeIfAbsent(start.id,k->new HashMap<>()).put(pipeId,tankFaces.get(startLink.id()));
                    if(end!=null&&end.kind==TopologyCompiler.Kind.RESERVOIR)faces.computeIfAbsent(end.id,k->new HashMap<>()).put(pipeId,tankFaces.get(endLink.id()));
                    if(start!=null&&start.filter()&&positiveSide(start,points.get(other(startLink,run.first()))))positiveFilterEdges.computeIfAbsent(start.id,k->new HashSet<>()).add(pipeId);
                    if(end!=null&&end.filter()&&positiveSide(end,points.get(other(endLink,run.second()))))positiveFilterEdges.computeIfAbsent(end.id,k->new HashSet<>()).add(pipeId);
                }
            }
            for(long id:new TreeSet<>(members))if(physical.containsKey(id)&&physical.get(id).filter()){
                int negative=indices.get(id),positive=reservoirs.size();var device=physical.get(id);
                reservoirs.add(new PassiveNetwork.Reservoir(virtual--,device.position.y,seed,PassiveNetwork.NodeKind.JUNCTION));
                var positiveEdges=positiveFilterEdges.getOrDefault(id,Set.of());
                for(int i=0;i<pipes.size();i++){var p=pipes.get(i);if(positiveEdges.contains(p.id()))pipes.set(i,new PassiveNetwork.Pipe(p.id(),p.first()==negative?positive:p.first(),p.second()==negative?positive:p.second(),p.sections(),p.control(),p.blockedDirections(),p.filter(),p.firstPort(),p.secondPort(),p.forbiddenDirections()));}
                long identity=filterIdentity(id);pipes.add(new PassiveNetwork.Pipe(identity,negative,positive,device.geometry).withFilter(filterStock.getOrDefault(id,InlineFilter.empty())));
                views.put(id,List.of(new View(islandIndex,identity,true)));
            }
            var ids=new TreeSet<Long>();for(long id:members)if(physical.containsKey(id))ids.add(id);
            Optional<String> error=ids.stream().filter(compiled.invalidPumpCycles()::contains).findFirst().map(id->diagnostics.get(id));
            islands.add(new Island(ids,new PassiveNetwork(reservoirs,pipes),error));
        }
        for(var d:physical.values())if(d.kind==TopologyCompiler.Kind.PIPE&&!views.containsKey(d.id))diagnostics.putIfAbsent(d.id,"NO FLOW: unconnected pipe");
        // An unconnected filter still owns its cake. Equal fixed property ports have exactly zero flow
        // and no finite fluid ownership; they keep the filter's simulation clock available for later edits. A filter on a
        // dead-end branch (owner decision D18, NetworkReduction step 2) is one of them: its cake stays, frozen, until a
        // topology event gives its branch a destination.
        for(var device:physical.values())if(device.filter()&&!views.containsKey(device.id)){
            boolean deadEnd=diagnostics.containsKey(device.id);
            if(idleSeed==null)throw new IllegalArgumentException("Disconnected filter needs a zero-flow property seed");
            long identity=filterIdentity(device.id);int island=islands.size();double y=device.position.y;
            var dormant=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(virtual--,y,idleSeed,PassiveNetwork.NodeKind.GENERATOR),new PassiveNetwork.Reservoir(virtual--,y,idleSeed,PassiveNetwork.NodeKind.VOID)),
                    List.of(new PassiveNetwork.Pipe(identity,0,1,device.geometry).withFilter(filterStock.getOrDefault(device.id,InlineFilter.empty())).withBlockedDirections(3)));
            islands.add(new Island(Set.of(device.id),dormant,Optional.empty()));views.put(device.id,List.of(new View(island,identity,true)));diagnostics.put(device.id,deadEnd?"NO FLOW: dead-end filter":"NO FLOW: disconnected filter");
        }
        return new Compiled(islands,views,diagnostics,faces);
    }
    /** Restrictions are in the run's traversal order, then swapped with a discharge-driven end. */
    private static int checkRestrictions(Device start,Device end,TopologyCompiler.Link first,TopologyCompiler.Link last,Map<Long,Point> points,boolean reversed) {
        int mask=0;
        if(start!=null&&start.cutoff==Cutoff.CHECK)mask|=positiveSide(start,points.get(other(first,start.id)))?2:1;
        if(end!=null&&end.cutoff==Cutoff.CHECK)mask|=positiveSide(end,points.get(other(last,end.id)))?1:2;
        return reversed?((mask&1)<<1)|((mask&2)>>1):mask;
    }
    private static long other(TopologyCompiler.Link link,long id){return link.first()==id?link.second():link.first();}
    /**
     * The port a run end compiles to: at a tank, the port its link leaves the tank by ({@link Device#portAt}); every other
     * end BULK. A run of one link from a junction into a tank has that link at both ends; only the tank's end is a port.
     */
    private static PassiveNetwork.PhasePort endPort(Device end,TopologyCompiler.Link link,Map<Long,PassiveNetwork.PhasePort> tankPorts) {
        return end!=null&&end.kind==TopologyCompiler.Kind.RESERVOIR?tankPorts.getOrDefault(link.id(),PassiveNetwork.PhasePort.BULK):PassiveNetwork.PhasePort.BULK;
    }
    /** A generator or a void: a boundary whose state nothing in the network changes, shared by every island that reaches it. */
    static boolean fixedBoundary(Device device){return device!=null&&device.fixed();}
    static Direction opposite(Direction d){return switch(d){case DOWN->Direction.UP;case UP->Direction.DOWN;case NORTH->Direction.SOUTH;case SOUTH->Direction.NORTH;case WEST->Direction.EAST;case EAST->Direction.WEST;};}
    /** Independent edits must not renumber a different island's last-interval debug history. */
    private static long runIdentity(TopologyCompiler.Run run,Map<Long,Long> owners,int part) {
        long hash=0xcbf29ce484222325L;hash=(hash^run.first())*0x100000001b3L;hash=(hash^run.second())*0x100000001b3L;
        for(var segment:run.segments())hash=(hash^owners.get(segment.linkId()))*0x100000001b3L;
        hash=(hash^part)*0x100000001b3L;return (hash&Long.MAX_VALUE)==0?1:hash&Long.MAX_VALUE;
    }
    private static FlowControl control(Device device,Set<Long> invalidPumps) {
        if(device.control instanceof FlowControl.Pump pump&&invalidPumps.contains(device.id))return new FlowControl.Pump(0,pump.maximumAddedPressure());
        if(device.control instanceof FlowControl.Compressor compressor&&invalidPumps.contains(device.id))return new FlowControl.Compressor(0,compressor.maximumPressureRatio());
        return device.control;
    }
    private static boolean positiveSide(Device device,Point neighbor){var p=device.position;var d=device.facing;return (neighbor.x-p.x)*d.x+(neighbor.y-p.y)*d.y+(neighbor.z-p.z)*d.z>0;}
    /**
     * A device's half of its link to {@code line}: half its own geometry, except that a pump, compressor or valve takes the
     * bore and roughness of the pipe it is joined to (owner decision 2026-10-16, batch 2026-10-15-custom-equipment-rendering:
     * their nozzles match the line, as on a plant). It keeps its own length and minor loss. Equal bores give the old doubles.
     */
    private static PipeResistance.Geometry half(Device device,Device line) {
        var g=device.geometry;
        if(device.kind!=TopologyCompiler.Kind.PIPE&&device.actuator()&&line.kind==TopologyCompiler.Kind.PIPE)g=new PipeResistance.Geometry(g.length(),line.geometry.diameter(),line.geometry.roughness(),g.minorLoss());
        return scaled(g,.5);
    }
    private static PipeResistance.Geometry scaled(PipeResistance.Geometry g,double fraction){return new PipeResistance.Geometry(g.length()*fraction,g.diameter(),g.roughness(),g.minorLoss()*fraction);}
    private static void mapViews(Map<Long,List<View>> views,TopologyCompiler.Run run,Map<Long,Long> owners,Map<Long,Device> physical,int island,long pipe,boolean forward) {mapViews(views,run,owners,physical,island,pipe,forward,false);}
    private static void mapViews(Map<Long,List<View>> views,TopologyCompiler.Run run,Map<Long,Long> owners,Map<Long,Device> physical,int island,long pipe,boolean forward,boolean split) {
        double total=run.segments().stream().mapToDouble(s->s.geometry().length()).sum(),cursor=0;var spans=new LinkedHashMap<Long,double[]>();
        for(var segment:run.segments()){long id=owners.get(segment.linkId());double start=cursor;cursor+=segment.geometry().length();var span=spans.get(id);if(span==null)spans.put(id,new double[]{start,cursor});else span[1]=cursor;}
        for(var segment:run.segments()) {
            long id=owners.get(segment.linkId());if(physical.get(id).kind!=TopologyCompiler.Kind.PIPE)continue;
            var span=spans.get(id);double tap=(span[0]+span[1])/(2*total);if(!forward)tap=1-tap;if(split)tap=tap<=.5?tap*2:Double.NaN;var entry=new View(island,pipe,forward,tap);var list=views.computeIfAbsent(id,ignored->new ArrayList<>());if(!list.contains(entry))list.add(entry);
        }
    }
}
