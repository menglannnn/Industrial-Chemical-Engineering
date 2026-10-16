package com.wormzjl.createcheme.client;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.client.gui.screens.EquipmentConstructorScreen;
import com.wormzjl.createcheme.client.gui.screens.VesselScreen;
import com.wormzjl.createcheme.client.model.EquipmentShellModel;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentConstructorBlockEntity;
import net.minecraft.core.BlockPos;
import com.wormzjl.createcheme.registry.ModBlocks;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import com.wormzjl.createcheme.world.level.block.EquipmentStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/**
 * Client side of parametric vessels: the shell model loader, one outline for the whole vessel instead of per-cell boxes
 * (its silhouette as seen from the camera), and the screens the blocks open (blocks call {@link #openConstructor} and
 * {@link #openVessel} on the client only).
 */
@EventBusSubscriber(modid = CreateChemE.MOD_ID, value = Dist.CLIENT)
public final class EquipmentClient {
    private EquipmentClient() {
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(EquipmentShellModel.LOADER_ID, EquipmentShellModel.LOADER);
        event.register(com.wormzjl.createcheme.client.model.FluidDeviceModel.LOADER_ID, com.wormzjl.createcheme.client.model.FluidDeviceModel.LOADER);
    }

    /** Called by the constructor block on the client only: open its design screen. */
    public static void openConstructor(BlockPos position) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(position) instanceof EquipmentConstructorBlockEntity constructor)
            mc.setScreen(new EquipmentConstructorScreen(position, constructor.facing(), constructor.design()));
    }

    /** Called by shell and interior cells on the client only: open the screen of the vessel the cell belongs to. */
    public static void openVessel(BlockPos position) {
        var mc = Minecraft.getInstance();
        var vessel = mc.level == null ? null : EquipmentStructure.find(mc.level, position);
        if (vessel != null) mc.setScreen(new VesselScreen(vessel));
    }

    @SubscribeEvent
    public static void highlight(RenderHighlightEvent.Block event) {
        var level = Minecraft.getInstance().level;
        var position = event.getTarget().getBlockPos();
        if (level == null) return;
        var state = level.getBlockState(position);
        if (!state.is(ModBlocks.EQUIPMENT_SHELL.get()) && !state.is(ModBlocks.EQUIPMENT_INTERIOR.get())) return;
        var vessel = EquipmentStructure.find(level, position);
        if (vessel == null) return;
        var camera = event.getCamera().getPosition();
        double ox = vessel.origin().getX() - camera.x, oy = vessel.origin().getY() - camera.y, oz = vessel.origin().getZ() - camera.z;
        var pose = event.getPoseStack().last();
        var lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        for (var s : ShapeRasterizer.outline(vessel.shape(), -ox, -oy, -oz)) {
            float x0 = (float) (ox + s[0]), y0 = (float) (oy + s[1]), z0 = (float) (oz + s[2]);
            float x1 = (float) (ox + s[3]), y1 = (float) (oy + s[4]), z1 = (float) (oz + s[5]);
            float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0, length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length == 0) continue;
            lines.addVertex(pose, x0, y0, z0).setColor(0, 0, 0, .4F).setNormal(pose, dx / length, dy / length, dz / length);
            lines.addVertex(pose, x1, y1, z1).setColor(0, 0, 0, .4F).setNormal(pose, dx / length, dy / length, dz / length);
        }
        event.setCanceled(true);
    }
}
