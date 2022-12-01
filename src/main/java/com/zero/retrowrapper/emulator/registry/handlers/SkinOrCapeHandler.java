package com.zero.retrowrapper.emulator.registry.handlers;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.injector.RetroTweakInjectorTarget;
import com.zero.retrowrapper.util.NetworkUtil;

import net.minecraft.launchwrapper.LogWrapper;

public final class SkinOrCapeHandler extends EmulatorHandler {
    private static final Pattern pngPattern = Pattern.compile(".png", Pattern.LITERAL);
    private final HashMap<String, byte[]> imagesCache = new HashMap<String, byte[]>();
    // TODO Refactor
    private final boolean isCape;

    private static final byte[] HTTP_404_HEADERS =
        ("HTTP/1.1 404 Not Found\n"
         + "Date: Mon, 27 Jul 2009 12:28:53 GMT\n"
         + "Server: Apache/2.2.14 (Win32)\n"
         + "Content-Length: 0\n"
         + "Connection: Closed\r\n\n").getBytes();

    private static final File classiCubeDefaultChar = new File(RetroEmulator.getInstance().getCacheDirectory(), File.separator + "classicube" + File.separator + "char.png");

    // TODO Detect supported skin sizes
    private static final int SKIN_WIDTH = 64;
    private static final int SKIN_HEIGHT = 32;

    static {
        if (RetroTweakInjectorTarget.showClassiCubeUserDefaultSkin && !classiCubeDefaultChar.isFile()) {
            InputStream is = null;
            ZipInputStream zis = null;
            final URLConnection urlConnection;
            HttpURLConnection skinUrlConnection = null;

            try {
                urlConnection = new URL("https://classicube.net/static/default.zip").openConnection();

                if (urlConnection instanceof HttpURLConnection) {
                    skinUrlConnection = (HttpURLConnection) urlConnection;
                    skinUrlConnection.connect();
                    final int respCode = skinUrlConnection.getResponseCode();

                    if (respCode == HttpURLConnection.HTTP_OK) {
                        is = skinUrlConnection.getInputStream();
                        zis = new ZipInputStream(is);

                        while (true) {
                            final ZipEntry entry = zis.getNextEntry();

                            if (entry == null) {
                                LogWrapper.warning("Could not find char.png in ClassiCube default resources");
                                break;
                            }

                            if ("char.png".equals(entry.getName())) {
                                final byte[] charBytes = IOUtils.toByteArray(zis);
                                FileUtils.writeByteArrayToFile(classiCubeDefaultChar, charBytes);
                                break;
                            }

                            zis.closeEntry();
                        }
                    } else {
                        LogWrapper.warning("Response code " + respCode + " given while trying to get ClassiCube default resources");
                    }
                } else {
                    LogWrapper.severe("URL.openConnection() didn't return instance of HttpURLConnection");
                }
            } catch (final Exception e) {
                LogWrapper.warning("Exception thrown while trying to get ClassiCube skin: " + ExceptionUtils.getStackTrace(e));
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(zis);

                if (skinUrlConnection != null) {
                    skinUrlConnection.disconnect();
                }
            }
        }
    }

    public SkinOrCapeHandler(String url, boolean isCape) {
        super(url);
        // TODO Refactor
        this.isCape = isCape;
    }

    public void sendHeaders(OutputStream os, String get) throws IOException {
        if (downloadAndCacheSkinOrCape(get)) {
            super.sendHeaders(os, get);
        } else {
            os.write(HTTP_404_HEADERS);
        }
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        if (downloadAndCacheSkinOrCape(get)) {
            final String username = pngPattern.matcher(get.replace(url, "")).replaceAll("");
            final String cacheName;

            if (isCape) {
                cacheName = username + ".cape";
            } else {
                cacheName = username;
            }

            os.write(imagesCache.get(cacheName));
        }
    }

