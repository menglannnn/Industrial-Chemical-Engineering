package com.wormzjl.createcheme.client.gui.screens.inventory;

import com.wormzjl.createcheme.client.MaterialNames;
import com.wormzjl.createcheme.client.gui.common.*;
import com.wormzjl.createcheme.network.FluidNetwork;
import com.wormzjl.createcheme.runtime.fluid.SlurryFeed;
import com.wormzjl.createcheme.science.fluid.network.PipeResistance;
import com.wormzjl.createcheme.science.fluid.state.ParticleSize;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.createcheme.science.material.MaterialName;
import com.wormzjl.createcheme.world.inventory.FluidDeviceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;
import static com.wormzjl.createcheme.client.gui.common.ProcessUi.*;

/** Shared process controls and phase tables. Opening replays the last published snapshot; new state follows engine buckets. */
public final class FluidDeviceScreen extends AbstractContainerScreen<FluidDeviceMenu> {
    private final Map<String,NumericDraft> fields=new LinkedHashMap<>();
    private final Map<String,EditBox> editors=new LinkedHashMap<>();
    private final List<Label> labels=new ArrayList<>();
    private final ProcessScroll scroll=new ProcessScroll();
    private RelativeComposition composition;
    private ComponentDropdown componentDropdown;
    private TemperatureUnit temperatureUnit=TemperatureUnit.CELSIUS;
    private String[][] particles=new String[64][3];
    private long revision=Long.MIN_VALUE,messageRevision;
    private int page,phase=2;
    private boolean mass,templates,rebuildNext,hasConnections;
    private String search="",message="",hovered;
    private V3ClientPresets library;
    private V3ClientPresets.Snapshot mixtureSnapshot;
    private record Label(String text,int x,int y,int width){}
    private record Choice(String label,Runnable select){}
    private List<Choice> choices=List.of();

