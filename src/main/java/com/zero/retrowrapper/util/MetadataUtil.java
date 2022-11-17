package com.zero.retrowrapper.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public final class MetadataUtil {
    public static final List<String> INSTALLER_SPLASHES = getSplashes();
    public static final String VERSION = getVersion();

    private static List<String> getSplashes() {
        try {
            return IOUtils.readLines(ClassLoader.getSystemResourceAsStream("com/zero/retrowrapper/retrowrapperInstallerSplashes.txt"), "UTF-8");
        } catch (final IOException e) {
            final ArrayList<String> missingno = new ArrayList<String>();
            missingno.add("missingno");
            return missingno;
        }
    }

    private static String getVersion() {
        try {
            return IOUtils.toString(ClassLoader.getSystemResourceAsStream("com/zero/retrowrapper/retrowrapperVersion.txt"), "UTF-8");
        } catch (final IOException e) {
            return "0.0.0-SNAPSHOT+missingno";
        }
    }

    private MetadataUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
