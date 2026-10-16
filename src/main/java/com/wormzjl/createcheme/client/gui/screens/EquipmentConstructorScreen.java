package com.wormzjl.createcheme.client.gui.screens;

import com.wormzjl.createcheme.client.EquipmentGhost;
import com.wormzjl.createcheme.client.gui.common.ColumnDiagram;
import com.wormzjl.createcheme.client.gui.common.ProcessUi;
import com.wormzjl.createcheme.network.EquipmentNetwork;
import com.wormzjl.createcheme.science.equipment.ColumnTrays;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.Paint;
import com.wormzjl.createcheme.science.equipment.ProcessUnit;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import com.wormzjl.createcheme.science.equipment.VesselPlacement;
import com.wormzjl.createcheme.world.level.block.EquipmentStructure;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentConstructorBlockEntity;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentConstructorBlockEntity.Design;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

/**
 * The equipment constructor's screen: first what to build (a vertical drum, a horizontal drum, a distillation column, or
 * a unit: a heat exchanger, a kettle reboiler, an air cooler bay or a steam ejector; owner requests 2026-10-16), then its
 * design, in a panel on the left beside the world and the vessel's ghost; a drum's side connections and every kind's
 * paint have pages of their own (owner requests 2026-10-16). It edits a local draft; the server receives the design when
 * the screen closes, or with a build request on Build. A column is designed here by its trays and feed, or imported whole
 * (draws, pumparounds and steam too) from the nearest column calculator; its schematic is the calculator's
 * ({@link ColumnDiagram}).
 */
public final class EquipmentConstructorScreen extends Screen {
    /** The screen's pages: what to build, its design, a drum's side connections and the paint. */
    private enum Page {
        CHOOSE, DESIGN, CONNECTIONS, PAINT
    }

    /** The paint page's palette: two rows of eight swatches across the panel. */
    private static final int SWATCH = 19, SWATCH_GAP = 4, SWATCHES_PER_ROW = 8;
    private static final String PAINT_KEY = "gui.createcheme.paint.";

    /** Panel layout in GUI pixels, top-down, so it fits a 240-pixel-high GUI (854×480 at scale 2). */
    /** The schematic strip's width: the steam lines (90 pixels left of the axis) to the draw marks (39 right of it), and margins. */
    private static final int STRIP = 140;
    private static final int PANEL = 196, TOP = 24, ROW = 17, ROWS = 8, LINE = 11, LINES = TOP + ROWS * ROW + 4, STATUS = LINES + 3 * LINE + 2, BUTTONS = STATUS + 14;
    /** The choice page: one button per kind of equipment, its description in its hover text. */
    private static final int CHOICE = 38, CHOICE_PITCH = 24;
    private static final String KEY = "gui.createcheme.equipment_constructor.";

    /** What the constructor builds; a horizontal drum lies along or across the constructor's facing. */
    private enum Equipment {
        VERTICAL_DRUM, HORIZONTAL_DRUM, COLUMN, EXCHANGER, KETTLE, AIR_COOLER, EJECTOR;

        static Equipment of(VesselPlacement.Form form) {
            return switch (form) {
                case VERTICAL -> VERTICAL_DRUM;
                case LENGTHWISE, CROSSWISE -> HORIZONTAL_DRUM;
                case COLUMN -> COLUMN;
                case EXCHANGER -> EXCHANGER;
                case KETTLE -> KETTLE;
                case AIR_COOLER -> AIR_COOLER;
                case EJECTOR -> EJECTOR;
            };
        }

        String key() {
            return KEY + "equipment." + name().toLowerCase(Locale.ROOT);
        }
    }

    private final BlockPos position;
    private final Direction facing;
    private Design saved;
    /** The screen opens on the choice page. */
    private Page page = Page.CHOOSE;
    private VesselPlacement.Form form;
    /** The diameter (a unit's size), always on its kind's grid: 0.5 m steps for a vessel ({@link ProcessUnit#snap} for a unit). */
    private double diameter;
    private int tangentLength, support, forward, right, up;
    private ColumnTrays trays;
    /** A drum's side connections, kept within the drum's ranges ({@link #fitConnections}), and the one the page edits. */
    private final List<EquipmentShape.Connection> connections = new ArrayList<>();
    private int selected;
    /** The paint, and the channel the paint page edits ({@link Paint#BODY} or {@link Paint#STRUCTURE}). */
    private Paint paint;
    /** An air cooler's bays in parallel and in series, kept for the other kinds. */
    private ProcessUnit.Bank bank;
    private int channel = Paint.BODY;
    private @Nullable EditBox custom;
    /** The paint page's body and structure buttons (null off that page). */
    private final Button[] channelButtons = new Button[2];
    private @Nullable Button build;
    private ShapeRasterizer.Layout layout;
    /** Set by Import until the constructor's synced design changes; the draft then takes it. */
    private @Nullable Design importing;
    private boolean sent;

    public EquipmentConstructorScreen(BlockPos position, Direction facing, Design design) {
        super(Component.translatable("block.createcheme.equipment_constructor"));
        this.position = position.immutable();
        this.facing = facing;
        load(design);
    }

    private void load(Design design) {
        saved = design;
        form = design.form();
        diameter = design.diameter();
        tangentLength = design.tangentLength();
        support = Math.max(design.support(), minimumSupport());
        forward = design.placement().forward();
        right = design.placement().right();
        up = design.placement().up();
        trays = design.trays();
        connections.clear();
        connections.addAll(design.connections());
        selected = 0;
        paint = design.paint();
        bank = design.bank();
        fitConnections();
        layout = ShapeRasterizer.layout(design().shape(facing));
    }

