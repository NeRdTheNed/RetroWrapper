package com.zero.retrowrapper.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import net.minecraft.launchwrapper.Launch;

public final class FileUtil {
    public static String defaultMinecraftDirectory() {
        if (SystemUtils.IS_OS_WINDOWS) { // windows uses the %appdata%/.minecraft structure
            return System.getenv("AppData") + File.separator + ".minecraft";
        }

        if (SystemUtils.IS_OS_MAC) { // mac os uses %user%/Library/Library/Application Support/minecraft
            return System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft";
        }

        return System.getProperty("user.home") + File.separator + ".minecraft";
    }

    // TODO Re-add?
    public static ByteBuffer loadIcon(File iconFile) throws IOException {
        final BufferedImage icon = ImageIO.read(iconFile);
        final int[] rgb = icon.getRGB(0, 0, icon.getWidth(), icon.getHeight(), null, 0, icon.getWidth());
        final ByteBuffer buffer = ByteBuffer.allocate(4 * rgb.length);

        for (final int color : rgb) {
            buffer.putInt((color << 8) | (color >>> 24));
        }

        buffer.flip();
        return buffer;
    }

    // TODO @Nullable?
    public static File tryFindFirstFile(File... files) {
        for (final File file : files) {
            if (file.exists() && file.isFile()) {
                return file;
            }
        }

        return null;
    }

    public static List<File> findFiles(File dir) {
        final List<File> files = new ArrayList<File>();

        for (final File file : dir.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(findFiles(file));
            } else {
                files.add(file);
            }
        }

        return files;
    }

    public static List<File> findFiles(File dir, String... exts) {
        if ((exts == null) || (exts.length == 0)) {
            return findFiles(dir);
        }

        final List<File> files = new ArrayList<File>();
        final int length = exts.length;
        final String[] lowercaseExts = new String[length];

        for (int i = 0; i < length; ++i) {
            lowercaseExts[i] = exts[i].toLowerCase(Locale.ENGLISH);
        }

        for (final File file : findFiles(dir)) {
            final String fileNameCaseIns = file.getName().toLowerCase(Locale.ENGLISH);

            for (final String ext : lowercaseExts) {
                if (fileNameCaseIns.endsWith(ext)) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    // TODO @Nullable?
    public static File tryFindResourceFile(String file) {
        final File oldLocation = new File(Launch.assetsDir, file);
        final File resLocation = new File(Launch.minecraftHome, "resources/" + file);
        final File virtualLegacyAssets = new File(Launch.minecraftHome, "assets/virtual/legacy/" + file);
        final File virtualPreAssets = new File(Launch.minecraftHome, "assets/virtual/pre-1.6/" + file);
        final File defResLocation = new File(defaultMinecraftDirectory(), "resources/" + file);
        final File defVirtualLegacyAssets = new File(defaultMinecraftDirectory(), "assets/virtual/legacy/" + file);
        final File defVirtualPreAssets = new File(defaultMinecraftDirectory(), "assets/virtual/pre-1.6/" + file);
        return tryFindFirstFile(oldLocation, resLocation, virtualLegacyAssets, virtualPreAssets, defResLocation, defVirtualLegacyAssets, defVirtualPreAssets);
    }

    public static JsonValue readFileAsJsonOrNull(File file) {
        JsonValue toReturn = null;
        FileReader reader = null;

        try {
            reader = new FileReader(file);
            toReturn = Json.parse(reader);
        } catch (final Exception ignored) {
            // Ignored
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return toReturn;
    }

    private FileUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
