package dev.ryzen.water;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class RyzenWaterClient implements ClientModInitializer {
    // Ванильная анимированная текстура воды (32 кадра по вертикали)
    private static final Identifier WATER = Identifier.ofVanilla("textures/block/water_still.png");

    private static KeyBinding toggleKey;
    private static double lastGroundY = Double.NaN;

    @Override
    public void onInitializeClient() {
        RyzenWaterConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ryzenwater.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.ryzenwater"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                RyzenWaterConfig c = RyzenWaterConfig.INSTANCE;
                c.enabled = !c.enabled;
                RyzenWaterConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Ryzen Water: " + (c.enabled ? "ON" : "OFF")), true);
                }
            }
            if (client.player != null && client.player.isOnGround()) {
                lastGroundY = client.player.getY();
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(RyzenWaterClient::render);
    }

    private static float height(double x, double z, double t, float amp) {
        double w = Math.sin(x * 1.4 + t) + Math.sin(z * 1.1 - t * 1.2) + Math.sin((x + z) * 0.8 + t * 0.7);
        // минимум 0.02 — волны всегда выше пола, без мерцания
        return (float) (0.02 + amp * (1.0 + w / 3.0));
    }

    private static void render(WorldRenderContext ctx) {
        RyzenWaterConfig cfg = RyzenWaterConfig.INSTANCE;
        if (!cfg.enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || Double.isNaN(lastGroundY)) return;

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (ms == null || consumers == null) return;

        Vec3d cam = ctx.camera().getPos();
        float td = ctx.tickCounter().getTickDelta(false);
        Vec3d pos = player.getLerpedPos(td);

        long worldTime = mc.world.getTime();
        double t = (worldTime + td) * 0.1 * cfg.waveSpeed;
        int frame = (int) ((worldTime / 2) % 32);
        float vFrame = frame / 32f;

        int floorY = MathHelper.floor(lastGroundY + 0.01);
        int radius = cfg.radius;
        float edgeStart = radius * 0.75f;

        RenderLayer layer = RenderLayer.getEntityTranslucent(WATER);
        VertexConsumer vc = consumers.getBuffer(layer);
        Matrix4f m = ms.peek().getPositionMatrix();

        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int ov = OverlayTexture.DEFAULT_UV;

        int cx = MathHelper.floor(pos.x);
        int cz = MathHelper.floor(pos.z);
        boolean rainbow = cfg.color >= RyzenWaterConfig.COLORS.length;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int bx = cx + dx;
                int bz = cz + dz;

                // только над твёрдой землёй
                if (mc.world.getBlockState(new BlockPos(bx, floorY - 1, bz)).isAir()) continue;

                for (int sx = 0; sx < 2; sx++) {
                    for (int sz = 0; sz < 2; sz++) {
                        double wx0 = bx + sx * 0.5, wz0 = bz + sz * 0.5;
                        double wx1 = wx0 + 0.5, wz1 = wz0 + 0.5;

                        double dist = Math.hypot(wx0 + 0.25 - pos.x, wz0 + 0.25 - pos.z);
                        if (dist > radius) continue;

                        float a = cfg.opacity;
                        if (cfg.edgeFade && dist > edgeStart) {
                            a *= MathHelper.clamp((float) ((radius - dist) / (radius - edgeStart)), 0f, 1f);
                        }
                        if (a < 0.02f) continue;
                        int alpha = (int) (MathHelper.clamp(a, 0f, 1f) * 255);

                        int rgb;
                        if (rainbow) {
                            float hue = (float) (((worldTime + td) * 0.004 + (wx0 + wz0) * 0.03) % 1.0);
                            if (hue < 0) hue += 1f;
                            rgb = MathHelper.hsvToRgb(hue, 0.6f, 1.0f);
                        } else {
                            rgb = RyzenWaterConfig.COLORS[cfg.color];
                        }
                        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;

                        float x0 = (float) (wx0 - cam.x), x1 = (float) (wx1 - cam.x);
                        float z0 = (float) (wz0 - cam.z), z1 = (float) (wz1 - cam.z);
                        float base = (float) (floorY - cam.y);

                        float y00 = base + height(wx0, wz0, t, cfg.waveHeight);
                        float y01 = base + height(wx0, wz1, t, cfg.waveHeight);
                        float y11 = base + height(wx1, wz1, t, cfg.waveHeight);
                        float y10 = base + height(wx1, wz0, t, cfg.waveHeight);

                        float u0 = sx * 0.5f, u1 = u0 + 0.5f;
                        float v0 = vFrame + sz * 0.5f / 32f, v1 = v0 + 0.5f / 32f;

                        vc.vertex(m, x0, y00, z0).color(r, g, b, alpha).texture(u0, v0).overlay(ov).light(light).normal(0, 1, 0);
                        vc.vertex(m, x0, y01, z1).color(r, g, b, alpha).texture(u0, v1).overlay(ov).light(light).normal(0, 1, 0);
                        vc.vertex(m, x1, y11, z1).color(r, g, b, alpha).texture(u1, v1).overlay(ov).light(light).normal(0, 1, 0);
                        vc.vertex(m, x1, y10, z0).color(r, g, b, alpha).texture(u1, v0).overlay(ov).light(light).normal(0, 1, 0);
                    }
                }
            }
        }

        if (consumers instanceof VertexConsumerProvider.Immediate imm) {
            imm.draw(layer);
        }
    }
}
