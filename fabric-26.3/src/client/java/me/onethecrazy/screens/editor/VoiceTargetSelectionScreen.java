package me.onethecrazy.screens.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Searchable picker uses source shape IDs, so equal names on separate meshes stay distinct. */
public final class VoiceTargetSelectionScreen extends Screen {
    public record Entry(String id, String name) {}
    private final Screen parent;
    private final List<Entry> entries;
    private final Consumer<Entry> selection;
    private final List<Button> buttons = new ArrayList<>();
    private List<Entry> filtered;
    private String query = "";
    private int offset;
    private int listBottom;
    private int rows;

    public VoiceTargetSelectionScreen(Screen parent, List<Entry> entries, Consumer<Entry> selection) {
        super(Component.translatable("gui.fbxplayermodels.voice_shape_select"));
        this.parent = parent;
        this.entries = entries.stream().sorted(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER)).toList();
        this.selection = selection;
    }

    @Override
    protected void init() {
        int x = 12;
        int width = Math.max(1, this.width - 24);
        listBottom = this.height - 38;
        rows = Math.max(0, (listBottom - 64) / 24);
        buttons.clear();
        addRenderableOnly(this::renderContent);
        EditBox search = new EditBox(font, x, 34, width, 20,
                Component.translatable("gui.fbxplayermodels.voice_shape_search"));
        search.setMaxLength(256);
        search.setValue(query);
        search.setHint(Component.translatable("gui.fbxplayermodels.voice_shape_search"));
        search.setResponder(value -> { query = value; offset = 0; filter(); });
        addRenderableWidget(search);
        for (int row = 0; row < rows; row++) {
            final int index = row;
            Button button = Button.builder(Component.empty(), clicked -> {
                int selected = offset + index;
                if (selected >= filtered.size()) return;
                selection.accept(filtered.get(selected));
                onClose();
            }).bounds(x, 64 + row * 24, width, 20).build();
            buttons.add(button);
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.fbxplayermodels.shape_keys_back"), button -> onClose())
                .bounds(x, this.height - 28, width, 20).build());
        filter();
        setInitialFocus(search);
    }

    private void filter() {
        String text = query.toLowerCase(Locale.ROOT);
        filtered = entries.stream().filter(entry -> entry.name().toLowerCase(Locale.ROOT).contains(text)).toList();
        offset = Math.max(0, Math.min(offset, Math.max(0, filtered.size() - rows)));
        updateButtons();
    }

    private void updateButtons() {
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            int index = offset + i;
            button.visible = index < filtered.size();
            if (!button.visible) { if (getFocused() == button) setFocused(null); continue; }
            Entry entry = filtered.get(index);
            boolean duplicate = entries.stream().filter(candidate -> candidate.name().equals(entry.name())).count() > 1;
            String label = entry.name() + (duplicate ? " [" + entry.id() + "]" : "");
            button.setMessage(Component.literal(font.plainSubstrByWidth(label, Math.max(1, button.getWidth() - 12))));
            button.setTooltip(Tooltip.create(Component.literal(label)));
        }
    }

    private void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.text(font, title, (this.width - font.width(title)) / 2, 10, 0xFFFFFFFF, true);
        if (filtered.isEmpty()) context.text(font, Component.translatable("gui.fbxplayermodels.voice_shape_no_matches"),
                12, 64, 0xFFCCCCCC, false);
        if (filtered.size() > rows && rows > 0) {
            int thumb = Math.max(4, rows * (listBottom - 64) / filtered.size());
            int top = 64 + offset * (listBottom - 64 - thumb) / (filtered.size() - rows);
            context.fill(this.width - 9, 64, this.width - 5, listBottom, 0xFF333333);
            context.fill(this.width - 9, top, this.width - 5, top + thumb, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y >= 64 && y < listBottom && filtered.size() > rows) {
            offset = Math.max(0, Math.min(offset - (int) Math.round(vertical * 3), filtered.size() - rows));
            updateButtons();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
}
