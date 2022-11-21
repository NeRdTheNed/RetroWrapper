package com.zero.retrowrapper.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public final class MetadataUtil {
    public static final List<String> INSTALLER_SPLASHES;
    public static final String VERSION;
    public static final String TAG;
    public static final boolean IS_RELEASE;

    static {
        List<String> tempSplash;

        try {
            tempSplash = IOUtils.readLines(ClassLoader.getSystemResourceAsStream("com/zero/retrowrapper/retrowrapperInstallerSplashes.txt"), "UTF-8");
        } catch (final Exception e) {
            final ArrayList<String> missingno = new ArrayList<String>();
            missingno.add("missingno");
            tempSplash = missingno;
        }

        INSTALLER_SPLASHES = tempSplash;
        String tempVer;
        String tempTag;

        try {
            final List<String> versionLines = IOUtils.readLines(ClassLoader.getSystemResourceAsStream("com/zero/retrowrapper/retrowrapperVersion.txt"), "UTF-8");
            tempVer = versionLines.get(0);
            tempTag = versionLines.get(1);
        } catch (final Exception e) {
            tempVer = "0.0.0-SNAPSHOT+missingno";
            tempTag = "missingno";
        }

        VERSION = tempVer;
        TAG = tempTag;
        IS_RELEASE = !VERSION.contains("SNAPSHOT");
    }

    private MetadataUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
