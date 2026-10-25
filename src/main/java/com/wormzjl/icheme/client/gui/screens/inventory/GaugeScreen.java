package com.wormzjl.icheme.client.gui.screens.inventory;

import com.wormzjl.icheme.client.gui.common.ProcessUi;
import com.wormzjl.icheme.network.InstrumentNetwork;
import com.wormzjl.icheme.world.inventory.GaugeMenu;
import com.wormzjl.icheme.world.item.GaugeSettings;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public final class GaugeScreen extends AbstractContainerScreen<GaugeMenu> {
    private GaugeSettings display;
    private GaugeSettings parsed;
    private EditBox minimum,maximum;
    private ProcessUi.NumericDraft minimumDraft,maximumDraft;
    private Button save,units,reference;
    public GaugeScreen(GaugeMenu menu,Inventory inventory,Component title){super(menu,inventory,title);display=menu.initial();imageWidth=246;imageHeight=174;inventoryLabelY=1000;}
    private static Component label(String key){return Component.translatable("gui.icheme.gauge."+key);}
    @Override protected void init(){super.init();minimum=addRenderableWidget(new EditBox(font,leftPos+12,topPos+48,104,20,label("minimum")));maximum=addRenderableWidget(new EditBox(font,leftPos+130,topPos+48,104,20,label("maximum")));minimum.setMaxLength(40);maximum.setMaxLength(40);units=addRenderableWidget(Button.builder(Component.literal(display.unit().symbol),b->switchDisplay(false)).bounds(leftPos+12,topPos+80,104,20).build());reference=addRenderableWidget(Button.builder(referenceLabel(),b->switchDisplay(true)).bounds(leftPos+130,topPos+80,104,20).build());reference.visible=display.kind()==GaugeSettings.Kind.PRESSURE;save=addRenderableWidget(Button.builder(Component.translatable("gui.icheme.pipeline_tool.save"),b->save()).bounds(leftPos+12,topPos+142,104,20).build());addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL,b->onClose()).bounds(leftPos+130,topPos+142,104,20).build());draft(display);minimum.setResponder(t->validate());maximum.setResponder(t->validate());validate();setInitialFocus(minimum);}
    private Component referenceLabel(){return label(display.relative()?"relative":"absolute");}
    private void draft(GaugeSettings s){var unit=new com.wormzjl.icheme.science.display.DisplayUnit(){public double display(double si){return s.display(si);}public double si(double value){return s.si(value);}};minimumDraft=new ProcessUi.NumericDraft(s.minimum(),unit);maximumDraft=new ProcessUi.NumericDraft(s.maximum(),unit);minimum.setValue(minimumDraft.text());maximum.setValue(maximumDraft.text());}
    private void validate(){parsed=null;try{minimumDraft.text(minimum.getValue());maximumDraft.text(maximum.getValue());parsed=new GaugeSettings(display.kind(),minimumDraft.si(),maximumDraft.si(),display.unit(),display.relative());}catch(IllegalArgumentException ignored){}save.active=parsed!=null;}
    private void switchDisplay(boolean basis){validate();if(parsed==null)return;display=parsed.displayAs(basis?display.unit():display.unit().next(display.kind()),basis?!display.relative():display.relative());draft(display);units.setMessage(Component.literal(display.unit().symbol));reference.setMessage(referenceLabel());validate();}
    private void save(){validate();if(parsed!=null){InstrumentNetwork.save(menu.containerId,parsed);onClose();}}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.fill(leftPos-1,topPos-1,leftPos+imageWidth+1,topPos+imageHeight+1,ProcessUi.LINE);g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,ProcessUi.BG);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,title,12,12,ProcessUi.TEXT,false);g.drawString(font,label("minimum"),12,34,ProcessUi.MUTED,false);g.drawString(font,label("maximum"),130,34,ProcessUi.MUTED,false);if(parsed==null)g.drawString(font,label("invalid"),12,112,ProcessUi.WARN,false);else g.drawString(font,label("dial_only"),12,112,ProcessUi.MUTED,false);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){save();return true;}if(ProcessUi.textKeyPressed(this,key,scan,modifiers))return true;return super.keyPressed(key,scan,modifiers);}
}
