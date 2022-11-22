package com.zero.retrowrapper.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public final class MetadataUtil {
    public static final List<String> INSTALLER_SPLASHES;
    public static final String VERSION;
    public static final String TAG;
    public static final boolean IS_RELEASE;

    private static final int LESS_THAN = -1;
    private static final int SAME = 0;
    private static final int GREATER_THAN = 1;

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
        IS_RELEASE = !isVersionSnapshot(VERSION);
    }

    public static boolean isVersionSnapshot(final String version) {
        return version.contains("SNAPSHOT");
    }

    public static int compareSemver(String ver1, String ver2) throws NumberFormatException {
        // Ignore build metadata
        ver1 = ver1.split("\\+")[0];
        ver2 = ver2.split("\\+")[0];
        int majorVersion1 = 0;
        int minorVersion1 = 0;
        int patchVersion1 = 0;
        final boolean snapshot1 = isVersionSnapshot(ver1);
        final String[] versionSplit1 = ver1.split("-SNAPSHOT")[0].split("\\.");

        if (versionSplit1.length > 0) {
            majorVersion1 = Integer.parseInt(versionSplit1[0]);

            if (versionSplit1.length > 1) {
                minorVersion1 = Integer.parseInt(versionSplit1[1]);

                if (versionSplit1.length > 2) {
                    patchVersion1 = Integer.parseInt(versionSplit1[2]);
                }
            }
        }

        int majorVersion2 = 0;
        int minorVersion2 = 0;
        int patchVersion2 = 0;
        final boolean snapshot2 = isVersionSnapshot(ver2);
        final String[] versionSplit2 = ver1.split("-SNAPSHOT")[0].split("\\.");

        if (versionSplit2.length > 0) {
            majorVersion2 = Integer.parseInt(versionSplit2[0]);

            if (versionSplit2.length > 1) {
                minorVersion2 = Integer.parseInt(versionSplit2[1]);

                if (versionSplit2.length > 2) {
                    patchVersion2 = Integer.parseInt(versionSplit2[2]);
                }
            }
        }

        final int returnVal;

        if (majorVersion1 > majorVersion2) {
            returnVal = GREATER_THAN;
        } else if (majorVersion1 == majorVersion2) {
            if (minorVersion1 > minorVersion2) {
                returnVal = GREATER_THAN;
            } else if (minorVersion1 == minorVersion2) {
                if (patchVersion1 > patchVersion2) {
                    returnVal = GREATER_THAN;
                } else if (patchVersion1 == patchVersion2) {
                    if (!snapshot1 && snapshot2) {
                        returnVal = GREATER_THAN;
                    } else if (snapshot1 == snapshot2) {
                        returnVal = SAME;
                    } else {
                        returnVal = LESS_THAN;
                    }
                } else {
                    returnVal = LESS_THAN;
                }
            } else {
                returnVal = LESS_THAN;
            }
        } else {
            returnVal = LESS_THAN;
        }

        return returnVal;
    }

    private MetadataUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
