package com.zero.retrowrapper.emulator.registry.handlers;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.injector.RetroTweakInjectorTarget;
import com.zero.retrowrapper.util.NetworkUtil;

import net.minecraft.launchwrapper.LogWrapper;

public final class SkinOrCapeHandler extends EmulatorHandler {
    private static final Pattern pngPattern = Pattern.compile(".png", Pattern.LITERAL);
    private final Map<String, byte[]> imagesCache = new ConcurrentHashMap<String, byte[]>();
    // TODO Refactor
    private final boolean isCape;

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

    @Override
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
            os.write(imagesCache.get(isCape ? username + ".cape" : username));
        }
    }

    private boolean downloadAndCacheSkinOrCape(String get) {
        final String username = pngPattern.matcher(get.replace(url, "")).replaceAll("");
        final String cacheName = isCape ? username + ".cape" : username;

        if (imagesCache.containsKey(cacheName)) {
            return true;
        }

        try {
            final byte[] imageBytes = downloadSkinOrCape(username, isCape);

            if (imageBytes != null) {
                final BufferedImage imgRaw = ImageIO.read(new ByteArrayInputStream(imageBytes));
                final byte[] bytes;

                if ((imgRaw.getWidth() == SKIN_WIDTH) && (imgRaw.getHeight() == SKIN_HEIGHT)) {
                    bytes = imageBytes;
                } else {
                    final BufferedImage imgRawCorrectRes;

                    if (imgRaw.getWidth() == SKIN_WIDTH) {
                        imgRawCorrectRes = imgRaw;
                    } else {
                        // Scale any non-standard sized images (e.g. ClassiCube skins) to have a width of 64
                        final double scale = (double) SKIN_WIDTH / (double) imgRaw.getWidth();
                        imgRawCorrectRes = new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), AffineTransformOp.TYPE_BICUBIC).filter(imgRaw, new BufferedImage(SKIN_WIDTH, (int)(imgRaw.getHeight() * scale), BufferedImage.TYPE_INT_ARGB));
                    }

                    final BufferedImage imgFixed = new BufferedImage(SKIN_WIDTH, SKIN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                    imgFixed.getGraphics().drawImage(imgRawCorrectRes, 0, 0, null);
                    final ByteArrayOutputStream osImg = new ByteArrayOutputStream();
                    ImageIO.write(imgFixed, "png", osImg);
                    osImg.flush();
                    bytes = osImg.toByteArray();
                }

                imagesCache.put(cacheName, bytes);
                return true;
            }

            LogWrapper.warning("Could not get " + (isCape ? "cape" : "skin") + " for " + username);
        } catch (final Exception e) {
            LogWrapper.warning("Exception thrown while trying to get " + (isCape ? "cape" : "skin") + ": " + ExceptionUtils.getStackTrace(e));
        }

        return false;
    }

    // TODO @Nullable?
    private static byte[] downloadSkinOrCape(String username, boolean cape) {
        final File imageCache = new File(RetroEmulator.getInstance().getCacheDirectory(), username + (cape ? ".cape.png" : ".png"));
        final boolean isClassiCubeUser = RetroTweakInjectorTarget.connectedToClassicServer && !cape && username.endsWith("+");
        byte[] skinFileBytes = isClassiCubeUser ? getImageBytesFromClassiCube(username) : getImageBytesFromMojang(username, cape);

        if (skinFileBytes != null) {
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(imageCache);
                fos.write(skinFileBytes);
            } catch (final Exception e) {
                LogWrapper.warning("Could not write " + (cape ? "cape" : "skin") + " to file: " + ExceptionUtils.getStackTrace(e));
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
            InputStream profileStream = null;
            InputStreamReader profileStreamReader = null;
            InputStream imageStream = null;
            HttpURLConnection httpConnection = null;

            try {
                final URLConnection profileConnection = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).openConnection();

                if (profileConnection instanceof HttpURLConnection) {
                    httpConnection = (HttpURLConnection) profileConnection;
                }

                profileConnection.connect();

                if ((httpConnection != null) && ((httpConnection.getResponseCode() / 100) != 2)) {
                    LogWrapper.warning("Error getting profile for skin information: " + NetworkUtil.getResponseAfterErrorAndClose(httpConnection));
                    return null;
                }

                profileStream = profileConnection.getInputStream();
                profileStreamReader = new InputStreamReader(profileStream);
                final JsonObject profile = Json.parse(profileStreamReader).asObject();
                final Iterable<JsonValue> properties = profile.get("properties").asArray();
                String base64 = "";

                for (final JsonValue property : properties) {
                    final JsonObject propertyObj = property.asObject();

                    if ("textures".equalsIgnoreCase(propertyObj.get("name").asString())) {
                        base64 = propertyObj.get("value").asString();
                    }
                }

                final JsonObject textures1 = Json.parse(new String(Base64.decodeBase64(base64))).asObject();
                final JsonObject textures = textures1.get("textures").asObject();
                final JsonValue capeOrSkin = textures.get(cape ? "CAPE" : "SKIN");

                if (capeOrSkin != null) {
                    final JsonObject imageLinkJSON = capeOrSkin.asObject();

                    if (imageLinkJSON != null) {
                        final String imageURL = imageLinkJSON.get("url").asString();
                        LogWrapper.fine(imageURL);
                        imageStream = new URL(imageURL).openStream();
                        return IOUtils.toByteArray(imageStream);
                    }
                }

                LogWrapper.warning("No " + (cape ? "cape" : "skin") + " found for username " + username);
            } catch (final Exception e) {
                LogWrapper.warning("Issue downloading " + (cape ? "cape" : "skin") + ": " + ExceptionUtils.getStackTrace(e));
            } finally {
                IOUtils.closeQuietly(profileStream);
                IOUtils.closeQuietly(profileStreamReader);
                IOUtils.closeQuietly(imageStream);

                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
            }
        } else {
            LogWrapper.warning("No UUID found for username " + username + ", could not download " + (cape ? "cape" : "skin") + ".");
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

                LogWrapper.warning("Could not use local skin or cape?");
            } catch (final Exception e) {
                LogWrapper.warning("Issue using local skin or cape: " + ExceptionUtils.getStackTrace(e));
            }
        }

        return null;
    }
}
