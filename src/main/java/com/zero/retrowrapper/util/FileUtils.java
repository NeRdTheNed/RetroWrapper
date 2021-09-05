package com.zero.retrowrapper.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import net.minecraft.launchwrapper.Launch;

public class FileUtils {
    // TODO Re-add?
    public static ByteBuffer loadIcon(File iconFile) throws IOException {
        final BufferedImage icon = ImageIO.read(iconFile);
        final int[] rgb = icon.getRGB(0, 0, icon.getWidth(), icon.getHeight(), (int[]) null, 0, icon.getWidth());
        final ByteBuffer buffer = ByteBuffer.allocate(4 * rgb.length);
        final int[] arg3 = rgb;
        final int arg4 = rgb.length;

        for (int arg5 = 0; arg5 < arg4; ++arg5) {
            final int color = arg3[arg5];
            buffer.putInt((color << 8) | ((color >> 24) & 255));
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

    // TODO @Nullable?
    public static File tryFindResourceFile(String file) {
        final File oldLocation = new File(Launch.assetsDir, file);
        final File virtualPreAssets = new File(Launch.minecraftHome, "assets/virtual/pre-1.6/" + file);
        final File virtualLegacyAssets = new File(Launch.minecraftHome, "assets/virtual/legacy/" + file);
        return tryFindFirstFile(oldLocation, virtualPreAssets, virtualLegacyAssets);
    }

    private FileUtils() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
