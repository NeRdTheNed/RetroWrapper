package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.util.FileUtil;

import net.minecraft.launchwrapper.Launch;

public final class ResourcesHandler extends EmulatorHandler {
    private static final byte[] OLD_SOUNDS_LIST =
        ("\nsound/step/wood4.ogg,0,1245702004000\n"
         + "sound/step/gravel3.ogg,0,1245702004000\n"
         + "sound/step/wood2.ogg,0,1245702004000\n"
         + "sound/step/gravel1.ogg,0,1245702004000\n"
         + "sound/step/grass2.ogg,0,1245702004000\n"
         + "sound/step/gravel4.ogg,0,1245702004000\n"
         + "sound/step/grass4.ogg,0,1245702004000\n"
         + "sound/step/gravel2.ogg,0,1245702004000\n"
         + "sound/step/wood1.ogg,0,1245702004000\n"
         + "sound/step/stone4.ogg,0,1245702004000\n"
         + "sound/step/grass3.ogg,0,1245702004000\n"
         + "sound/step/wood3.ogg,0,1245702004000\n"
         + "sound/step/stone2.ogg,0,1245702004000\n"
         + "sound/step/stone3.ogg,0,1245702004000\n"
         + "sound/step/grass1.ogg,0,1245702004000\n"
         + "sound/step/stone1.ogg,0,1245702004000\n"
         + "music/calm2.ogg,0,1245702004000\n"
         + "music/calm3.ogg,0,1245702004000\n"
         + "music/calm1.ogg,0,1245702004000\n").getBytes();

    private static final int smallestSize = 16;

    private JsonObject jsonObjects;

    public ResourcesHandler(String handle) {
        super(handle);

        try {
            downloadSoundData();
        } catch (final Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
        }
    }

    private void downloadSoundData() {
        Scanner sc = null;

        try {
            sc = new Scanner(new URL("https://launchermeta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json").openStream()).useDelimiter("\\A");
            final JsonValue json = Json.parse(sc.next());
            final JsonObject obj = json.asObject();
            jsonObjects = obj.get("objects").asObject();
        } catch (final Exception e) {
            System.out.println("Exception downloading legacy.json: " + ExceptionUtils.getStackTrace(e));

            if (e instanceof SSLHandshakeException) {
                System.out.println("The Java installation that Minecraft is running on " +
                                   "(" + SystemUtils.JAVA_VERSION + " (" + SystemUtils.JAVA_VENDOR + " " + SystemUtils.JAVA_VM_VERSION + ") located at " + SystemUtils.JAVA_HOME + ")" +
                                   " may not support modern versions of TLS/SSL (e.g. TLSv1.3). Consider using a newer Java installation to fix this.");
            }

            final File backupFile = FileUtil.tryFindFirstFile(
                                        new File(Launch.minecraftHome + "/assets/indexes/legacy.json"), new File(Launch.minecraftHome + "/assets/indexes/pre-1.6.json"),
                                        new File(FileUtil.defaultMinecraftDirectory() + "/assets/indexes/legacy.json"), new File(FileUtil.defaultMinecraftDirectory() + "/assets/indexes/pre-1.6.json")
                                    );

            if (backupFile != null) {
                Scanner sc2 = null;

                try {
                    sc2 = new Scanner(backupFile).useDelimiter("\\A");
                    final JsonValue json = Json.parse(sc2.next());
                    final JsonObject obj = json.asObject();
                    jsonObjects = obj.get("objects").asObject();
                    System.out.println("Using local legacy.json.");
                } catch (final Exception ee) {
                    System.out.println("Exception loading local legacy.json: " + ExceptionUtils.getStackTrace(ee) + "\nThe sound fix probably won't work.");
                } finally {
                    if (sc2 != null) {
                        sc2.close();
                    }
                }
            } else {
                System.out.println("Could not find local legacy.json.\nThe sound fix probably won't work.");
            }
        } finally {
            if (sc != null) {
                sc.close();
            }
        }
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        if ("/resources/".equals(get)) {
            os.write(OLD_SOUNDS_LIST);
        } else if ("/MinecraftResources/".equals(get)) {
            // This URL still exists!
            try {
                final URL resourceURL = new URL("http://s3.amazonaws.com" + get);
                final InputStream is = resourceURL.openStream();
                final byte[] asBytes = IOUtils.toByteArray(is);
                os.write(asBytes);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            final String name = get.replace("/resources/", "").replace("/MinecraftResources/", "");
            final byte[] bytes = getResourceByName(name);

            if ((bytes != null) && (bytes.length > smallestSize)) {
                os.write(bytes);
                System.out.println("Succesfully installed resource! " + name + " (" + bytes.length + ")");
            } else {
                System.out.println("Error installing resource " + name);
            }
        }
    }

    // TODO @Nullable?
    private byte[] getResourceByName(String res) throws IOException {
        RetroEmulator.getInstance().getCacheDirectory().mkdir();
        final File resourceCache = new File(RetroEmulator.getInstance().getCacheDirectory(), res);

        if (resourceCache.exists()) {
            FileInputStream fis = null;

            try {
                fis = new FileInputStream(resourceCache);
                return IOUtils.toByteArray(fis);
            } catch (final Exception e) {
                e.printStackTrace();
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

        try {
            if (jsonObjects == null) {
                throw new IllegalStateException("Could not download or find legacy.json!");
            }

            if (jsonObjects.get(res) == null) {
                throw new IllegalStateException("No hash for resource " + res + " in legacy.json!");
            }

            final String hash = jsonObjects.get(res).asObject().get("hash").asString();
            System.out.println(res + " " + hash);
            final URL toDownload = new URL("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash);
            final InputStream is = toDownload.openStream();
            final byte[] resourceBytes = IOUtils.toByteArray(is);

            if (resourceBytes.length > smallestSize) {
                new File(resourceCache.getParent()).mkdirs();
                FileOutputStream fos = null;

                try {
                    fos = new FileOutputStream(resourceCache);
                    fos.write(resourceBytes);
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

                return resourceBytes;
            }

            throw new IllegalStateException("The resource server for URL " + toDownload + " might be down");
        } catch (final Exception e) {
            System.out.println("Resource " + res + " not downloaded due to exception: " + ExceptionUtils.getStackTrace(e));
            e.printStackTrace();
            final File backupFile = FileUtil.tryFindResourceFile(res);

            if (backupFile != null) {
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(backupFile);
                    System.out.println("Using " + backupFile);
                    return IOUtils.toByteArray(fis);
                } catch (final Exception ee) {
                    ee.printStackTrace();
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

            System.out.println("No backup location found for resource " + res);
            return null;
        }
    }
}
