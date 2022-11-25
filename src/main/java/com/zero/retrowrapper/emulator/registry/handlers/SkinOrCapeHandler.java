package com.zero.retrowrapper.emulator.registry.handlers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

import net.minecraft.launchwrapper.LogWrapper;

public final class SkinOrCapeHandler extends EmulatorHandler {
    private static final Pattern pngPattern = Pattern.compile(".png", Pattern.LITERAL);
    private final HashMap<String, byte[]> imagesCache = new HashMap<String, byte[]>();
    // TODO Refactor
    private final boolean isCape;

    public SkinOrCapeHandler(String url, boolean isCape) {
        super(url);
        // TODO Refactor
        this.isCape = isCape;
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        final String username = pngPattern.matcher(get.replace(url, "")).replaceAll("");
        final String cacheName;

        if (isCape) {
            cacheName = username + ".cape";
        } else {
            cacheName = username;
        }

        if (imagesCache.containsKey(cacheName)) {
            os.write(imagesCache.get(cacheName));
        } else {
            final byte[] bytes3 = downloadSkinOrCape(username, isCape);

            if (bytes3 != null) {
                final BufferedImage imgRaw = ImageIO.read(new ByteArrayInputStream(bytes3));
                final BufferedImage imgFixed = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
                imgFixed.getGraphics().drawImage(imgRaw, 0, 0, null);
                final ByteArrayOutputStream osImg = new ByteArrayOutputStream();
                ImageIO.write(imgFixed, "png", osImg);
                osImg.flush();
                final byte[] bytes = osImg.toByteArray();
                os.write(bytes);
                imagesCache.put(cacheName, bytes);
            }
        }
    }

    // TODO @Nullable?
    private static byte[] downloadSkinOrCape(String username, boolean cape) throws IOException {
        final String fileNameEnd;

        if (cape) {
            fileNameEnd = ".cape.png";
        } else {
            fileNameEnd = ".png";
        }

        final File imageCache = new File(RetroEmulator.getInstance().getCacheDirectory(), username + fileNameEnd);
        InputStream is = null;
        InputStreamReader reader = null;

        try {
            is = new URL("https://api.mojang.com/users/profiles/minecraft/" + username + "?at=" + System.currentTimeMillis()).openStream();
            reader = new InputStreamReader(is);
            final JsonObject profile1 = (JsonObject) Json.parse(reader);
            final String uuid = profile1.get("id").asString();
            LogWrapper.fine(uuid);
            InputStream is2 = null;
            InputStreamReader reader2 = null;
            InputStream is3 = null;
            FileOutputStream fos = null;

            try {
                is2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).openStream();
                reader2 = new InputStreamReader(is2);
                final JsonObject profile2 = (JsonObject) Json.parse(reader2);
                final Iterable<JsonValue> properties = (JsonArray) profile2.get("properties");
                String base64 = "";

                for (final JsonValue property : properties) {
                    final JsonObject propertyj = property.asObject();

                    if ("textures".equalsIgnoreCase(propertyj.get("name").asString())) {
                        base64 = propertyj.get("value").asString();
                    }
                }

                final JsonObject textures1 = (JsonObject) Json.parse(new String(Base64.decodeBase64(base64)));
                final JsonObject textures = (JsonObject) textures1.get("textures");
                final JsonObject imageLinkJSON;

                if (cape) {
                    imageLinkJSON = (JsonObject) textures.get("CAPE");
                } else {
                    imageLinkJSON = (JsonObject) textures.get("SKIN");
                }

                if (imageLinkJSON == null) {
                    if (cape) {
                        LogWrapper.warning("No cape found for username " + username);
                    } else {
                        LogWrapper.warning("No skin found for username " + username);
                    }

                    return null;
                }

                final String imageURL = imageLinkJSON.get("url").asString();
                LogWrapper.fine(imageURL);
                is3 = new URL(imageURL).openStream();
                final byte[] imageBytes = IOUtils.toByteArray(is3);

                try {
                    fos = new FileOutputStream(imageCache);
                    fos.write(imageBytes);
                } finally {
                    IOUtils.closeQuietly(fos);
                }

                return imageBytes;
            } finally {
                IOUtils.closeQuietly(is2);
                IOUtils.closeQuietly(reader2);
                IOUtils.closeQuietly(is3);
                IOUtils.closeQuietly(fos);
            }
        } catch (final Exception e) {
            LogWrapper.warning("Error when trying to get skin or cape for username " + username + ": " + ExceptionUtils.getStackTrace(e));

            if (imageCache.exists()) {
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(imageCache);
                    return IOUtils.toByteArray(fis);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            }

            return null;
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(is);
        }
    }
}
