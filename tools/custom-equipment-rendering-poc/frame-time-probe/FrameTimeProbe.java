package com.wormzjl.createcheme.mcpcompat;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Properties;

/**
 * Measurement only, for batch 2026-10-15-custom-equipment-rendering; detach to tools/ before the batch merges.
 *
 * <p>{@code /fpsprobe <warm-up s> <measurement s> <label>} (client command) waits the warm-up, records every frame
 * interval of the measurement window and logs one {@code FPS_PROBE} line: frames, mean FPS and frame-time percentiles.
 *
 * <p>Autopilot, for lanes where the bridge cannot take control (with Sodium its {@code enter_control_mode} crashes in
 * {@code glfwSetInputMode}): when {@code <game dir>/fps-autopilot/autopilot.properties} exists ({@code save}, {@code label},
 * {@code warmup}, {@code measure}), the client opens that save from the title screen, sends {@code camera.txt} and
 * {@code columns.txt}, measures, takes a screenshot, sends {@code empty.txt}, measures again and quits.
 */
@EventBusSubscriber(modid = "createcheme_mcp_compat", value = Dist.CLIENT)
public final class FrameTimeProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long[] SAMPLES = new long[1 << 21];
    private static boolean active;
    private static long warmupEnds, measureEnds, last;
    private static int count;
    private static String label = "";
    // Autopilot state (render thread only).
    private static Properties pilot;
    private static int stage;
    private static long stageStarted;
    private static boolean shot;
    private static final ArrayDeque<String> COMMANDS = new ArrayDeque<>();

    private FrameTimeProbe() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fpsprobe")
                .then(Commands.argument("warmup", IntegerArgumentType.integer(0, 600))
                        .then(Commands.argument("measure", IntegerArgumentType.integer(1, 600))
                                .then(Commands.argument("label", StringArgumentType.word()).executes(context -> {
                                    start(StringArgumentType.getString(context, "label"), IntegerArgumentType.getInteger(context, "warmup"),
                                            IntegerArgumentType.getInteger(context, "measure"));
                                    context.getSource().sendSystemMessage(Component.literal("fpsprobe started: " + label));
                                    return 1;
                                })))));
    }

    private static void start(String name, int warmupSeconds, int measureSeconds) {
        long now = System.nanoTime();
        warmupEnds = now + warmupSeconds * 1_000_000_000L;
        measureEnds = warmupEnds + measureSeconds * 1_000_000_000L;
        label = name;
        count = 0;
        last = 0;
        active = true;
        LOGGER.info("FPS_PROBE start label={}", label);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!active) return;
        long now = System.nanoTime();
        if (now >= warmupEnds && last >= warmupEnds && count < SAMPLES.length) SAMPLES[count++] = now - last;
        last = now;
        if (now < measureEnds) return;
        active = false;
        var sorted = Arrays.copyOf(SAMPLES, count);
        Arrays.sort(sorted);
        long total = 0;
        for (long sample : sorted) total += sample;
        double seconds = total / 1e9;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        LOGGER.info("FPS_PROBE label={} frames={} seconds={} meanFps={} p50ms={} p95ms={} p99ms={} maxMs={} camera={}",
                label, count, String.format("%.2f", seconds), String.format("%.1f", count / seconds),
                ms(sorted, .50), ms(sorted, .95), ms(sorted, .99), String.format("%.2f", count == 0 ? 0 : sorted[count - 1] / 1e6), camera);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (stage == 0) {
            var file = mc.gameDirectory.toPath().resolve("fps-autopilot/autopilot.properties");
            if (!(mc.screen instanceof TitleScreen) || !Files.isRegularFile(file)) return;
            pilot = new Properties();
            try (Reader reader = Files.newBufferedReader(file)) {
                pilot.load(reader);
            } catch (IOException e) {
                LOGGER.error("FPS_AUTOPILOT cannot read {}", file, e);
                stage = -1;
                return;
            }
            LOGGER.info("FPS_AUTOPILOT opening save {}", pilot.getProperty("save"));
            stage = 1;
            mc.createWorldOpenFlows().openWorld(pilot.getProperty("save"), () -> LOGGER.error("FPS_AUTOPILOT could not open the save"));
            return;
        }
        if (stage < 0 || stage > 6) return;
        long now = System.nanoTime();
        if (!COMMANDS.isEmpty()) {
            if (mc.player != null) mc.player.connection.sendCommand(COMMANDS.poll());
            return;
        }
        int warmup = Integer.parseInt(pilot.getProperty("warmup", "60")), measure = Integer.parseInt(pilot.getProperty("measure", "60"));
        String name = pilot.getProperty("label", "lane");
        switch (stage) {
            case 1 -> { // in the world: wait 10 s for the first chunks, then camera and columns
                if (mc.player == null || mc.level == null || mc.screen != null) return;
                if (stageStarted == 0) stageStarted = now;
                if (now - stageStarted < 10_000_000_000L) return;
                queue(mc, "camera.txt");
                queue(mc, "columns.txt");
                stage = 2;
            }
            case 2 -> {
                start(name + "-columns", warmup, measure);
                shot = false;
                stage = 3;
            }
            case 3 -> {
                if (!shot && now > warmupEnds - 5_000_000_000L) {
                    Screenshot.grab(mc.gameDirectory, "fps-" + name + "-columns.png", mc.getMainRenderTarget(), message -> {});
                    shot = true;
                }
                if (active) return;
                queue(mc, "empty.txt");
                stage = 4;
            }
            case 4 -> {
                start(name + "-empty", warmup, measure);
                stage = 5;
            }
            case 5 -> {
                if (active) return;
                LOGGER.info("FPS_AUTOPILOT done, saving and quitting");
                stage = 6;
                if (mc.level != null) mc.level.disconnect();
                mc.disconnect(new GenericMessageScreen(Component.literal("Saving")));
            }
            case 6 -> {
                if (mc.level != null) return;
                stage = 7;
                mc.stop();
            }
            default -> {
            }
        }
    }

    private static void queue(Minecraft mc, String file) {
        try {
            for (var line : Files.readAllLines(mc.gameDirectory.toPath().resolve("fps-autopilot").resolve(file))) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) COMMANDS.add(line.startsWith("/") ? line.substring(1) : line);
            }
        } catch (IOException e) {
            LOGGER.error("FPS_AUTOPILOT cannot read {}", file, e);
            stage = -1;
        }
    }

    private static String ms(long[] sorted, double quantile) {
        if (sorted.length == 0) return "0";
        return String.format("%.2f", sorted[Math.min(sorted.length - 1, (int) (quantile * sorted.length))] / 1e6);
    }
}
