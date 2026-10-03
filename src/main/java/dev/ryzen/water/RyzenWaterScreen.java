package dev.ryzen.water;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;

public class RyzenWaterScreen extends Screen {
    private final Screen parent;

    public RyzenWaterScreen(Screen parent) {
        super(Text.literal("RyzenWater"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RyzenWaterConfig c = RyzenWaterConfig.INSTANCE;
        int w = 220;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 100;
        int step = 24;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Водичка: " + (c.enabled ? "ВКЛ" : "ВЫКЛ")),
                b -> { c.enabled = !c.enabled; clearAndInit(); }
        ).dimensions(x, y, w, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Цвет: " + RyzenWaterConfig.COLOR_NAMES[c.color]),
                b -> { c.color = (c.color + 1) % RyzenWaterConfig.COLOR_NAMES.length; clearAndInit(); }
        ).dimensions(x, y + step, w, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Плавный край: " + (c.edgeFade ? "ДА" : "НЕТ")),
                b -> { c.edgeFade = !c.edgeFade; clearAndInit(); }
        ).dimensions(x, y + step * 2, w, 20).build());

        addDrawableChild(new Slider(x, y + step * 3, w, 20, "Радиус", 3, 16, c.radius, true,
                v -> c.radius = (int) v));
        addDrawableChild(new Slider(x, y + step * 4, w, 20, "Прозрачность", 0.1, 1.0, c.opacity, false,
                v -> c.opacity = (float) v));
        addDrawableChild(new Slider(x, y + step * 5, w, 20, "Высота волн", 0.0, 0.12, c.waveHeight, false,
                v -> c.waveHeight = (float) v));
        addDrawableChild(new Slider(x, y + step * 6, w, 20, "Скорость волн", 0.2, 3.0, c.waveSpeed, false,
                v -> c.waveSpeed = (float) v));

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(x, y + step * 7 + 8, w, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 125, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Вкл/выкл в игре: клавиша K"),
                this.width / 2, this.height / 2 - 112, 0xAAAAAA);
    }

    @Override
    public void close() {
        RyzenWaterConfig.save();
        this.client.setScreen(parent);
    }

    private static class Slider extends SliderWidget {
        private final String label;
        private final double min, max;
        private final boolean integer;
        private final DoubleConsumer setter;

        Slider(int x, int y, int w, int h, String label, double min, double max, double cur,
               boolean integer, DoubleConsumer setter) {
            super(x, y, w, h, Text.empty(), (cur - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.setter = setter;
            updateMessage();
        }

        private double real() {
            double v = min + this.value * (max - min);
            return integer ? Math.round(v) : v;
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + (integer ? String.valueOf((int) real()) : String.format("%.2f", real()))));
        }

        @Override
        protected void applyValue() {
            setter.accept(real());
        }
    }
}
