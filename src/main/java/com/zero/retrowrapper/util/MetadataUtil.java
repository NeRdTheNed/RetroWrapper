package com.zero.retrowrapper.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public final class MetadataUtil {
    public static final List<String> INSTALLER_SPLASHES;
    public static final String VERSION;
    public static final String TAG;
    public static final String RELEASE_URL;
    public static final boolean IS_RELEASE;

    private static final int LESS_THAN = -1;
    private static final int SAME = 0;
    private static final int GREATER_THAN = 1;
    private static final Pattern plusPattern = Pattern.compile("\\+");
    private static final Pattern snapshotPattern = Pattern.compile("-SNAPSHOT");
    private static final Pattern dotPattern = Pattern.compile("\\.");
    private static final Pattern colonPattern = Pattern.compile(":");

    static {
        List<String> tempSplash;
        InputStream splashesStream = null;

        try {
            splashesStream = ClassLoader.getSystemResourceAsStream("com/zero/retrowrapper/retrowrapperInstallerSplashes.txt");
            tempSplash = IOUtils.readLines(splashesStream, "UTF-8");
        } catch (final Exception e) {
            final List<String> missingno = new ArrayList<String>();
            missingno.add("missingno");
            tempSplash = missingno;
        } finally {
            IOUtils.closeQuietly(splashesStream);
        }

        INSTALLER_SPLASHES = tempSplash;
        String tempVer;
        String tempTag;
        InputStream versionStream = null;

        try {
            versionStream = ClassLoader.getSystemResourceAsStream("com/zero/retrowrapper/retrowrapperVersion.txt");
            final List<String> versionLines = IOUtils.readLines(versionStream, "UTF-8");
            tempVer = versionLines.get(0);
            tempTag = versionLines.get(1);
        } catch (final Exception e) {
            tempVer = "0.0.0-SNAPSHOT+missingno";
            tempTag = "missingno";
        } finally {
            IOUtils.closeQuietly(versionStream);
        }

        VERSION = tempVer;
        TAG = tempTag;
        IS_RELEASE = !isVersionSnapshot(VERSION);
        RELEASE_URL = IS_RELEASE ?
                      "https://github.com/NeRdTheNed/RetroWrapper/releases/download/" + TAG + "/RetroWrapper-" + VERSION + ".jar"
                      : null;
    }

    public static JsonObject[] getLWJGLLibraries(String jsonFile) {
        JsonObject[] lwjglLibraries;
        final List<JsonObject> tempLwjglLibs = new ArrayList<JsonObject>();
        InputStream lwjglLibsStream = null;

        try {
            lwjglLibsStream = ClassLoader.getSystemResourceAsStream(jsonFile);
            final JsonArray lwjgl = Json.parse(IOUtils.toString(lwjglLibsStream)).asObject().get("libraries").asArray();

            for (final JsonValue jsonValue : lwjgl) {
                final JsonObject library = jsonValue.asObject();
                tempLwjglLibs.add(library);
            }

            lwjglLibraries = tempLwjglLibs.toArray(new JsonObject[0]);
        } catch (final Exception e) {
            lwjglLibraries = new JsonObject[0];
        } finally {
            IOUtils.closeQuietly(lwjglLibsStream);
        }

        return lwjglLibraries;
    }

    public static String[] getLWJGLLibraryNames(JsonObject[] lwjglLibraries) {
        final Set<String> names = new HashSet<String>();

        for (final JsonObject lib : lwjglLibraries) {
            final String nameWithVersion = lib.get("name").asString();
            final String name = nameWithVersion.substring(0, nameWithVersion.lastIndexOf(':'));
            names.add(name);
        }

        return names.toArray(new String[0]);
    }

    public static boolean isVersionSnapshot(final String version) {
        return version.contains("SNAPSHOT");
    }

    public static int compareSemver(String ver1, String ver2) throws NumberFormatException {
        // Ignore build metadata
        ver1 = plusPattern.split(ver1)[0];
        ver2 = plusPattern.split(ver2)[0];
        int majorVersion1 = 0;
        int minorVersion1 = 0;
        int patchVersion1 = 0;
        final boolean snapshot1 = isVersionSnapshot(ver1);
        final String[] versionSplit1 = dotPattern.split(snapshotPattern.split(ver1)[0]);

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
        final String[] versionSplit2 = dotPattern.split(snapshotPattern.split(ver2)[0]);

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

    public static String getLibraryVersionFromMojangVersion(JsonObject versionJson, final CharSequence libraryName) {
        String toReturn = null;
        final JsonArray libraries = versionJson.get("libraries").asArray();

        for (final JsonValue jsonValue : libraries) {
            final JsonObject library = jsonValue.asObject();
            final String libName = library.get("name").asString();

            if (libName.contains(libraryName)) {
                final String[] libNameSplit = colonPattern.split(libName);
                toReturn = libNameSplit[libNameSplit.length - 1];
            }
        }

        return toReturn;
    }

    public static JsonObject createMojangLibrary(String name) {
        return Json.object().add("name", name);
    }

    public static JsonObject createMojangLibrary(String name, String path, String url, String sha1, long size) {
        return createMojangLibraryWithArtifact(name, createMojangLibraryDownloadsArtifact(path, url, sha1, size));
    }

    public static JsonObject createMojangLibraryWithArtifact(String name, JsonObject artifact) {
        return createMojangLibraryWithDownloads(name, createMojangLibraryDownloadsFromArtifact(artifact));
    }

    public static JsonObject createMojangLibraryWithDownloads(String name, JsonObject downloads) {
        return createMojangLibrary(name).add("downloads", downloads);
    }

    public static JsonObject createMojangLibraryDownloadsFromArtifact(JsonObject artifact) {
        return Json.object().add("artifact", artifact);
    }

    public static JsonObject createMojangLibraryDownloadsArtifact(String path, String url, String sha1, long size) {
        final JsonObject artifact = Json.object();

        if (path != null) {
            artifact.add("path", path);
        }

        if (url != null) {
            artifact.add("url", url);
        }

        if (sha1 != null) {
            artifact.add("sha1", sha1);
        }

        if (size > 0) {
            artifact.add("size", size);
        }

        return artifact;
    }

    private MetadataUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }
}