    /** The constructor this screen designs for. */
    public BlockPos position() {
        return position;
    }

    /** The draft; always valid, because every stepper is clamped to the bounds of its parameter. */
    public Design design() {
        return new Design(diameter, tangentLength, form, support, new VesselPlacement(forward, right, up), trays, List.copyOf(connections), paint, bank);
    }

    /** The vessel the draft would build, for the ghost; null on the choice page and once the constructor is gone. */
    public @Nullable EquipmentStructure.Vessel vessel() {
        var constructor = constructor();
        return constructor != null && page != Page.CHOOSE ? constructor.vessel(design()) : null;
    }

    /**
     * Keeps a drum's side connections within its ranges after its size or form changed: each to the nearest station and
     * offset it may take, or dropped where the shell leaves it none. A column or a unit keeps them for when it is a drum again.
     */
    private void fitConnections() {
        if (form.kind() != EquipmentShape.Kind.DRUM) return;
        int[] stations = EquipmentShape.stations(diameter, tangentLength, form.orientation(), support);
        int across = EquipmentShape.maximumOffset(diameter);
        if (stations[0] > stations[1]) connections.clear();
        connections.replaceAll(c -> new EquipmentShape.Connection(c.side(), clamp(c.station(), stations[0], stations[1]), clamp(c.offset(), -across, across)));
        selected = clamp(selected, 0, Math.max(0, connections.size() - 1));
    }

