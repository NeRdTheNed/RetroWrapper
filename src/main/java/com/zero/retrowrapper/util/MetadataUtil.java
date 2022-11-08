package com.zero.retrowrapper.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public final class MetadataUtil {
    public static final List<String> INSTALLER_SPLASHES = getSplashes();
    public static final String VERSION = getVersion();

    private static List<String> getSplashes() {
        try {
            return IOUtils.readLines(ClassLoader.getSystemResourceAsStream("com/zero/retrowrapper/retrowrapperInstallerSplashes.txt"), Charset.defaultCharset());
        } catch (final IOException e) {
            final ArrayList<String> missingno = new ArrayList<>();
            missingno.add("missingno");
            return missingno;
        }
    }

    private static String getVersion() {
        try {
            return IOUtils.toString(ClassLoader.getSystemResourceAsStream("com/zero/retrowrapper/retrowrapperVersion.txt"), Charset.defaultCharset());
        } catch (final IOException e) {
            return "0.0.0-SNAPSHOT+Missingno";
        }
    }

    private MetadataUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
