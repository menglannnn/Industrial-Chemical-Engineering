package com.wormzjl.icheme.registry;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.world.inventory.ColumnCalculatorV3Menu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, IChemE.MOD_ID);
    public static final DeferredHolder<MenuType<?>,MenuType<com.wormzjl.icheme.world.inventory.FluidDeviceMenu>> FLUID_DEVICE=MENU_TYPES.register("fluid_device",()->IMenuTypeExtension.create(com.wormzjl.icheme.world.inventory.FluidDeviceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.wormzjl.icheme.world.inventory.PipelineToolMenu>> PIPELINE_TOOL =
            MENU_TYPES.register("pipeline_tool", () -> IMenuTypeExtension.create(com.wormzjl.icheme.world.inventory.PipelineToolMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ColumnCalculatorV3Menu>> COLUMN_CALCULATOR_V3 =
            MENU_TYPES.register(
                    "column_calculator_v3",
                    () -> IMenuTypeExtension.create(ColumnCalculatorV3Menu::new)
            );

    public static final DeferredHolder<MenuType<?>, MenuType<com.wormzjl.icheme.world.inventory.GaugeMenu>> GAUGE = MENU_TYPES.register("gauge", () -> IMenuTypeExtension.create(com.wormzjl.icheme.world.inventory.GaugeMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.wormzjl.icheme.world.inventory.AssemblyMenu>> ASSEMBLY = MENU_TYPES.register("assembly", () -> IMenuTypeExtension.create(com.wormzjl.icheme.world.inventory.AssemblyMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