    private @Nullable EquipmentConstructorBlockEntity constructor() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(position) instanceof EquipmentConstructorBlockEntity constructor ? constructor : null;
    }

    @Override
    protected void init() {
        build = null;
        custom = null;
        channelButtons[0] = channelButtons[1] = null;
        switch (page) {
            case CHOOSE -> {
                var current = Equipment.of(form);
                for (var equipment : Equipment.values()) {
                    Component name = Component.translatable(equipment.key());
                    if (equipment == current) name = Component.translatable(KEY + "current", name);
                    addRenderableWidget(Button.builder(name, b -> choose(equipment)).bounds(8, CHOICE + equipment.ordinal() * CHOICE_PITCH, PANEL - 16, 20)
                            .tooltip(Tooltip.create(Component.translatable(equipment.key() + ".hint"))).build());
                }
                addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose()).bounds(100, BUTTONS, 88, 20).build());
            }
            case DESIGN -> designPage();
            case CONNECTIONS -> connectionsPage();
            case PAINT -> paintPage();
        }
    }

    private void show(Page next) {
        page = next;
        rebuildWidgets();
    }

    private void designPage() {
        var change = Button.builder(Component.translatable(KEY + "change"), b -> show(Page.CHOOSE)).bounds(PANEL - 80, TOP - 1, 74, 16).build();
        change.setTooltip(Tooltip.create(Component.translatable(KEY + "change.hint")));
        addRenderableWidget(change);
        if (form.unit()) {
            unitDesign();
            return;
        }
        stepper(1, this::diameter);
        int next = 3;
        if (form.column()) {
            stepper(2, step -> trays = trays.withTrays(clamp(trays.trays() + step, 1, ColumnTrays.MAX_TRAYS)));
            stepper(3, step -> trays = trays.withFeed(clamp(trays.feed() + step, 1, trays.trays() + 1)));
            next = 4;
        } else stepper(2, step -> tangentLength = clamp(tangentLength + step, 1, (int) EquipmentShape.MAX_TANGENT_LENGTH));
        if (Equipment.of(form) == Equipment.HORIZONTAL_DRUM) {
            var axis = Button.builder(Component.translatable(KEY + "form." + form.name().toLowerCase(Locale.ROOT)), b -> {
                form = form == VesselPlacement.Form.LENGTHWISE ? VesselPlacement.Form.CROSSWISE : VesselPlacement.Form.LENGTHWISE;
                refresh();
                rebuildWidgets();
            }).bounds(PANEL - 80, TOP + 3 * ROW, 74, 16).build();
            axis.setTooltip(Tooltip.create(Component.translatable(KEY + "axis.hint")));
            addRenderableWidget(axis);
            next = 4;
        }
        stepper(next, step -> support = clamp(support + step, minimumSupport(), EquipmentShape.MAX_SUPPORT));
        stepper(next + 1, step -> forward = clamp(forward + step, 1, VesselPlacement.MAX_FORWARD));
        stepper(next + 2, step -> right = clamp(right + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        stepper(next + 3, step -> up = clamp(up + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        // A column imports its design; a drum's side connections have their own page, as the paint does.
        if (form.column())
            bottomRow(Button.builder(Component.translatable(KEY + "import"), b -> importColumn()).tooltip(Tooltip.create(Component.translatable(KEY + "import.hint", EquipmentNetwork.IMPORT_REACH))),
                    paintButton());
        else bottomRow(Button.builder(Component.translatable(KEY + "nozzles"), b -> show(Page.CONNECTIONS)).tooltip(Tooltip.create(Component.translatable(KEY + "connections.hint"))),
                paintButton());
    }

    private Button.Builder paintButton() {
        return Button.builder(Component.translatable(KEY + "paint"), b -> show(Page.PAINT)).tooltip(Tooltip.create(Component.translatable(KEY + "paint.hint")));
    }

    /**
     * The paint page (owner request 2026-10-16): the body's and the structure's colours, each a button that picks it for
     * editing; the palette; a custom colour as #RRGGBB; the defaults.
     */
    private void paintPage() {
        for (int c : new int[]{Paint.BODY, Paint.STRUCTURE}) {
            int k = c;
            channelButtons[c] = addRenderableWidget(Button.builder(colourName(paint.colour(c)), b -> {
                channel = k;
                rebuildWidgets();
            }).bounds(PANEL - 80, TOP + (1 + c) * ROW, 74, 16).tooltip(Tooltip.create(hint(1 + c))).build());
        }
        for (int i = 0; i < Paint.PALETTE.size(); i++) {
            int x = 8 + i % SWATCHES_PER_ROW * (SWATCH + SWATCH_GAP), y = TOP + (3 + i / SWATCHES_PER_ROW) * ROW;
            addRenderableWidget(new SwatchButton(x, y, Paint.PALETTE.get(i)));
        }
        custom = new EditBox(font, PANEL - 80, TOP + 5 * ROW, 74, 16, Component.translatable(KEY + "custom"));
        custom.setMaxLength(7);
        custom.setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
        custom.setValue(String.format(Locale.ROOT, "#%06X", paint.colour(channel)));
        custom.setResponder(text -> {
            var digits = text.startsWith("#") ? text.substring(1) : text;
            if (digits.length() == 6) colour(Integer.parseInt(digits, 16));
        });
        custom.setTooltip(Tooltip.create(hint(5)));
        addRenderableWidget(custom);
        addRenderableWidget(Button.builder(Component.translatable(KEY + "paint.default"), b -> {
            paint = Paint.DEFAULT;
            refresh();
            rebuildWidgets();
        }).bounds(PANEL - 80, TOP + 6 * ROW, 74, 16).tooltip(Tooltip.create(hint(6))).build());
        bottomRow(Button.builder(Component.translatable(KEY + "back"), b -> show(Page.DESIGN)).tooltip(Tooltip.create(Component.translatable(KEY + "back.hint"))));
    }

    /** Paints the edited channel {@code rgb}: its button names the colour, and the custom field shows it unless typed in. */
    private void colour(int rgb) {
        if (paint.colour(channel) == rgb) return;
        paint = paint.with(channel, rgb);
        refresh();
        if (channelButtons[channel] != null) channelButtons[channel].setMessage(colourName(rgb));
        if (custom != null && !custom.isFocused()) custom.setValue(String.format(Locale.ROOT, "#%06X", rgb));
    }

    /** A colour as the page names it: its palette number, or #RRGGBB for a custom one. */
    private static Component colourName(int rgb) {
        var swatch = Paint.swatch(rgb);
        return swatch != null ? Component.literal("RAL " + swatch.ral()) : Component.literal(String.format(Locale.ROOT, "#%06X", rgb));
    }

    /** A palette colour: a filled square, outlined in white when it is the edited channel's colour. */
    private final class SwatchButton extends AbstractButton {
        private final Paint.Swatch swatch;

        SwatchButton(int x, int y, Paint.Swatch swatch) {
            super(x, y, SWATCH, 16, Component.translatable(PAINT_KEY + swatch.key()));
            this.swatch = swatch;
            setTooltip(Tooltip.create(Component.translatable(KEY + "paint.swatch", swatch.ral(), Component.translatable(PAINT_KEY + swatch.key()))));
        }

        @Override
        public void onPress() {
            colour(swatch.rgb());
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean chosen = paint.colour(channel) == swatch.rgb();
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
            g.fill(x0, y0, x1, y1, chosen ? 0xFFFFFFFF : isHoveredOrFocused() ? ProcessUi.TEXT : ProcessUi.LINE);
            if (chosen) g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF000000);
            int inset = chosen ? 2 : 1;
            g.fill(x0 + inset, y0 + inset, x1 - inset, y1 - inset, 0xFF000000 | swatch.rgb());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /**
     * A unit's design: its size (an exchanger's shell, a kettle's bundle, an ejector's suction; an air cooler's bay, its
     * width and length together, and its bays in parallel and in series), its tube length, its support's height and its
     * placement.
     */
    private void unitDesign() {
        var kind = form.kind();
        int row = 1;
        if (kind == EquipmentShape.Kind.AIR_COOLER) {
            stepper(row++, this::bay);
            stepper(row++, step -> bank = new ProcessUnit.Bank(clamp(bank.parallel() + step, 1, ProcessUnit.Bank.MAX_PARALLEL), bank.series()));
            stepper(row, step -> bank = new ProcessUnit.Bank(bank.parallel(), clamp(bank.series() + step, 1, ProcessUnit.Bank.MAX_SERIES)));
        } else stepper(row, this::diameter);
        row++;
        if (kind == EquipmentShape.Kind.EXCHANGER || kind == EquipmentShape.Kind.KETTLE)
            stepper(row++, step -> tangentLength = clamp(tangentLength + step, ProcessUnit.minLength(kind, diameter), ProcessUnit.maxLength(kind)));
        stepper(row, step -> support = clamp(support + step, minimumSupport(), EquipmentShape.MAX_SUPPORT));
        stepper(row + 1, step -> forward = clamp(forward + step, 1, VesselPlacement.MAX_FORWARD));
        stepper(row + 2, step -> right = clamp(right + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        stepper(row + 3, step -> up = clamp(up + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        bottomRow(paintButton());
    }

    /**
     * The connections page: which connection, the side it faces, its height (a lying drum's place along its axis) and its
     * offset across the shell; Add places a new one where it meets nothing, Remove takes the selected one away.
     */
    private void connectionsPage() {
        stepper(1, step -> {
            selected = clamp(selected + step, 0, Math.max(0, connections.size() - 1));
            rebuildWidgets();
        });
        if (!connections.isEmpty()) {
            var side = Button.builder(Component.translatable(KEY + "side." + toward(connection().axis(), connection().sign())), b -> {
                var c = connection();
                connections.set(selected, new EquipmentShape.Connection((c.side() + 1) % EquipmentShape.CONNECTION_SIDES, c.station(), c.offset()));
                refresh();
                rebuildWidgets();
            }).bounds(PANEL - 80, TOP + 2 * ROW, 74, 16).tooltip(Tooltip.create(hint(2))).build();
            addRenderableWidget(side);
            stepper(3, step -> {
                var c = connection();
                int[] stations = stations();
                connections.set(selected, new EquipmentShape.Connection(c.side(), clamp(c.station() + step, stations[0], stations[1]), c.offset()));
            });
            stepper(4, step -> {
                var c = connection();
                int across = EquipmentShape.maximumOffset(diameter);
                connections.set(selected, new EquipmentShape.Connection(c.side(), c.station(), clamp(c.offset() + step, -across, across)));
            });
        }
        int half = (PANEL - 16 - 4) / 2, y = TOP + 5 * ROW;
        var add = Button.builder(Component.translatable(KEY + "add"), b -> add()).bounds(8, y, half, 16)
                .tooltip(Tooltip.create(Component.translatable(KEY + "add.hint", EquipmentShape.MAX_CONNECTIONS))).build();
        int[] stations = stations();
        add.active = connections.size() < EquipmentShape.MAX_CONNECTIONS && stations[0] <= stations[1];
        addRenderableWidget(add);
        var remove = Button.builder(Component.translatable(KEY + "remove"), b -> {
            connections.remove(selected);
            selected = clamp(selected, 0, Math.max(0, connections.size() - 1));
            refresh();
            rebuildWidgets();
        }).bounds(8 + half + 4, y, half, 16).tooltip(Tooltip.create(Component.translatable(KEY + "remove.hint"))).build();
        remove.active = !connections.isEmpty();
        addRenderableWidget(remove);
        bottomRow(Button.builder(Component.translatable(KEY + "back"), b -> show(Page.DESIGN)).tooltip(Tooltip.create(Component.translatable(KEY + "back.hint"))));
    }

    /** The bottom row: the page's own buttons beside Build and Done. Build waits while a side connection clashes. */
    private void bottomRow(Button.Builder... own) {
        int gap = 4, count = own.length + 2, span = (PANEL - 16 - (count - 1) * gap) / count;
        for (int i = 0; i < own.length; i++) addRenderableWidget(own[i].bounds(8 + i * (span + gap), BUTTONS, span, 20).build());
        build = addRenderableWidget(Button.builder(Component.translatable(KEY + "build"), b -> build()).bounds(8 + own.length * (span + gap), BUTTONS, span, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose()).bounds(8 + (own.length + 1) * (span + gap), BUTTONS, span, 20).build());
        build.active = clash() == null;
    }

    private EquipmentShape.Connection connection() {
        return connections.get(selected);
    }

    private int[] stations() {
        return EquipmentShape.stations(diameter, tangentLength, form.orientation(), support);
    }

    /**
     * Adds a side connection where it meets nothing: a standing drum's as high as it goes (an inlet's usual place),
     * facing your right, left, the back, then the front; a lying drum's nearest its front head, on top, at the sides,
     * then underneath. Where every place clashes it is added anyway at the first, and the status says what it meets.
     */
    private void add() {
        int[] stations = stations();
        boolean lying = form.orientation().horizontal();
        int[] sides = lying ? new int[]{1, 0, 2, 3} : new int[]{3, 1, 2, 0};
        EquipmentShape.Connection chosen = null;
        search:
        for (int k = 0; k <= stations[1] - stations[0]; k++) {
            int station = lying ? stations[0] + k : stations[1] - k;
            for (int side : sides) {
                var candidate = new EquipmentShape.Connection(side, station, 0);
                var trial = new ArrayList<>(connections);
                trial.add(candidate);
                if (EquipmentShape.drum(diameter, tangentLength, form.orientation(), front(), support, trial).clashes().isEmpty()) {
                    chosen = candidate;
                    break search;
                }
            }
        }
        connections.add(chosen != null ? chosen : new EquipmentShape.Connection(sides[0], lying ? stations[0] : stations[1], 0));
        selected = connections.size() - 1;
        refresh();
        rebuildWidgets();
    }

    private EquipmentShape.Heading front() {
        return form.front(facing.getStepX(), facing.getStepZ());
    }

    /**
     * The way frame direction {@code sign} along {@code axis} points in the world as seen from the constructor: front
     * (back towards it), back, left, right, top or bottom.
     */
    private String toward(int axis, int sign) {
        var d = new double[3];
        d[axis] = sign;
        var w = layout.shape().transform().direction(d);
        long x = Math.round(w[0]), y = Math.round(w[1]), z = Math.round(w[2]);
        if (y != 0) return y > 0 ? "top" : "bottom";
        int fx = facing.getStepX(), fz = facing.getStepZ();
        if (x == -fx && z == -fz) return "front";
        if (x == fx && z == fz) return "back";
        // The constructor's right is its facing turned clockwise seen from above.
        return x == -fz && z == fx ? "right" : "left";
    }

    /** The first clash of the draft's side connections, or null. */
    private @Nullable EquipmentShape.Clash clash() {
        var clashes = layout.shape().clashes();
        return clashes.isEmpty() ? null : clashes.getFirst();
    }

    /** What clash {@code clash} meets, as the status says it. */
    private static Component clashText(EquipmentShape.Clash clash) {
        int k = clash.connection() + 1;
        return switch (clash.other()) {
            case EquipmentShape.Clash.VAPOUR -> Component.translatable(KEY + "clash.vapour", k);
            case EquipmentShape.Clash.LIQUID -> Component.translatable(KEY + "clash.liquid", k);
            case EquipmentShape.Clash.SADDLE -> Component.translatable(KEY + "clash.saddle", k);
            case EquipmentShape.Clash.GROUND -> Component.translatable(KEY + "clash.ground", k);
            default -> Component.translatable(KEY + "clash.connection", k, clash.other() + 1);
        };
    }

    /** The page's rows, top-down: its title, then what each row sets. */
    private String[] rows() {
        if (page == Page.CONNECTIONS)
            return new String[]{"connections", "connection", "faces", form.orientation().horizontal() ? "along" : "height", "offset"};
        if (page == Page.PAINT) return new String[]{"paint", "paint.body", "paint.structure", "paint.palette", "paint.palette", "custom", "paint.default"};
        return switch (Equipment.of(form)) {
            case VERTICAL_DRUM -> new String[]{"equipment", "diameter", "tangent_length", "skirt", "forward", "right", "up"};
            case HORIZONTAL_DRUM -> new String[]{"equipment", "diameter", "tangent_length", "axis", "saddles", "forward", "right", "up"};
            case COLUMN -> new String[]{"equipment", "diameter", "trays", "feed", "skirt", "forward", "right", "up"};
            case EXCHANGER -> new String[]{"equipment", "shell", "tubes", "shell_bottom", "forward", "right", "up"};
            case KETTLE -> new String[]{"equipment", "bundle", "tubes", "kettle_bottom", "forward", "right", "up"};
            case AIR_COOLER -> new String[]{"equipment", "bay", "parallel", "series", "bundle_height", "forward", "right", "up"};
            case EJECTOR -> new String[]{"equipment", "suction", "discharge", "forward", "right", "up"};
        };
    }

    /** What row {@code row} sets, as its hover text says it. */
    private Component hint(int row) {
        String name = rows()[row];
        return Component.translatable(name.equals("equipment") ? Equipment.of(form).key() + ".hint" : KEY + name + ".hint");
    }

    /** A row of [−] value [+], each button saying what the row sets; shift steps by five. */
    private void stepper(int row, IntConsumer change) {
        int y = TOP + row * ROW;
        var hint = Tooltip.create(hint(row).copy().append("\n").append(Component.translatable(KEY + "shift")));
        var minus = Button.builder(Component.literal("−"), b -> edit(change, -1)).bounds(PANEL - 80, y, 20, 16).build();
        var plus = Button.builder(Component.literal("+"), b -> edit(change, 1)).bounds(PANEL - 26, y, 20, 16).build();
        minus.setTooltip(hint);
        plus.setTooltip(hint);
        addRenderableWidget(minus);
        addRenderableWidget(plus);
    }

    private void edit(IntConsumer change, int direction) {
        change.accept(direction * (hasShiftDown() ? 5 : 1));
        refresh();
    }

    /** The air cooler's bay {@code steps} steps on along {@link ProcessUnit#bays}: narrowest and shortest first. */
    private void bay(int steps) {
        var bays = ProcessUnit.bays();
        int at = 0;
        for (int i = 0; i < bays.size(); i++) if (bays.get(i)[0] == diameter && bays.get(i)[1] == tangentLength) at = i;
        var bay = bays.get(clamp(at + steps, 0, bays.size() - 1));
        diameter = bay[0];
        tangentLength = (int) bay[1];
    }

    /**
     * The diameter (a unit's size) {@code steps} steps on: 0.5 m for a vessel, whose skirt left at its default follows the
     * diameter's default and stays tall enough to enclose the bottom head; a unit's step, its tubes kept at least three
     * bundle diameters long.
     */
    private void diameter(int steps) {
        if (form.unit()) {
            var kind = form.kind();
            diameter = ProcessUnit.snap(kind, diameter + steps * ProcessUnit.diameterStep(kind));
            tangentLength = clamp(tangentLength, ProcessUnit.minLength(kind, diameter), ProcessUnit.maxLength(kind));
            return;
        }
        boolean standing = form.orientation() == EquipmentShape.Orientation.VERTICAL;
        boolean followed = standing && support == Math.max(EquipmentShape.defaultSupport(diameter, EquipmentShape.Orientation.VERTICAL), minimumSupport());
        diameter = vesselDiameter(diameter + .5 * steps);
        support = Math.max(followed ? EquipmentShape.defaultSupport(diameter, EquipmentShape.Orientation.VERTICAL) : support, minimumSupport());
    }

    /** {@code value} on the vessels' 0.5 m grid, within this form's range. */
    private double vesselDiameter(double value) {
        double maximum = form.column() ? EquipmentShape.MAX_COLUMN_DIAMETER : EquipmentShape.MAX_DIAMETER;
        return Math.max(EquipmentShape.MIN_DIAMETER, Math.min(maximum, Math.round(value * 2) / 2.0));
    }

    /**
     * Goes to the design of {@code equipment}, keeping the draft's sizes within the new kind's ranges; a horizontal drum
     * keeps its axis. A change between standing and lying, or to or from a unit, proposes that support's default height.
     */
    private void choose(Equipment equipment) {
        var before = form;
        form = switch (equipment) {
            case VERTICAL_DRUM -> VesselPlacement.Form.VERTICAL;
            case HORIZONTAL_DRUM -> Equipment.of(form) == Equipment.HORIZONTAL_DRUM ? form : VesselPlacement.Form.LENGTHWISE;
            case COLUMN -> VesselPlacement.Form.COLUMN;
            case EXCHANGER -> VesselPlacement.Form.EXCHANGER;
            case KETTLE -> VesselPlacement.Form.KETTLE;
            case AIR_COOLER -> VesselPlacement.Form.AIR_COOLER;
            case EJECTOR -> VesselPlacement.Form.EJECTOR;
        };
        if (form.unit()) {
            var kind = form.kind();
            diameter = ProcessUnit.snap(kind, diameter);
            tangentLength = clamp(tangentLength, ProcessUnit.minLength(kind, diameter), ProcessUnit.maxLength(kind));
            if (before.kind() != kind) support = ProcessUnit.defaultSupport(kind);
        } else {
            diameter = vesselDiameter(diameter);
            tangentLength = clamp(tangentLength, 1, (int) EquipmentShape.MAX_TANGENT_LENGTH);
            if (before.unit() || before.orientation() != form.orientation()) support = EquipmentShape.defaultSupport(diameter, form.orientation());
        }
        support = Math.max(support, minimumSupport());
        refresh();
        show(Page.DESIGN);
    }

    private void importColumn() {
        var constructor = constructor();
        if (constructor == null) return;
        importing = constructor.design();
        EquipmentNetwork.requestImport(position);
    }

    private int minimumSupport() {
        if (form.unit()) return ProcessUnit.minSupport(form.kind());
        if (form.column()) return EquipmentShape.minimumSkirt(diameter);
        return form.orientation() == EquipmentShape.Orientation.VERTICAL ? EquipmentShape.minimumSkirt(diameter) : 1;
    }

    private void refresh() {
        fitConnections();
        var shape = design().shape(facing);
        if (!shape.equals(layout.shape())) layout = ShapeRasterizer.layout(shape);
        if (build != null) build.active = clash() == null;
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }

    private void build() {
        EquipmentNetwork.send(position, design(), true);
        sent = true;
        onClose();
    }

    @Override
    public void tick() {
        var constructor = constructor();
        if (constructor == null) {
            onClose();
            return;
        }
        // An import arrives as the constructor's synced design: the draft takes it whole.
        if (importing != null && !constructor.design().equals(importing)) {
            importing = null;
            load(constructor.design());
            rebuildWidgets();
        }
    }

    /** Closing keeps the draft as the constructor's design (unless Build already sent it). */
    @Override
    public void removed() {
        if (!sent && !design().equals(saved)) EquipmentNetwork.send(position, design(), false);
        sent = true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** No blur: the ghost in the world is half of this screen. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, PANEL, height, 0xE0202A31);
        graphics.fill(PANEL, 0, PANEL + 1, height, ProcessUi.LINE);
        if (page == Page.DESIGN && form.column()) graphics.fill(width - 4 - STRIP, 20, width - 4, height - 20, 0xC0202A31);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, 8, 8, ProcessUi.TEXT);
        switch (page) {
            case CHOOSE -> choices(graphics);
            case DESIGN -> designRows(graphics, mouseX, mouseY);
            case CONNECTIONS -> connectionRows(graphics, mouseX, mouseY);
            case PAINT -> paintRows(graphics, mouseX, mouseY);
        }
    }

    /**
     * The paint page: each channel's label (the edited one marked) and colour; the edited colour's name, what it paints,
     * and what keeps its own colour; the ghost shows the paint.
     */
    private void paintRows(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] names = rows();
        graphics.drawString(font, Component.translatable(KEY + "paint"), 8, TOP + 4, ProcessUi.ACCENT);
        for (int c : new int[]{Paint.BODY, Paint.STRUCTURE}) {
            int y = TOP + (1 + c) * ROW;
            graphics.drawString(font, (c == channel ? "▶ " : "  ") + Component.translatable(KEY + names[1 + c]).getString(), 8, y + 4, c == channel ? ProcessUi.ACCENT : ProcessUi.TEXT);
            graphics.fill(PANEL - 100, y + 1, PANEL - 84, y + 15, ProcessUi.LINE);
            graphics.fill(PANEL - 99, y + 2, PANEL - 85, y + 14, 0xFF000000 | paint.colour(c));
        }
        graphics.drawString(font, Component.translatable(KEY + "custom"), 8, TOP + 5 * ROW + 4, ProcessUi.TEXT);
        var swatch = Paint.swatch(paint.colour(channel));
        Component name = swatch != null ? Component.translatable(KEY + "paint.named", swatch.ral(), Component.translatable(PAINT_KEY + swatch.key()))
                : Component.translatable(KEY + "paint.custom", String.format(Locale.ROOT, "#%06X", paint.colour(channel)));
        graphics.drawString(font, name, 8, LINES, ProcessUi.MUTED);
        graphics.drawString(font, Component.translatable(KEY + (channel == Paint.BODY ? "paint.body.parts" : "paint.structure.parts")), 8, LINES + LINE, ProcessUi.MUTED);
        graphics.drawString(font, Component.translatable(KEY + "paint.unpainted"), 8, LINES + 2 * LINE, ProcessUi.MUTED);
        status(graphics);
        describe(mouseX, mouseY, names.length, new String[]{"paint.named", "paint.parts", "paint.unpainted"});
    }

    /** The status line: an import under way, the first clash, or whether the ghost's cells are free. */
    private void status(GuiGraphics graphics) {
        int blocked = EquipmentGhost.blockedCount();
        var clash = clash();
        var status = importing != null ? Component.translatable(KEY + "importing")
                : clash != null ? clashText(clash)
                : EquipmentGhost.standing() ? Component.translatable(KEY + "standing")
                : blocked == 0 ? Component.translatable(KEY + "clear") : Component.translatable(KEY + "blocked", blocked);
        graphics.drawString(font, status, 8, STATUS, clash == null && blocked == 0 && !EquipmentGhost.standing() ? ProcessUi.ACCENT : ProcessUi.WARN);
    }

    private void designRows(GuiGraphics graphics, int mouseX, int mouseY) {
        var equipment = Equipment.of(form);
        String[] names = rows();
        String place = Integer.toString(forward), shift = Integer.toString(right), lift = Integer.toString(up), across = size(diameter) + " m";
        String[] values = switch (equipment) {
            case VERTICAL_DRUM -> new String[]{"", across, tangentLength + " m", support + " m", place, shift, lift};
            case HORIZONTAL_DRUM -> new String[]{"", across, tangentLength + " m", "", support + " m", place, shift, lift};
            case COLUMN -> new String[]{"", across, Integer.toString(trays.trays()), Integer.toString(trays.feed()), support + " m", place, shift, lift};
            case EXCHANGER, KETTLE -> new String[]{"", across, tangentLength + " m", support + " m", place, shift, lift};
            case AIR_COOLER -> new String[]{"", size(diameter) + " × " + tangentLength + " m", Integer.toString(bank.parallel()),
                    Integer.toString(bank.series()), support + " m", place, shift, lift};
            case EJECTOR -> new String[]{"", across, support + " m", place, shift, lift};
        };
        graphics.drawString(font, Component.translatable(equipment.key()), 8, TOP + 4, ProcessUi.ACCENT);
        for (int row = 1; row < names.length; row++) {
            int y = TOP + row * ROW + 4;
            graphics.drawString(font, Component.translatable(KEY + names[row]), 8, y, ProcessUi.TEXT);
            if (!values[row].isEmpty()) graphics.drawCenteredString(font, values[row], PANEL - 43, y, ProcessUi.ACCENT);
        }
        var shape = layout.shape();
        // The middle line says what the equipment holds or does: a vessel's volume, an exchanger's or a kettle's tube
        // surface, an air cooler bay's face, an ejector's length.
        String middle = switch (equipment) {
            case VERTICAL_DRUM, HORIZONTAL_DRUM, COLUMN -> "volume";
            case EXCHANGER -> "surface";
            case KETTLE -> "kettle_surface";
            case AIR_COOLER -> "bay_face";
            case EJECTOR -> "ejector_length";
        };
        Component held = switch (equipment) {
            case VERTICAL_DRUM, HORIZONTAL_DRUM, COLUMN -> Component.translatable(KEY + middle, number(shape.volume()), number(shape.surfaceArea()));
            case EXCHANGER -> Component.translatable(KEY + middle, number(ProcessUnit.tubeSurface(diameter, tangentLength)));
            case KETTLE -> Component.translatable(KEY + middle, size(ProcessUnit.kettleShell(diameter)), number(ProcessUnit.tubeSurface(diameter, tangentLength)));
            case AIR_COOLER -> Component.translatable(KEY + middle, number(bank.bays() * ProcessUnit.bundleFace(diameter, tangentLength)),
                    bank.bays() * ProcessUnit.fans(diameter, tangentLength).count(), size(ProcessUnit.fans(diameter, tangentLength).diameter()));
            case EJECTOR -> Component.translatable(KEY + middle, number(ProcessUnit.ejectorLength(diameter)));
        };
        int y = LINES;
        for (var line : new Component[]{
                Component.translatable(KEY + "extent", number(shape.topElevation()), number(shape.overallLength())),
                held,
                Component.translatable(KEY + "cells", layout.shell().size(), layout.interior().size())}) {
            graphics.drawString(font, line, 8, y, ProcessUi.MUTED);
            y += LINE;
        }
        status(graphics);
        if (form.column()) schematic(graphics);
        describe(mouseX, mouseY, names.length, new String[]{"extent", middle, "cells"});
    }

    /**
     * The connections page: the selected connection's side (on its button), height or place along the axis, and offset;
     * the largest pipe its nozzle takes there and what its port draws; the first clash, if any.
     */
    private void connectionRows(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] names = rows();
        graphics.drawString(font, Component.translatable(KEY + "connections"), 8, TOP + 4, ProcessUi.ACCENT);
        graphics.drawString(font, Component.translatable(KEY + "connection"), 8, TOP + ROW + 4, ProcessUi.TEXT);
        graphics.drawCenteredString(font, connections.isEmpty() ? "0" : (selected + 1) + " / " + connections.size(), PANEL - 43, TOP + ROW + 4, ProcessUi.ACCENT);
        Component status;
        if (connections.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY + "connections.none"), 8, TOP + 2 * ROW + 4, ProcessUi.MUTED);
            status = Component.translatable(KEY + "clash.none");
        } else {
            var c = connection();
            graphics.drawString(font, Component.translatable(KEY + "faces"), 8, TOP + 2 * ROW + 4, ProcessUi.TEXT);
            graphics.drawString(font, Component.translatable(KEY + names[3]), 8, TOP + 3 * ROW + 4, ProcessUi.TEXT);
            graphics.drawCenteredString(font, number(c.station() + .5) + " m", PANEL - 43, TOP + 3 * ROW + 4, ProcessUi.ACCENT);
            // The offset's label says which way it runs, as the side's button does.
            graphics.drawString(font, c.offset() == 0 ? Component.translatable(KEY + "offset")
                    : Component.translatable(KEY + "offset." + toward(c.across(), Integer.signum(c.offset()))), 8, TOP + 4 * ROW + 4, ProcessUi.TEXT);
            graphics.drawCenteredString(font, Math.abs(c.offset()) + " m", PANEL - 43, TOP + 4 * ROW + 4, ProcessUi.ACCENT);
            var nozzles = layout.shape().nozzles();
            var nozzle = nozzles.get(nozzles.size() - connections.size() + selected);
            graphics.drawString(font, Component.translatable(KEY + "connection.size", String.format(Locale.ROOT, "%.2f", nozzle.maxOd())), 8, LINES, ProcessUi.MUTED);
            graphics.drawString(font, Component.translatable(KEY + "connection.port"), 8, LINES + LINE, ProcessUi.MUTED);
            var clash = clash();
            status = clash != null ? clashText(clash) : Component.translatable(KEY + "clash.none");
        }
        graphics.drawString(font, status, 8, STATUS, clash() == null ? ProcessUi.ACCENT : ProcessUi.WARN);
        describe(mouseX, mouseY, connections.isEmpty() ? 2 : names.length, connections.isEmpty() ? new String[0] : new String[]{"connection.size", "connection.port"});
    }

    /**
     * Hover text for the panel's words: a row's label or value says what the row sets (its buttons say it too), the
     * datasheet lines ({@code lines}, by key) and the status line what they report.
     */
    private void describe(int mouseX, int mouseY, int rows, String[] lines) {
        if (mouseX < 4 || mouseX >= PANEL - 4) return;
        Component text = null;
        int row = Math.floorDiv(mouseY - TOP, ROW);
        boolean label = mouseX < PANEL - 82, value = mouseX >= PANEL - 60 && mouseX < PANEL - 26;
        if (mouseY >= TOP && row < rows && (label || value && row > 0)) text = hint(row);
        else if (mouseY >= LINES && mouseY < LINES + lines.length * LINE) text = Component.translatable(KEY + lines[(mouseY - LINES) / LINE] + ".hint");
        else if (mouseY >= STATUS - 1 && mouseY < STATUS + 10) text = Component.translatable(KEY + (page == Page.CONNECTIONS ? "clash.hint" : "status.hint"));
        if (text != null) setTooltipForNextRenderPass(font.split(text, 200));
    }

    /** The choice page's heading; what each kind of equipment is shows over its button (the draft's kind is marked on it). */
    private void choices(GuiGraphics g) {
        g.drawString(font, Component.translatable(KEY + "choose"), 8, TOP + 4, ProcessUi.TEXT);
    }

    /**
     * The column's trays, feed, side draws, pumparounds and steam, drawn as the column calculator draws them, inside the
     * right strip: the draw marks end 4 pixels inside its right edge, the steam lines start at least 7 inside its left one.
     */
    private void schematic(GuiGraphics g) {
        int x = width - 8 - ColumnDiagram.MARK - 13, top = 32, bottom = height - 54, n = trays.trays();
        double pitch = (bottom - top - 34) / (double) Math.max(1, n);
        IntUnaryOperator at = k -> top + 14 + (int) Math.round((k - 1) * pitch);
        ColumnDiagram.shell(g, x, top, bottom, 0xFF2B3740, ProcessUi.LINE);
        for (int k = 1; k <= n; k++) if (pitch >= 3 || k % 5 == 0 || k == 1 || k == n) ColumnDiagram.tray(g, x, at.applyAsInt(k), ProcessUi.MUTED);
        int fy = at.applyAsInt(trays.feed());
        ColumnDiagram.feed(g, x, x - 70, fy, ProcessUi.ACCENT);
        g.drawString(font, Component.translatable(KEY + "schematic.feed", trays.feed()), x - 70, fy - 10, ProcessUi.ACCENT);
        for (int draw : trays.draws()) ColumnDiagram.draw(g, x, at.applyAsInt(draw), ProcessUi.ACCENT);
        for (int i = 0; i < trays.pumparounds().size(); i++) {
            var p = trays.pumparounds().get(i);
            ColumnDiagram.pumparound(g, x, i, at.applyAsInt(p.draw()), at.applyAsInt(p.ret()), ProcessUi.WARN);
        }
        for (int i = 0; i < trays.steam().size(); i++) ColumnDiagram.steam(g, x, i, at.applyAsInt(trays.steam().get(i)), ProcessUi.WARN);
        int left = width - STRIP;
        g.drawString(font, Component.translatable(KEY + "schematic.draws", trays.draws().size()), left, height - 46, ProcessUi.MUTED);
        g.drawString(font, Component.translatable(KEY + "schematic.extras", trays.pumparounds().size(), trays.steam().size()), left, height - 35, ProcessUi.MUTED);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** A size on a 0.25 m or 0.1 m grid: one decimal where it has one, two where it has two (0.75). */
    private static String size(double value) {
        double tenths = value * 10;
        return Math.abs(tenths - Math.rint(tenths)) < 1e-6 ? number(value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
