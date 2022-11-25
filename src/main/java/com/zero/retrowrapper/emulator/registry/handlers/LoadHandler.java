package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;

import net.minecraft.launchwrapper.LogWrapper;

public final class LoadHandler extends EmulatorHandler {
    private static final Pattern loadPattern = Pattern.compile("/level/load.html?id=", Pattern.LITERAL);
    private static final Pattern andPattern = Pattern.compile("&");

    public LoadHandler() {
        super("/level/load.html?id=");
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        final String id = andPattern.split(loadPattern.matcher(get).replaceAll(""))[0];
        FileInputStream fis = null;
        DataOutputStream dis = null;

        try {
            fis = new FileInputStream(new File(RetroEmulator.getInstance().getMapsDirectory(), "map" + id + ".mclevel"));
            final byte[] bytes = IOUtils.toByteArray(fis);
            dis = new DataOutputStream(os);
            dis.writeUTF("ok");
            dis.write(bytes);
        } catch (final Exception e) {
            // TODO Better error handling
            LogWrapper.warning("Problem loading level " + id + ": " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(dis);
        }
    }
}