    public FluidDeviceScreen(FluidDeviceMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    private static String tr(String key,Object...args){return Component.translatable("gui.createcheme.fluid."+key,args).getString();}
    @Override protected void init(){
        imageWidth=Math.max(300,Math.min(790,width-16));imageHeight=Math.max(250,height-16);
        super.init();titleLabelY=inventoryLabelY=-1000;
        if(library==null)library=new V3ClientPresets(minecraft.gameDirectory.toPath(),name->{
            try{return minecraft.getResourceManager().getResource(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("createcheme",name.substring("assets/createcheme/".length()))).orElseThrow().open();}
            catch(java.io.IOException|NoSuchElementException e){return null;}
        });
        refresh();rebuild();
    }
    private boolean generator(){return menu.clientData()!=null&&menu.clientData().kind()==TopologyCompiler.Kind.GENERATOR;}
    private boolean pipe(){return menu.clientData()!=null&&menu.clientData().kind()==TopologyCompiler.Kind.PIPE;}
    private boolean transport(){return menu.clientData()!=null&&menu.clientData().view().pipeInfo()!=null;}
    private boolean canEdit(){return !menu.debug()&&menu.clientData()!=null;}
    private int rail(){return Math.min(210,imageWidth/3);}
    // The rail below "Operating conditions" (its fields, the filter's button and the values) is one column clipped to
    // the area above the status separator and scrolled by its own bar on the rail/table divider when it is longer than
    // that area, as the table area scrolls its rows. At 427x240 or 480x270 scaled pixels (GUI scale 2 at 854x480, the
    // automatic scale at 1920x1080) the screen is 250 GUI px high and a pump's or compressor's column does not fit.
    private static final int RAIL_STEP=10,RAIL_CONTROL=42,RAIL_LINE=30;
    private final ProcessScroll railScroll=new ProcessScroll();
    private int railBuiltOffset,hoverX=-1,hoverY=-1,clipTop=Integer.MIN_VALUE,clipBottom=Integer.MAX_VALUE;
    private int railTop(){return pipe()?78:90;}
    private int railBottom(){return imageHeight-52;}
    private int railOffset(){return railScroll.value()*RAIL_STEP;}
    private boolean inRail(int y,int height){return y>=railTop()&&y+height<=railBottom();}
    private int tableX(){return rail()+24;}
    private int tableWidth(){return imageWidth-tableX()-22;}
    // Page 4 is a tank's "Connections by face": one two-line row per connection in the table area, scrolled like the
    // other tables, so the list is readable at every GUI scale (it was on the rail, below the tank's values, and did
    // not fit below about 323 scaled pixels of height).
    private boolean tank(){return menu.clientData()!=null&&menu.clientData().kind()==TopologyCompiler.Kind.RESERVOIR;}
    private int tableTop(){return page==0?(pipe()?110:130):page==1?166:page==3?104:page==4?100:142;}
    private int rowHeight(){return page==0||page==3?18:page==4?24:25;}
    private int visibleRows(){return Math.max(1,(imageHeight-64-tableTop())/rowHeight());}
    private void refresh(){
        if(menu.messageRevision()!=messageRevision){messageRevision=menu.messageRevision();message=menu.message();}
        var data=menu.clientData();if(data==null)return;
        boolean connected=pipe()&&data.view().pipeInfo().connections().size()>1;
        if(connected!=hasConnections){hasConnections=connected;if(!connected&&page==3)page=0;rebuildNext=true;}
        if(data.view().inputRevision()==revision)return;
        boolean first=revision==Long.MIN_VALUE;revision=data.view().inputRevision();var c=data.controls();
        fields.clear();fields.put("temperature",new NumericDraft(temperatureUnit.display(c.temperature())));
        fields.put("pressure",new NumericDraft(c.pressure()));fields.put("diameter",new NumericDraft(c.diameter()));
        fields.put("volumeFlow",new NumericDraft(c.volumeFlow()));fields.put("maximumAddedPressure",new NumericDraft(c.maximumAddedPressure()));
        fields.put("maximumPressureRatio",new NumericDraft(c.maximumPressureRatio()));
        composition=new RelativeComposition(data.components(),data.molecularWeights(),c.composition());
        if(mass)composition.toggleBasis();
        loadSolids(c.solids());if(data.kind()==TopologyCompiler.Kind.FILTER)phase=3;
        else if(first&&data.view().state()!=null){
            var phases=data.view().state().phaseMoles();double largest=0;
            for(int i=0;i<phases.length;i++){double amount=Arrays.stream(phases[i]).sum();if(amount>largest){largest=amount;phase=i;}}
        }
        if(first&&pipe()&&data.view().pipeInfo()!=null){
            var phases=data.view().pipeInfo().contents().phaseMoles();double largest=0;
            for(int i=0;i<phases.length;i++){double amount=Arrays.stream(phases[i]).sum();if(amount>largest){largest=amount;phase=i;}}
        }
        templates=false;scroll.reset();rebuildNext=true;
    }
    private void loadSolids(SlurryFeed solids){
        particles=new String[64][3];int i=0;
        for(var grade:solids.grades())particles[i++]=new String[]{grade.material(),new java.math.BigDecimal(grade.size().metres()).scaleByPowerOfTen(6).toPlainString(),Double.toString(grade.massShare())};
        fields.put("solidFraction",new NumericDraft(solids.volumeFraction()*100));
    }
    private Button button(String text,int x,int y,int w,Runnable action){
        var button=addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(leftPos+x,topPos+y,w,18).build());
        button.setTooltip(Tooltip.create(Component.literal(text)));return button;
    }
    private Button action(String key,int x,int y,int w,Runnable run){
        var b=button(tr(key),x,y,w,run);b.setTooltip(Tooltip.create(Component.translatable("gui.createcheme.fluid."+key+".hint")));return b;
    }
    private EditBox field(String key,String label,int x,int y,int width){
        labels.add(new Label(label,x,y,width));
        var box=new EditBox(font,leftPos+x,topPos+y+12,width,18,Component.literal(label));
        box.setMaxLength(48);box.setValue(fields.get(key).text());box.setResponder(v->fields.get(key).text(v));
        box.setTooltip(Tooltip.create(Component.literal(label)));box.setEditable(canEdit());editors.put(key,addRenderableWidget(box));
        return box;
    }
    /** A field of the scrolled rail: its label is clipped with the rail, the box shows only when it lies wholly inside. */
    private void railField(String key,String label,int y){field(key,label,12,y,rail()-4).visible=inRail(y+12,18);}
    private int[] bottomButtons(){
        int[] widths={65,110,118,108};int natural=Arrays.stream(widths).sum(),room=imageWidth-38;
        if(room<natural)for(int i=0;i<widths.length;i++)widths[i]=widths[i]*room/natural;
        return widths;
    }
    /** Rail controls above the values: the fields a device's page edits, or a filter's recover button. */
    private int railControls(){
        var data=menu.clientData();if(data==null||!canEdit())return 0;
        return switch(data.kind()){case GENERATOR,PUMP,COMPRESSOR->2;case VALVE,VOID,PIPE,FILTER->1;default->0;};
    }
    private void changePage(int target){page=target;templates=false;scroll.reset();rebuild();}
    private void toggleBasis(){
        try{if(composition!=null){composition.toggleBasis();mass=composition.mass();}else mass=!mass;rebuild();}
        catch(IllegalArgumentException e){message=tr("invalid_composition");}
    }
    private void toggleTemperature(){
        try{
            if(fields.containsKey("temperature")){
                double kelvin=temperatureUnit.kelvin(fields.get("temperature").value());
                temperatureUnit=temperatureUnit.next();fields.put("temperature",new NumericDraft(temperatureUnit.display(kelvin)));
            }else temperatureUnit=temperatureUnit.next();
            rebuild();
        }catch(IllegalArgumentException e){message=tr("invalid_number");}
    }
    private void rebuild(){
        rebuildNext=false;String focused=editors.entrySet().stream().filter(e->e.getValue()==getFocused()).map(Map.Entry::getKey).findFirst().orElse(null);
        int cursor=focused==null?0:editors.get(focused).getCursorPosition();
        boolean dropdownOpen=componentDropdown!=null&&componentDropdown.expanded();
        clearWidgets();editors.clear();labels.clear();componentDropdown=null;
        action("overview",10,29,100,()->changePage(0)).active=page!=0;
        if(generator()&&canEdit()){
            action("composition",116,29,112,()->changePage(1)).active=page!=1;
            action("solids",234,29,100,()->changePage(2)).active=page!=2;
        }
        // The button row: Close and Apply from the left, the basis and unit toggles from the right, all four narrowed in
        // proportion when they do not fit side by side (their natural widths need 439 GUI px; a 427-wide screen has 411).
        int[] bottom=bottomButtons();int by=imageHeight-27;
        button(tr("temperature_unit",temperatureUnit.symbol()),imageWidth-10-bottom[3],by,bottom[3],this::toggleTemperature)
            .setTooltip(Tooltip.create(Component.translatable("gui.createcheme.column.temperature_unit.hint")));
        button(tr(mass?"mass_basis":"mole_basis"),imageWidth-16-bottom[3]-bottom[2],by,bottom[2],this::toggleBasis)
            .setTooltip(Tooltip.create(Component.translatable("gui.createcheme.column.composition_basis.hint")));
        action("close",10,by,bottom[0],this::onClose);
        var data=menu.clientData();if(data==null)return;
        if(canEdit()&&data.kind()!=TopologyCompiler.Kind.RESERVOIR&&data.kind()!=TopologyCompiler.Kind.FILTER)
            action("apply",16+bottom[0],by,bottom[1],this::apply);
        if(pipe()&&data.view().pipeInfo().connections().size()>1)
            action("connections",116,29,112,()->changePage(3)).active=page!=3;
        // The tab's hover hint is the outlets hint (the D11 rule), key "outlets.hint".
        if(tank())action("outlets",116,29,130,()->changePage(4)).active=page!=4;
        railBuiltOffset=railOffset();int y=railTop()-railBuiltOffset;
        if(canEdit())switch(data.kind()){
            case GENERATOR->{railField("pressure",tr("pressure_input"),y);railField("temperature",tr("temperature_input",temperatureUnit.symbol()),y+RAIL_CONTROL);}
            case PUMP->{railField("volumeFlow",tr("flow_input"),y);railField("maximumAddedPressure",tr("head_input"),y+RAIL_CONTROL);}
            case COMPRESSOR->{railField("volumeFlow",tr("flow_input"),y);railField("maximumPressureRatio",tr("ratio_input"),y+RAIL_CONTROL);}
            case VALVE,VOID->railField("pressure",tr("pressure_input"),y);
            case PIPE->railField("diameter",tr("diameter_input"),y);
            default->{}
        }
        if(data.kind()==TopologyCompiler.Kind.FILTER&&canEdit()){
            var recover=action("recover",12,y,rail()-4,()->{FluidNetwork.recoverSolids(menu,revision);message=tr("waiting");});
            recover.active=data.view().filter()!=null&&!data.view().filter().captured().empty();recover.visible=inRail(y,18);
        }
        if(page==0)buildContents();else if(page==1)buildComposition();else if(page==2)buildSolids();
        else if(page==4)configureScroll(data.view().outlets().size());else configureScroll(data.view().pipeInfo().connections().size());
        if(focused!=null&&editors.containsKey(focused)){setFocused(editors.get(focused));editors.get(focused).setCursorPosition(cursor);}
        if(componentDropdown!=null&&"search".equals(focused))componentDropdown.expanded(dropdownOpen);
    }
    private void buildContents(){
        int w=(tableWidth()-12)/4;
        for(int i=0;i<4;i++){int selected=i;button(tr("phase."+i),tableX()+i*(w+4),pipe()?62:82,w,()->{phase=selected;scroll.reset();rebuild();}).active=phase!=i;}
        configureScroll(phase==3?displayedSolids().populations().size():present().size());
    }
    private void configureScroll(int size){scroll.configure(leftPos+imageWidth-16,topPos+tableTop(),imageHeight-64-tableTop(),visibleRows(),size);}
    private void openTemplates(){
        mixtureSnapshot=library.refresh(V3ClientPresets.Kind.MIXTURE);choices=mixtureChoices();templates=true;scroll.reset();rebuild();
    }
    private List<Choice> mixtureChoices(){
        var data=menu.clientData();var list=new ArrayList<Choice>();
        for(var preset:data.presets())list.add(new Choice(preset.name(),()->{
            composition=new RelativeComposition(data.components(),data.molecularWeights(),preset.moleFractions());
            if(mass)composition.toggleBasis();loadSolids(preset.solids());templates=false;scroll.reset();message=tr("mixture_loaded");rebuild();
        }));
        if(mixtureSnapshot!=null)for(var entry:mixtureSnapshot.entries()){
            var mixture=(V3ClientPresets.Mixture)entry.preset();
            if(!data.components().containsAll(mixture.amounts().keySet()))continue;
            String name=mixture.translationKey().isBlank()?mixture.name():Component.translatable(mixture.translationKey()).getString();
            list.add(new Choice(name+" · "+tr(entry.bundled()?"bundled":"custom"),()->{
                var replacement=new RelativeComposition(data.components(),data.molecularWeights(),new double[data.components().size()]);
                replacement.load(mixture.amounts(),mixture.mass());composition=replacement;mass=mixture.mass();
                templates=false;scroll.reset();message=tr("mixture_loaded");rebuild();
            }));
        }
        return List.copyOf(list);
    }
    private void buildComposition(){
        int x=tableX(),w=tableWidth();
        if(templates){
            action("back",x,82,76,()->{templates=false;scroll.reset();rebuild();});
            action("refresh",x+82,82,90,this::openTemplates);
            action("folder",x+178,82,100,()->net.minecraft.Util.getPlatform().openPath(library.directory(V3ClientPresets.Kind.MIXTURE)));
            configureScroll(choices.size());
            for(int i=scroll.value();i<Math.min(choices.size(),scroll.value()+visibleRows());i++){
                var choice=choices.get(i);button(choice.label(),x,tableTop()+(i-scroll.value())*25,w-4,choice.select());
            }return;
        }
        int bw=Math.max(60,(w-12)/3);
        action("load_mixture",x,82,bw,this::openTemplates);
        action("new_mixture",x+bw+6,82,bw,()->{composition.clear();scroll.reset();message="";rebuild();});
        var options=new ArrayList<ComponentSearch.Option>();var data=menu.clientData();
        for(int i=0;i<data.components().size();i++)if(!composition.contains(i))
            options.add(new ComponentSearch.Option(i,material(i),data.components().get(i)));
        componentDropdown=new ComponentDropdown(font,leftPos+x,topPos+112,Math.min(360,w-4),imageHeight-178,options,search,
            value->search=value,id->{composition.add(id);search="";rebuild();});
        editors.put("search",addRenderableWidget(componentDropdown));
        var ids=composition.rows();configureScroll(ids.size());
        int amount=x+w/2,norm=amount+76;
        for(int i=scroll.value();i<Math.min(ids.size(),scroll.value()+visibleRows());i++){
            int id=ids.get(i),y=tableTop()+(i-scroll.value())*25;
            var box=new EditBox(font,leftPos+amount,topPos+y,64,18,Component.translatable("gui.createcheme.fluid.relative"));
            box.setValue(composition.get(id));box.setMaxLength(32);box.setResponder(v->composition.set(id,v));
            box.setTooltip(Tooltip.create(Component.translatable("gui.createcheme.column.relative_amount.hint")));editors.put("comp"+id,addRenderableWidget(box));
            button("×",x+w-26,y,22,()->{composition.remove(id);rebuild();}).setTooltip(Tooltip.create(Component.translatable("gui.createcheme.column.remove_component.hint")));
        }
    }
    private void buildSolids(){
        field("solidFraction",tr("solid_fraction"),tableX(),82,Math.min(160,tableWidth()));
        configureScroll(64);
        for(int i=scroll.value();i<Math.min(64,scroll.value()+visibleRows());i++)for(int c=0;c<3;c++){
            int index=i,col=c,w=tableWidth(),x=tableX()+(c==0?0:c==1?w/2:3*w/4);
            int width=c==0?w/2-6:w/4-6;
            var box=new EditBox(font,leftPos+x,topPos+142+(i-scroll.value())*25,width,18,Component.literal(tr("solid_col."+c)));
            box.setMaxLength(c==0?128:64);box.setValue(particles[i][c]==null?"":particles[i][c]);
            box.setResponder(v->particles[index][col]=v);editors.put("solid"+i+"_"+c,addRenderableWidget(box));
        }
    }
    private SlurryFeed solids(){
        var grades=new ArrayList<SlurryFeed.Grade>();
        for(var row:particles){
            if(Arrays.stream(row).allMatch(v->v==null||v.isBlank()))continue;
            if(Arrays.stream(row).anyMatch(v->v==null||v.isBlank()))throw new IllegalArgumentException(tr("invalid_solid"));
            grades.add(new SlurryFeed.Grade(row[0].strip(),ParticleSize.micrometres(row[1].strip()),Double.parseDouble(row[2])));
        }
        return new SlurryFeed(fields.get("solidFraction").value()/100,grades);
    }
    private void apply(){
        if(!canEdit())return;
        // A compressor's ratio outside its bounds is named with them (the bounds Controls validates with), not the generic message.
        if(menu.clientData().kind()==TopologyCompiler.Kind.COMPRESSOR){
            double ratio;try{ratio=fields.get("maximumPressureRatio").value();}catch(IllegalArgumentException e){ratio=Double.NaN;}
            if(!FluidNetwork.pressureRatioAllowed(ratio)){message=tr("invalid_ratio",(Object[])FluidNetwork.pressureRatioBoundsText());return;}
        }
        try{
            var controls=new FluidNetwork.Controls(temperatureUnit.kelvin(fields.get("temperature").value()),fields.get("pressure").value(),
                fields.get("diameter").value(),PipeResistance.DEFAULT_ROUGHNESS_METRES,fields.get("volumeFlow").value(),
                fields.get("maximumAddedPressure").value(),composition.moleFractions(),solids(),fields.get("maximumPressureRatio").value());
            FluidNetwork.sendEdit(menu,revision,controls);message=tr("waiting");
        }catch(IllegalArgumentException e){message=tr("invalid_input");}
    }
    private double[][] amounts(){
        var data=menu.clientData();var view=data.view();
        if(view.pipeInfo()!=null)return view.pipeInfo().contents().phaseMoles();
        return view.state()==null?new double[3][data.components().size()]:view.state().phaseMoles();
    }
    private List<Integer> present(){
        double[] values=amounts()[Math.min(2,phase)];var ids=new ArrayList<Integer>();
        for(int i=0;i<values.length;i++)if(values[i]>0)ids.add(i);return ids;
    }
    private double displayedFlow(){
        var data=menu.clientData();var view=data.view();
        if(view.pipeInfo()!=null){
            if(view.intervalSeconds()<=0)return Double.NaN;
            var stream=view.pipeInfo().contents();
            if(mass)return stream.massKg()*3600/view.intervalSeconds();
            double moles=0;for(var phase:stream.phaseMoles())for(double amount:phase)moles+=amount;
            return moles*3.6/view.intervalSeconds();
        }
        double flow=view.massFlow()*(data.kind()==TopologyCompiler.Kind.VOID?-1:1);
        if(mass||flow==0)return flow*3600;
        if(view.state()==null)return Double.NaN;
        double moles=0;for(var phase:view.state().phaseMoles())for(double amount:phase)moles+=amount;
        // The bulk mass includes suspended solids, which do not contribute fluid moles.
        return view.state().mass()>0?flow*moles/view.state().mass()*3.6:Double.NaN;
    }
    private SolidInventory displayedSolids(){
        var data=menu.clientData();if(data==null)return SolidInventory.EMPTY;var view=data.view();
        if(view.filter()!=null)return view.filter().captured();
        if(view.pipeInfo()!=null)return view.pipeInfo().contents().solids();
        return view.state()==null?SolidInventory.EMPTY:view.state().solids();
    }
    private String material(int id){
        var data=menu.clientData();String name=data.components().get(id);
        return MaterialNames.localized(data.materialNames().getOrDefault(name,MaterialName.chemical(name)));
    }
    private void text(GuiGraphics g,String value,int x,int y,int color,int width){
        String shown=font.plainSubstrByWidth(value,Math.max(0,width));g.drawString(font,shown,x,y,color,false);
        // A line cut to its column reads in full on hover, so a narrow screen drops nothing silently.
        if(shown.length()<value.length()&&hoverX>=x&&hoverX<x+width&&hoverY>=Math.max(y,clipTop)&&hoverY<Math.min(y+9,clipBottom))hovered=value;
    }
    private void railLine(GuiGraphics g,String key,String value,int y){text(g,tr(key),12,y,MUTED,rail()-2);text(g,value,12,y+12,TEXT,rail()-2);}
    private boolean railHover(int mx,int my,int y0,int y1){return mx>=12&&mx<rail()+10&&my>=Math.max(y0,railTop())&&my<Math.min(y1,railBottom());}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        ProcessUi.restoreCursor(minecraft,this);refresh();if(rebuildNext)rebuild();hovered=null;
        super.render(g,mouseX,mouseY,partialTick);
        if(hovered!=null&&(componentDropdown==null||!componentDropdown.popupContains(mouseX,mouseY))){
            var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
            for(var line:hovered.split("\\n"))lines.addAll(font.split(Component.literal(line),Math.max(160,width/2)));
            g.renderTooltip(font,lines,mouseX,mouseY);
        }
        if(componentDropdown!=null)componentDropdown.renderSuggestions(g,mouseX,mouseY);
    }
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,BG);
        g.renderOutline(leftPos,topPos,imageWidth,imageHeight,LINE);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        hoverX=mouseX-leftPos;hoverY=mouseY-topPos;
        text(g,title.getString(),10,10,TEXT,imageWidth-20);
        var data=menu.clientData();
        if(data==null){text(g,tr("waiting"),10,62,MUTED,imageWidth-20);return;}
        // A refused mover's reason ("ERROR: pump inlet not liquid ...") and a hold read in the warning colour.
        var view=data.view();if(!pipe())text(g,view.status(),10,59,warning(view.status())?WARN:MUTED,imageWidth-20);
        g.vLine(rail()+14,pipe()?60:78,imageHeight-52,LINE);g.hLine(8,imageWidth-8,imageHeight-49,LINE);
        text(g,tr("operating"),12,pipe()?62:77,ACCENT,rail());
        for(var label:labels)if(label.x()>=tableX())text(g,label.text(),label.x(),label.y(),MUTED,label.width());
        // The rail column (see railTop): drawn clipped to its area, its length sets the rail's scrollbar.
        int top=railTop(),bottom=railBottom(),start=top-railOffset();
        g.enableScissor(leftPos+1,topPos+top,leftPos+rail()+10,topPos+bottom);clipTop=top;clipBottom=bottom;
        for(var label:labels)if(label.x()<tableX())text(g,label.text(),label.x(),label.y(),MUTED,label.width());
        int end=pipe()?renderPipeRail(g,hoverX,hoverY,start):renderDeviceRail(g,start+(railControls()==0?4:railControls()*RAIL_CONTROL));
        g.disableScissor();clipTop=Integer.MIN_VALUE;clipBottom=Integer.MAX_VALUE;
        int visible=Math.max(1,(bottom-top)/RAIL_STEP),overflow=end-start-(bottom-top);
        railScroll.configure(leftPos+rail()+10,topPos+top,bottom-top,visible,visible+(overflow>0?(overflow+RAIL_STEP-1)/RAIL_STEP:0));
        if(railOffset()!=railBuiltOffset)rebuildNext=true;
        if(page==0)renderContents(g,mouseX-leftPos,mouseY-topPos);
        else if(page==1)renderComposition(g,mouseX-leftPos,mouseY-topPos);
        else if(page==3)renderConnections(g,mouseX-leftPos,mouseY-topPos);
        else if(page==4)renderOutlets(g,mouseX-leftPos,mouseY-topPos);
        else {
            int w=tableWidth();text(g,tr("solid_col.0"),tableX(),128,TEXT,w/2);
            text(g,tr("solid_col.1"),tableX()+w/2,128,TEXT,w/4);text(g,tr("solid_col.2"),tableX()+3*w/4,128,TEXT,w/4);
        }
        String status=message.isBlank()?tr("view_time",number(view.onlineTick()/20.0)):message;
        text(g,status,10,imageHeight-42,WARN,imageWidth-20);
        // Scrollbars store screen coordinates because all mouse events arrive there.
        g.pose().pushPose();g.pose().translate(-leftPos,-topPos,0);scroll.draw(g);railScroll.draw(g);g.pose().popPose();
    }
    /** A device's values on the rail from y (unscrolled: below its controls); returns the bottom of the last line. */
    private int renderDeviceRail(GuiGraphics g,int y){
        var data=menu.clientData();var view=data.view();
        if(view.state()!=null){
            var state=view.state();
            railLine(g,"temperature",number(temperatureUnit.display(state.temperature()))+" "+temperatureUnit.symbol(),y);y+=RAIL_LINE;
            railLine(g,"pressure",pressure(state.pressure()),y);y+=RAIL_LINE;
            if(data.kind()==TopologyCompiler.Kind.RESERVOIR){
                railLine(g,"volume",number(state.volume()*1000)+" L",y);y+=RAIL_LINE;
                railLine(g,"fill",number(state.fill()*100)+" %",y);y+=RAIL_LINE;
                railLine(g,"mass",number(state.mass())+" kg",y);y+=RAIL_LINE;
            }
        }
        if(data.kind()==TopologyCompiler.Kind.PIPE||data.kind()==TopologyCompiler.Kind.PUMP||data.kind()==TopologyCompiler.Kind.COMPRESSOR||data.kind()==TopologyCompiler.Kind.VOID||generator()){
            double flow=displayedFlow();
            railLine(g,"last_flow",(Double.isFinite(flow)?number(flow):"—")+(mass?" kg/h":" kmol/h"),y);y+=RAIL_LINE;
        }
        if(view.filter()!=null){
            railLine(g,"filter_load",number(view.filter().loading()*100)+" %",y);y+=RAIL_LINE;
            railLine(g,"captured",number(view.filter().captured().massKg())+" kg",y);y+=RAIL_LINE;
        }
        if(view.devicePressureChange()!=null){railLine(g,"pressure_change",pressure(view.devicePressureChange()),y);y+=RAIL_LINE;}
        // The compressor's rise over its suction (its own junction, the state this rail shows): the quantity its ratio limits.
        if(data.kind()==TopologyCompiler.Kind.COMPRESSOR&&view.devicePressureChange()!=null&&view.state()!=null&&view.state().pressure()>0){
            railLine(g,"pressure_ratio",number((view.state().pressure()+view.devicePressureChange())/view.state().pressure()),y);y+=RAIL_LINE;}
        return y-(RAIL_LINE-21);
    }
    private static boolean warning(String status){return status.contains("HELD")||status.contains("ERROR");}
    /**
     * A tank's connections by face (plan 4.3), page 4: what each outlet is and which face it leaves by, what it drew over
     * the last interval and its flow; one two-line row per connection in the table area, scrolled when they do not all fit.
     */
    private void renderOutlets(GuiGraphics g,int mx,int my){
        var outlets=menu.clientData().view().outlets();int x=tableX(),w=tableWidth();
        text(g,tr("outlets"),x,82,ACCENT,w);if(mx>=x&&mx<x+w&&my>=80&&my<92)hovered=tr("outlets.hint");
        configureScroll(outlets.size());
        for(int i=scroll.value();i<Math.min(outlets.size(),scroll.value()+visibleRows());i++){
            var outlet=outlets.get(i);int y=tableTop()+(i-scroll.value())*rowHeight();
            // No row tooltip: a screen opens with the cursor at the window centre, inside these rows at GUI scale 2 and
            // above, and a tooltip there would cover them. The hint is on the page title and on the tab button.
            text(g,outletName(outlet),x,y,MUTED,w-4);text(g,outletState(outlet),x,y+11,TEXT,w-4);g.hLine(x,x+w,y+21,LINE);
        }
        if(outlets.isEmpty())text(g,tr("outlets.none"),x,tableTop()+8,MUTED,w);
    }
    private String outletName(com.wormzjl.createcheme.runtime.fluid.FluidView.Outlet outlet){
        String face=tr("face."+outlet.face().name().toLowerCase(Locale.ROOT));
        return switch(outlet.port()){
            case VAPOR->tr("outlet.top",face);
            case BULK->tr("outlet.side",face);
            case LIQUID->outlet.head()>0?tr("outlet.bottom_head",face,number(outlet.head()/1000)):tr("outlet.bottom",face);
        };
    }
    private String outletState(com.wormzjl.createcheme.runtime.fluid.FluidView.Outlet outlet){
        double flow=mass?outlet.massFlow()*3600:outlet.moleFlow()*3.6;String unit=mass?" kg/h":" kmol/h";
        if(outlet.massFlow()<0)return tr("outlet.receiving")+" · "+number(-flow)+unit;
        if(!(outlet.massFlow()>0))return tr("outlet.idle");
        String drawing=outlet.port()==com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.PhasePort.BULK?tr("outlet.drawing_all")
            :outlet.drawn()==2?tr("outlet.drawing_gas")
            :outlet.port()==com.wormzjl.createcheme.science.fluid.network.PassiveNetwork.PhasePort.VAPOR?tr("outlet.drawing_overflow")
            :outlet.drawn()==1?tr("outlet.drawing_water"):outlet.drawn()==0?tr("outlet.drawing_oil"):"—";
        return drawing+" · "+number(flow)+unit;
    }
    /** A pipe's rail from base (the scrolled rail top); returns the bottom of its last status line. */
    private int renderPipeRail(GuiGraphics g,int mx,int my,int base){
        var view=menu.clientData().view();var info=view.pipeInfo();int y=base+46;
        if(info.junction()){text(g,tr("junction",info.connections().size()),12,base+34,ACCENT,rail());y=base+58;}
        double flow=displayedFlow();
        railLine(g,"throughput",(Double.isFinite(flow)?number(flow):"—")+(mass?" kg/h":" kmol/h"),y);
        if(railHover(mx,my,y,y+27))hovered=tr("throughput.hint");
        y+=30;
        railLine(g,"velocity",metricRange(info.connections(),com.wormzjl.createcheme.runtime.fluid.FluidView.PipeConnection::velocityMetresPerSecond," m/s"),y);
        if(railHover(mx,my,y,y+27))hovered=tr("velocity.hint");
        y+=30;
        railLine(g,"gradient",metricRange(info.connections(),com.wormzjl.createcheme.runtime.fluid.FluidView.PipeConnection::pressureDropPascalPerMetre," Pa/m"),y);
        if(railHover(mx,my,y,y+27))hovered=tr("gradient.hint");
        y+=32;text(g,tr("phase_ratio"),12,y,MUTED,rail());y+=14;
        double[] volumes=Arrays.copyOf(info.contents().phaseVolumes(),4);volumes[3]=info.contents().solids().volume();
        double total=Arrays.stream(volumes).sum();int at=12;int[] colors={0xffd6a65a,0xff549bd2,0xffb3c0ca,0xffa28565};
        for(int i=0;i<4;i++){int end=i==3?rail()+8:at+(total>0?(int)Math.round((rail()-4)*volumes[i]/total):0);if(total>0)g.fill(at,y,end,y+5,colors[i]);at=end;}
        y+=10;
        for(int i=0;i<4;i++){int col=i%2,row=i/2;text(g,tr("phase."+i)+" "+(total>0?number(100*volumes[i]/total)+"%":"—"),12+col*rail()/2,y+row*13,TEXT,rail()/2-4);}
        if(railHover(mx,my,y-24,y+26))hovered=tr("phase_ratio.hint");
        y+=34;text(g,tr("pipe_status"),12,y,ACCENT,rail());y+=14;
        String status=pipeStatus(view.status());
        if(info.changedDirection())status=tr("reversed_interval")+" "+status;
        // Every line of the status: the rail scrolls when they do not fit (they were cut at the separator before).
        var lines=font.split(Component.literal(status),rail()-4);
        for(int i=0;i<lines.size();i++)g.drawString(font,lines.get(i),12,y+i*10,view.status().contains("HELD")||view.status().contains("ERROR")?WARN:MUTED,false);
        if(railHover(mx,my,y-14,y+lines.size()*10))hovered=view.status();
        return y+Math.max(1,lines.size())*10-1;
    }
    private String pipeStatus(String status){
        if(status.contains("HELD")||status.contains("ERROR")||status.contains("WAITING")||status.contains("closed")||status.contains("UNBOUND"))return status;
        if(status.startsWith("STEADY: no flow"))return tr("no_flow");
        if(status.startsWith("STEADY:"))return tr(status.contains("VELOCITY_LIMIT")?"velocity_limited":"flowing");
        if(status.equals("FULL")||status.equals("READY"))return tr("ready");
        return status;
    }
    private String metricRange(List<com.wormzjl.createcheme.runtime.fluid.FluidView.PipeConnection> metrics,
            java.util.function.ToDoubleFunction<com.wormzjl.createcheme.runtime.fluid.FluidView.PipeConnection> value,String unit){
        if(metrics.isEmpty())return "—";
        double min=metrics.stream().mapToDouble(value).min().orElse(0),max=metrics.stream().mapToDouble(value).max().orElse(0);
        return (Math.abs(max-min)<1e-9?number(max):number(min)+"–"+number(max))+unit;
    }
    private void renderConnections(GuiGraphics g,int mx,int my){
        var view=menu.clientData().view();var metrics=view.pipeInfo().connections();int x=tableX(),w=tableWidth();
        text(g,tr("connections.hint"),x,64,MUTED,w);
        ProcessUi.tableRow(g,font,new String[]{tr("connection"),tr(mass?"kg_h":"kmol_h"),tr("speed"),tr("dp_m")},x,tableTop()-16,w,true);
        configureScroll(metrics.size());
        for(int i=scroll.value();i<Math.min(metrics.size(),scroll.value()+visibleRows());i++){
            var metric=metrics.get(i);String name=tr("connection");
            for(var route:view.pipeRoutes())if(route.pipeId()==metric.pipeId())name=(metric.reverse()?route.second():route.first())+" → "+(metric.reverse()?route.first():route.second());
            int y=tableTop()+(i-scroll.value())*18;
            ProcessUi.tableRow(g,font,new String[]{name,number(connectionFlow(view,metric)),number(metric.velocityMetresPerSecond()),number(metric.pressureDropPascalPerMetre())},x,y,w,false);
            if(mx>=x&&mx<x+w&&my>=y-2&&my<y+16)hovered=name;
        }
    }
    private double connectionFlow(com.wormzjl.createcheme.runtime.fluid.FluidView view,
            com.wormzjl.createcheme.runtime.fluid.FluidView.PipeConnection metric){
        if(mass)return metric.massRateKgPerSecond()*3600;
        if(view.intervalSeconds()<=0)return 0;
        for(var transfer:view.pipeHistory())if(transfer.pipeId()==metric.pipeId())
            return (Arrays.stream(transfer.forward().componentMoles()).sum()+Arrays.stream(transfer.reverse().componentMoles()).sum())*3.6/view.intervalSeconds();
        return 0;
    }
    private void renderContents(GuiGraphics g,int mx,int my){
        int x=tableX(),w=tableWidth();
        if(phase==3){
            ProcessUi.tableRow(g,font,new String[]{tr("solid_col.0"),tr("solid_col.1"),tr("mass_kg")},x,tableTop()-16,w,true);
            var populations=displayedSolids().populations();configureScroll(populations.size());
            for(int i=scroll.value();i<Math.min(populations.size(),scroll.value()+visibleRows());i++){
                var p=populations.get(i);ProcessUi.tableRow(g,font,new String[]{p.material().id(),number(p.size().diameterMetres()*1e6),number(p.massKg())},x,tableTop()+(i-scroll.value())*18,w,false);
            }
            if(populations.isEmpty())text(g,tr("absent"),x,tableTop()+8,MUTED,w);return;
        }
        var data=menu.clientData();boolean rates=transport();double[] values=amounts()[phase];
        double[] weights=data.molecularWeights().stream().mapToDouble(Double::doubleValue).toArray();
        double total=0;for(int i=0;i<values.length;i++)total+=values[i]*(mass?weights[i]:1);
        ProcessUi.tableRow(g,font,new String[]{tr("component"),tr(mass?"mass_percent":"mole_percent"),tr(rates?(mass?"kg_h":"kmol_h"):(mass?"kg":"kmol"))},x,tableTop()-16,w,true);
        var ids=present();configureScroll(ids.size());
        for(int i=scroll.value();i<Math.min(ids.size(),scroll.value()+visibleRows());i++){
            int id=ids.get(i),y=tableTop()+(i-scroll.value())*18;double amount=values[id]*(mass?weights[id]:1);
            double quantity=mass?amount:amount/1000;
            if(rates)quantity=data.view().intervalSeconds()>0?quantity*3600/data.view().intervalSeconds():Double.NaN;
            ProcessUi.tableRow(g,font,new String[]{material(id),number(100*amount/total),Double.isFinite(quantity)?number(quantity):"—"},x,y,w,false);
            if(mx>=x&&mx<x+w/2&&my>=y-2&&my<y+16)hovered=material(id)+"\n"+data.components().get(id);
        }
        if(ids.isEmpty())text(g,tr("absent"),x,tableTop()+8,MUTED,w);
    }
    private void renderComposition(GuiGraphics g,int mx,int my){
        int x=tableX(),w=tableWidth();
        if(templates){text(g,tr("mixture_library"),x,116,ACCENT,w);return;}
        text(g,tr("component"),x,150,TEXT,w/2-6);text(g,tr("relative"),x+w/2,150,TEXT,70);
        text(g,tr(mass?"mass_percent":"mole_percent"),x+w/2+76,150,TEXT,w/2-108);
        double[] fractions=null;try{fractions=composition.displayFractions();}catch(IllegalArgumentException ignored){}
        var ids=composition.rows();
        for(int i=scroll.value();i<Math.min(ids.size(),scroll.value()+visibleRows());i++){
            int id=ids.get(i),y=tableTop()+(i-scroll.value())*25;
            text(g,material(id),x+3,y+5,MUTED,w/2-8);text(g,fractions==null?"—":number(fractions[id]*100),x+w/2+76,y+5,TEXT,w/2-108);
            g.hLine(x,x+w,y+22,LINE);
            if(mx>=x&&mx<x+w/2&&my>=y&&my<y+23)hovered=material(id)+"\n"+menu.clientData().components().get(id);
        }
        if(ids.isEmpty())text(g,tr("empty_mixture"),x,tableTop()+8,MUTED,w);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(ProcessUi.textKeyPressed(this,key,scan,modifiers))return true;
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(componentDropdown!=null&&componentDropdown.popupClick(x,y,button))return true;
        if(scroll.click(x,y,button)||railScroll.click(x,y,button)){rebuildNext=true;return true;}return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(componentDropdown!=null&&componentDropdown.popupDrag(y))return true;
        if(scroll.drag(y)||railScroll.drag(y)){rebuildNext=true;return true;}return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(componentDropdown!=null)componentDropdown.popupRelease();scroll.release();railScroll.release();return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(componentDropdown!=null&&componentDropdown.popupScroll(x,y,vertical))return true;
        if(x>=leftPos+tableX()&&scroll.wheel(vertical)){rebuildNext=true;return true;}
        // The wheel over the rail column scrolls it (three steps of 10 px: one value line).
        if(x<leftPos+tableX()&&y>=topPos+railTop()&&y<topPos+railBottom()&&railScroll.wheel(vertical)){rebuildNext=true;return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
}
