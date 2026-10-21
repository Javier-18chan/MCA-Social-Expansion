package com.joelj.mcasocial.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class ContactPhotoCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Photo> PHOTOS = new HashMap<>();
    private static final Set<UUID> LOADING = new HashSet<>();
    private static final Map<UUID, Integer> CAPTURE_ATTEMPTS = new HashMap<>();
    private static final Set<UUID> DISK_CHECKED = new HashSet<>();
    private static final Set<UUID> REFRESH_PENDING = new HashSet<>();
    private static final Map<UUID, Long> RETRY_AFTER = new HashMap<>();
    private static String scope = "";
    private static Object session;
    private static long nextCaptureTime;

    private ContactPhotoCache() {}

    static Photo get(UUID uuid) {
        if (uuid == null) return null;
        ensureScope();
        Photo photo = PHOTOS.get(uuid);
        if (photo != null || !DISK_CHECKED.add(uuid)) return photo;

        String loadScope = scope;
        Path file = path(uuid);
        LOADING.add(uuid);
        CompletableFuture.supplyAsync(() -> readPhoto(uuid, file), Util.ioPool())
                .thenAccept(image -> Minecraft.getInstance().execute(() -> {
                    if (!scope.equals(loadScope)) {
                        if (image != null) image.close();
                        return;
                    }
                    LOADING.remove(uuid);
                    if (image != null) register(uuid, image);
                }));
        return null;
    }

    private static NativeImage readPhoto(UUID uuid, Path file) {
        if (!Files.isRegularFile(file)) return null;
        try (InputStream stream = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(stream);
            if (image.getWidth() < 8 || image.getWidth() > 256 || image.getHeight() != image.getWidth()) {
                image.close();
                return null;
            }
            return image;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not load MCA Social Expansion portrait for {}", uuid, e);
            return null;
        }
    }

    static boolean shouldCapture(UUID uuid, float screenOffsetY) {
        if (uuid == null || Math.abs(screenOffsetY) > 0.5F) return false;
        if (LOADING.contains(uuid) || CAPTURE_ATTEMPTS.getOrDefault(uuid, 0) >= 3
                || PHOTOS.containsKey(uuid) && !REFRESH_PENDING.contains(uuid)) return false;
        long now = Util.getMillis();
        if (now < nextCaptureTime || now < RETRY_AFTER.getOrDefault(uuid, 0L)) return false;
        RETRY_AFTER.put(uuid, now + 5000L);
        nextCaptureTime = now + 250L;
        return true;
    }

    static boolean isRefreshPending(UUID uuid) {
        return uuid != null && REFRESH_PENDING.contains(uuid);
    }

    static void requestRefresh(UUID uuid) {
        ensureScope();
        REFRESH_PENDING.add(uuid);
        CAPTURE_ATTEMPTS.remove(uuid);
        RETRY_AFTER.remove(uuid);
    }

    static void capture(UUID uuid, GuiGraphics graphics, int x, int y, int size, float screenOffsetY) {
        Minecraft minecraft = Minecraft.getInstance();
        double scale = minecraft.getWindow().getGuiScale();
        int pixelX = (int) Math.round(x * scale);
        int pixelY = (int) Math.round((y + screenOffsetY) * scale);
        int pixelSize = (int) Math.round(size * scale);
        if (pixelSize < 8) return;

        CAPTURE_ATTEMPTS.merge(uuid, 1, Integer::sum);
        graphics.flush();
        try (NativeImage screen = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            if (pixelX < 0 || pixelY < 0 || pixelX + pixelSize > screen.getWidth()
                    || pixelY + pixelSize > screen.getHeight()) return;

            int storedSize = Math.min(128, pixelSize);
            NativeImage image = new NativeImage(storedSize, storedSize, false);
            Set<Integer> colors = new HashSet<>();
            for (int row = 0; row < storedSize; row++) {
                for (int col = 0; col < storedSize; col++) {
                    if (outsideRoundedCorner(col, row, storedSize)) {
                        image.setPixelRGBA(col, row, 0);
                    } else {
                        int color = screen.getPixelRGBA(pixelX + col * pixelSize / storedSize, pixelY + row * pixelSize / storedSize);
                        image.setPixelRGBA(col, row, color);
                        if (colors.size() < 4) colors.add(color);
                    }
                }
            }

            if (colors.size() < 4) {
                image.close();
                return;
            }

            savePhoto(uuid, image);
            register(uuid, image);
            REFRESH_PENDING.remove(uuid);
            CAPTURE_ATTEMPTS.remove(uuid);
            nextCaptureTime = Util.getMillis() + 1000L;
        } catch (RuntimeException e) {
            LOGGER.warn("Could not capture MCA Social Expansion portrait for {}", uuid, e);
        }
    }

    private static void savePhoto(UUID uuid, NativeImage image) {
        Path file = path(uuid);
        NativeImage copy = new NativeImage(image.getWidth(), image.getHeight(), false);
        copy.copyFrom(image);
        Util.ioPool().execute(() -> {
            try (copy) {
                Files.createDirectories(file.getParent());
                copy.writeToFile(file);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Could not save MCA Social Expansion portrait for {}", uuid, e);
            }
        });
    }

    private static boolean outsideRoundedCorner(int x, int y, int size) {
        int radius = Math.max(2, Math.round(size * 0.15F));
        int cornerX = x < radius ? radius : x >= size - radius ? size - radius - 1 : x;
        int cornerY = y < radius ? radius : y >= size - radius ? size - radius - 1 : y;
        int dx = x - cornerX;
        int dy = y - cornerY;
        return dx * dx + dy * dy > radius * radius;
    }

    private static Photo register(UUID uuid, NativeImage image) {
        ResourceLocation texture = Minecraft.getInstance().getTextureManager().register(
                "mcasocial/portrait", new DynamicTexture(image));
        Photo photo = new Photo(texture, image.getWidth());
        Photo previous = PHOTOS.put(uuid, photo);
        if (previous != null) Minecraft.getInstance().getTextureManager().release(previous.texture());
        return photo;
    }

    private static Path path(UUID uuid) {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("mcasocial").resolve("portraits").resolve(scope).resolve(uuid + ".png");
    }

    private static void ensureScope() {
        Minecraft minecraft = Minecraft.getInstance();
        Object currentSession = minecraft.getConnection() != null
                ? minecraft.getConnection() : minecraft.level;
        if (session == currentSession && !scope.isEmpty()) return;

        String world;
        if (minecraft.getCurrentServer() != null) {
            world = "server:" + minecraft.getCurrentServer().ip;
        } else if (minecraft.getSingleplayerServer() != null) {
            world = "world:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            world = "unknown";
        }
        String current = UUID.nameUUIDFromBytes(world.getBytes(StandardCharsets.UTF_8)).toString();
        if (scope.equals(current)) {
            session = currentSession;
            return;
        }

        PHOTOS.values().forEach(photo -> minecraft.getTextureManager().release(photo.texture()));
        PHOTOS.clear();
        LOADING.clear();
        CAPTURE_ATTEMPTS.clear();
        DISK_CHECKED.clear();
        REFRESH_PENDING.clear();
        RETRY_AFTER.clear();
        nextCaptureTime = 0L;
        scope = current;
        session = currentSession;
    }

    record Photo(ResourceLocation texture, int pixelSize) {}
}
