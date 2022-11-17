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

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

public final class SkinOrCapeHandler extends EmulatorHandler {
    private final HashMap<String, byte[]> imagesCache = new HashMap<String, byte[]>();
    // TODO Refactor
    private final boolean isCape;

    public SkinOrCapeHandler(String url, boolean isCape) {
        super(url);
        // TODO Refactor
        this.isCape = isCape;
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        final String username = get.replace(url, "").replace(".png", "");
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
        InputStreamReader reader = null;

        try {
            reader = new InputStreamReader(new URL("https://api.mojang.com/users/profiles/minecraft/" + username + "?at=" + System.currentTimeMillis()).openStream());
            final JsonObject profile1 = (JsonObject) Json.parse(reader);
            final String uuid = profile1.get("id").asString();
            System.out.println(uuid);
            InputStreamReader reader2 = null;

            try {
                reader2 = new InputStreamReader(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).openStream());
                final JsonObject profile2 = (JsonObject) Json.parse(reader2);
                final JsonArray properties = (JsonArray) profile2.get("properties");
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
                        System.out.println("No cape found for username " + username);
                    } else {
                        System.out.println("No skin found for username " + username);
                    }

                    return null;
                }

                final String imageURL = imageLinkJSON.get("url").asString();
                System.out.println(imageURL);
                final InputStream is = new URL(imageURL).openStream();
                final byte[] imageBytes = IOUtils.toByteArray(is);
                FileOutputStream fos = null;

                try {
                    fos = new FileOutputStream(imageCache);
                    fos.write(imageBytes);
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (final IOException ee) {
                            // TODO Better error handling
                            ee.printStackTrace();
                        }
                    }
                }

                return imageBytes;
            } finally {
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (final IOException ee) {
                        // TODO Better error handling
                        ee.printStackTrace();
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();

            if (imageCache.exists()) {
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(imageCache);
                    return IOUtils.toByteArray(fis);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (final IOException ee) {
                            // TODO Better error handling
                            ee.printStackTrace();
                        }
                    }
                }
            }

            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException ee) {
                    // TODO Better error handling
                    ee.printStackTrace();
                }
            }
        }
    }
}