    private boolean downloadAndCacheSkinOrCape(String get) {
        final String username = pngPattern.matcher(get.replace(url, "")).replaceAll("");
        final String cacheName;

        if (isCape) {
            cacheName = username + ".cape";
        } else {
            cacheName = username;
        }

        if (imagesCache.containsKey(cacheName)) {
            return true;
        }

        try {
            final byte[] bytes3 = downloadSkinOrCape(username, isCape);

            if (bytes3 != null) {
                final BufferedImage imgRaw = ImageIO.read(new ByteArrayInputStream(bytes3));
                final Image imgRawCorrectRes;

                if (imgRaw.getWidth() == SKIN_WIDTH) {
                    imgRawCorrectRes = imgRaw;
                } else {
                    // Scale any non-standard sized images (e.g. ClassiCube skins) to have a width of 64
                    imgRawCorrectRes = imgRaw.getScaledInstance(SKIN_WIDTH, -1, Image.SCALE_SMOOTH);
                }

                final BufferedImage imgFixed = new BufferedImage(SKIN_WIDTH, SKIN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                imgFixed.getGraphics().drawImage(imgRawCorrectRes, 0, 0, null);
                final ByteArrayOutputStream osImg = new ByteArrayOutputStream();
                ImageIO.write(imgFixed, "png", osImg);
                osImg.flush();
                final byte[] bytes = osImg.toByteArray();
                imagesCache.put(cacheName, bytes);
                return true;
            }

            LogWrapper.warning("Could not get skin for " + username);
        } catch (final Exception e) {
            LogWrapper.warning("Exception thrown while trying to get skin: " + ExceptionUtils.getStackTrace(e));
        }

        return false;
    }

    // TODO @Nullable?
    private static byte[] downloadSkinOrCape(String username, boolean cape) {
        final String fileNameEnd;

        if (cape) {
            fileNameEnd = ".cape.png";
        } else {
            fileNameEnd = ".png";
        }

        final File imageCache = new File(RetroEmulator.getInstance().getCacheDirectory(), username + fileNameEnd);
        byte[] skinFileBytes;
        final boolean isClassiCubeUser = RetroTweakInjectorTarget.connectedToClassicServer && !cape && username.endsWith("+");

        if (isClassiCubeUser) {
            // ClassiCube user
            skinFileBytes = getImageBytesFromClassiCube(username);
        } else {
            skinFileBytes = getImageBytesFromMojang(username, cape);
        }

        if (skinFileBytes != null) {
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(imageCache);
                fos.write(skinFileBytes);
            } catch (final Exception e) {
                LogWrapper.warning("Could not write skin to file: " + ExceptionUtils.getStackTrace(e));
            } finally {
                IOUtils.closeQuietly(fos);
            }

            return skinFileBytes;
        }

        skinFileBytes = getImageBytesFromFile(imageCache);

        if (skinFileBytes != null) {
            return skinFileBytes;
        }

        return isClassiCubeUser && RetroTweakInjectorTarget.showClassiCubeUserDefaultSkin && classiCubeDefaultChar.exists() ? getImageBytesFromFile(classiCubeDefaultChar) : null;
    }

    private static byte[] getImageBytesFromClassiCube(String username) {
        final String classiCubeUsername = username.substring(0, username.length() - 1);
        InputStream is = null;
        final URLConnection urlConnection;
        HttpURLConnection skinUrlConnection = null;

        try {
            urlConnection = new URL("https://classicube.s3.amazonaws.com/skin/" + classiCubeUsername + ".png").openConnection();

            if (urlConnection instanceof HttpURLConnection) {
                skinUrlConnection = (HttpURLConnection) urlConnection;
                skinUrlConnection.connect();
                final int respCode = skinUrlConnection.getResponseCode();

                if (respCode == HttpURLConnection.HTTP_OK) {
                    is = skinUrlConnection.getInputStream();
                    return IOUtils.toByteArray(is);
                }

                LogWrapper.warning("Response code " + respCode + " given while trying to get ClassiCube skin");
            } else {
                LogWrapper.severe("URL.openConnection() didn't return instance of HttpURLConnection");
            }
        } catch (final Exception e) {
            LogWrapper.warning("Exception thrown while trying to get ClassiCube skin: " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(is);

            if (skinUrlConnection != null) {
                skinUrlConnection.disconnect();
            }
        }

        return null;
    }

    private static byte[] getImageBytesFromMojang(String username, boolean cape) {
        final String uuid = NetworkUtil.getUUIDFromUsername(username);

        if (uuid != null) {
            InputStream is2 = null;
            InputStreamReader reader2 = null;
            InputStream is3 = null;

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

                if (imageLinkJSON != null) {
                    final String imageURL = imageLinkJSON.get("url").asString();
                    LogWrapper.fine(imageURL);
                    is3 = new URL(imageURL).openStream();
                    return IOUtils.toByteArray(is3);
                }

                if (cape) {
                    LogWrapper.warning("No cape found for username " + username);
                } else {
                    LogWrapper.warning("No skin found for username " + username);
                }
            } catch (final Exception e) {
                LogWrapper.warning("Issue downloading skin: " + ExceptionUtils.getStackTrace(e));
            } finally {
                IOUtils.closeQuietly(is2);
                IOUtils.closeQuietly(reader2);
                IOUtils.closeQuietly(is3);
            }
        } else {
            LogWrapper.warning("No UUID found for username " + username + ", could not download skin.");
        }

        return null;
    }

    private static byte[] getImageBytesFromFile(File imageCache) {
        if (imageCache.isFile()) {
            try {
                final BufferedImage localImage = ImageIO.read(imageCache);

                if (localImage != null) {
                    final ByteArrayOutputStream osImg = new ByteArrayOutputStream();
                    ImageIO.write(localImage, "png", osImg);
                    osImg.flush();
                    return osImg.toByteArray();
                }

                LogWrapper.warning("Could not use local skin?");
            } catch (final Exception e) {
                LogWrapper.warning("Issue using local skin: " + ExceptionUtils.getStackTrace(e));
            }
        }

        return null;
    }
}
