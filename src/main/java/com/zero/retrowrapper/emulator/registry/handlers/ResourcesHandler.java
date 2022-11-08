package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

public final class ResourcesHandler extends EmulatorHandler {
    private static final byte[] SOUNDS_LIST =
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

    private JsonObject jsonObjects;

    public ResourcesHandler() {
        super("/resources/");
        downloadSoundData();
    }

    private void downloadSoundData() {
        try (Scanner sc = new Scanner(new URL("https://launchermeta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json").openStream()).useDelimiter("\\A")) {
            final JsonValue json = Json.parse(sc.next());
            final JsonObject obj = json.asObject();
            jsonObjects = obj.get("objects").asObject();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        if ("/resources/".equals(get)) {
            os.write(SOUNDS_LIST);
        } else {
            final String name = get.replace("/resources/", "");
            final byte[] bytes = getResourceByName(name);

            if (bytes != null) {
                os.write(bytes);
                System.out.println("Succesfully installed resource! " + name + " (" + bytes.length + ")");
            }
        }
    }

    // TODO @Nullable?
    private byte[] getResourceByName(String res) throws IOException {
        final File resourceCache = new File(RetroEmulator.getInstance().getCacheDirectory(), res);

        if (resourceCache.exists()) {
            try (FileInputStream fis = new FileInputStream(resourceCache)) {
                return IOUtils.toByteArray(fis);
            }
        }

        try {
            if (jsonObjects.get(res) == null) {
                return null;
            }

            final String hash = jsonObjects.get(res).asObject().get("hash").asString();
            System.out.println(hash);
            final InputStream is = new URL("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash).openStream();
            final byte[] resourceBytes = IOUtils.toByteArray(is);
            new File(resourceCache.getParent()).mkdirs();

            try (FileOutputStream fos = new FileOutputStream(resourceCache)) {
                fos.write(resourceBytes);
            }

            return resourceBytes;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
