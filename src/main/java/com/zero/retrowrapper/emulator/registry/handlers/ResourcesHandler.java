package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.util.FileUtil;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

public final class ResourcesHandler extends EmulatorHandler {
    public enum ResourcesFormat {
        CLASSIC,
        AWS
    }

    private static final String[] CLASSIC_SOUNDS_LIST = {
        "sound/step/wood4.ogg",
        "sound/step/gravel3.ogg",
        "sound/step/wood2.ogg",
        "sound/step/gravel1.ogg",
        "sound/step/grass2.ogg",
        "sound/step/gravel4.ogg",
        "sound/step/grass4.ogg",
        "sound/step/gravel2.ogg",
        "sound/step/wood1.ogg",
        "sound/step/stone4.ogg",
        "sound/step/grass3.ogg",
        "sound/step/wood3.ogg",
        "sound/step/stone2.ogg",
        "sound/step/stone3.ogg",
        "sound/step/grass1.ogg",
        "sound/step/stone1.ogg",
        "sound/loops/ocean.ogg",
        "sound/loops/cave chimes.ogg",
        "sound/loops/waterfall.ogg",
        "sound/loops/birds screaming loop.ogg",
        "sound/random/wood click.ogg",
        "music/calm2.ogg",
        "music/calm3.ogg",
        "music/calm1.ogg"
    };

    private static final Map<String, String> CLASSIC_ALIAS_MAP;

    private static final int smallestSize = 16;
    private static final Pattern slashPattern = Pattern.compile("/");

    private final JsonObject jsonObjects;
    private final byte[] soundsList;
    private final String indexName;
    private final ResourcesFormat resourcesFormat;

    private final Pattern resourcesPattern;

    static {
        CLASSIC_ALIAS_MAP = new HashMap<String, String>();
        // Mojang renamed this file for reasons unknown
        CLASSIC_ALIAS_MAP.put("sound/random/wood click.ogg", "sound/random/wood_click.ogg");
    }

    public ResourcesHandler(String handle, ResourcesFormat resourcesFormat, String indexName, String indexURL) {
        super(handle);
        this.resourcesFormat = resourcesFormat;
        this.indexName = indexName;
        final JsonObject tempObjects = downloadSoundData(indexName, indexURL);
        jsonObjects = tempObjects != null ? tempObjects : Json.object();

        switch (resourcesFormat) {
        case CLASSIC:
            soundsList = jsonResourcesToClassic(jsonObjects).getBytes();
            break;

        case AWS:
            soundsList = jsonResourcesToAwsXml(jsonObjects).getBytes();
            break;

        default:
            LogWrapper.severe("According to all known laws of Javiation, there is no way that a switch should be able to reach this case.");
            soundsList = null;
            break;
        }

        resourcesPattern = Pattern.compile(handle, Pattern.LITERAL);
    }

    private static JsonObject downloadSoundData(String indexName, String indexURL) {
        final File jsonCached = new File(RetroEmulator.getInstance().getCacheDirectory() + File.separator + indexName, indexName + ".json");
        final File localLegacyJson = FileUtil.tryFindFirstFile(
                                         jsonCached,
                                         new File(Launch.minecraftHome + File.separator + "assets" + File.separator + "indexes" + File.separator + indexName + ".json"),
                                         new File(FileUtil.defaultMinecraftDirectory() + File.separator + "assets" + File.separator + "indexes" + File.separator + indexName + ".json")
                                     );

        if (localLegacyJson != null) {
            FileInputStream fis = null;

            try {
                fis = new FileInputStream(localLegacyJson);
                final String jsonString = IOUtils.toString(fis);
                final JsonValue json = Json.parse(jsonString);
                final JsonObject obj = json.asObject();
                final JsonObject resourceObjects = obj.get("objects").asObject();

                if (!jsonCached.isFile()) {
                    FileUtils.copyFile(localLegacyJson, jsonCached);
                }

                LogWrapper.info("Using local " + indexName + ".json.");
                return resourceObjects;
            } catch (final Exception e) {
                LogWrapper.warning("Exception loading local " + indexName + ".json: " + ExceptionUtils.getStackTrace(e));
            } finally {
                IOUtils.closeQuietly(fis);
            }
        } else {
            LogWrapper.warning("Could not find local " + indexName + ".json.");
        }

        InputStream is = null;

        try {
            is = new URL(indexURL).openStream();
            final String jsonString = IOUtils.toString(is);
            final JsonValue json = Json.parse(jsonString);
            final JsonObject obj = json.asObject();
            final JsonObject resourceObjects = obj.get("objects").asObject();
            // Cache file locally
            FileUtils.writeStringToFile(jsonCached, jsonString);
            LogWrapper.info("Using downloaded " + indexName + ".json.");
            return resourceObjects;
        } catch (final Exception e) {
            LogWrapper.warning("Exception downloading " + indexName + ".json: " + ExceptionUtils.getStackTrace(e) + "\nThe sound fix probably won't work.");

            if (e instanceof SSLHandshakeException) {
                LogWrapper.warning("The Java installation that Minecraft is running on " +
                                   "(" + SystemUtils.JAVA_VERSION + " (" + SystemUtils.JAVA_VENDOR + " " + SystemUtils.JAVA_VM_VERSION + ") located at " + SystemUtils.JAVA_HOME + ")" +
                                   " may not support modern versions of TLS/SSL (e.g. TLSv1.3). Consider using a newer Java installation to fix this.");
            }
        } finally {
            IOUtils.closeQuietly(is);
        }

        return null;
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        if (url.equals(get)) {
            os.write(soundsList);
        } else {
            final String name = resourcesPattern.matcher(get).replaceAll("");
            final byte[] bytes = getResourceByName(name);

            if ((bytes != null) && (bytes.length > smallestSize)) {
                os.write(bytes);
                LogWrapper.info("Successfully installed resource! " + name + " (" + bytes.length + ")");
            } else {
                LogWrapper.warning("Error installing resource " + name);
            }
        }
    }

    // TODO @Nullable?
    private byte[] getResourceByName(String res) throws IOException {
        if (resourcesFormat == ResourcesFormat.CLASSIC) {
            final String checkAlias = CLASSIC_ALIAS_MAP.get(res);

            if (checkAlias != null) {
                res = checkAlias;
            }
        }

        RetroEmulator.getInstance().getCacheDirectory().mkdir();
        final File resourceCache = new File(RetroEmulator.getInstance().getCacheDirectory() + File.separator + indexName, res);

        if (resourceCache.exists()) {
            FileInputStream fis = null;

            try {
                fis = new FileInputStream(resourceCache);
                return IOUtils.toByteArray(fis);
            } catch (final Exception e) {
                LogWrapper.warning("Error when reading local resource file " + resourceCache + ": " + ExceptionUtils.getStackTrace(e));
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }

        InputStream is = null;
        FileOutputStream resFile = null;

        try {
            if (jsonObjects == null) {
                throw new IllegalStateException("Could not download or find " + indexName + ".json!");
            }

            if (jsonObjects.get(res) == null) {
                throw new IllegalStateException("No hash for resource " + res + " in " + indexName + ".json!");
            }

            final String hash = jsonObjects.get(res).asObject().get("hash").asString();
            LogWrapper.fine(res + " " + hash);
            final File localLauncherObject = FileUtil.tryFindFirstFile(
                                                 new File(Launch.minecraftHome + File.separator + "assets" + File.separator + "objects" + File.separator + hash.substring(0, 2), hash),
                                                 new File(FileUtil.defaultMinecraftDirectory() + File.separator + "assets" + File.separator + "objects" + File.separator + hash.substring(0, 2), hash)
                                             );

            if ((localLauncherObject != null) && localLauncherObject.exists()) {
                FileInputStream fis = null;

                try {
                    LogWrapper.info("Using local launcher object " + localLauncherObject);
                    fis = new FileInputStream(localLauncherObject);
                    return IOUtils.toByteArray(fis);
                } catch (final Exception e) {
                    LogWrapper.warning("Error when reading local launcher object " + localLauncherObject + ": " + ExceptionUtils.getStackTrace(e));
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            } else {
                LogWrapper.info("No local launcher object for " + res);
            }

            final URL toDownload = new URL("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash);
            is = toDownload.openStream();
            final byte[] resourceBytes = IOUtils.toByteArray(is);

            if (resourceBytes.length > smallestSize) {
                resourceCache.getParentFile().mkdirs();

                try {
                    resFile = new FileOutputStream(resourceCache);
                    resFile.write(resourceBytes);
                    resFile.close();
                } catch (final Exception e) {
                    LogWrapper.warning("Resource " + res + " not written to cache due to exception: " + ExceptionUtils.getStackTrace(e));
                } finally {
                    IOUtils.closeQuietly(resFile);
                }

                return resourceBytes;
            }

            throw new IllegalStateException("The resource server for URL " + toDownload + " might be down");
        } catch (final Exception e) {
            LogWrapper.warning("Resource " + res + " not downloaded due to exception: " + ExceptionUtils.getStackTrace(e));
            final File backupFile = FileUtil.tryFindResourceFile(res);

            if (backupFile != null) {
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(backupFile);
                    LogWrapper.info("Using " + backupFile);
                    return IOUtils.toByteArray(fis);
                } catch (final Exception ee) {
                    LogWrapper.warning("Error when reading local resource file " + backupFile + ": " + ExceptionUtils.getStackTrace(ee));
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            }

            LogWrapper.warning("No backup location found for resource " + res);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(resFile);
        }
    }

    private static String jsonResourcesToClassic(JsonObject resources) {
        final StringBuilder builder = new StringBuilder();

        if (!resources.isEmpty()) {
            final Iterator<JsonObject.Member> libraryIterator = resources.iterator();
            final Map<String, ResourceEntry> entries = new HashMap<String, ResourceEntry>();

            while (libraryIterator.hasNext()) {
                final JsonObject.Member member = libraryIterator.next();
                String tempname = member.getName();

                for (final Entry<String, String> entry : CLASSIC_ALIAS_MAP.entrySet()) {
                    if (tempname.equals(entry.getValue())) {
                        tempname = entry.getKey();
                        break;
                    }
                }

                final String name = tempname;

                if (!ArrayUtils.contains(CLASSIC_SOUNDS_LIST, name)) {
                    continue;
                }

                if (!name.contains("/")) {
                    // Minecraft throws exceptions when it encounters top level resources for some reason
                    // TODO See if it's possible to fix this somehow?
                    LogWrapper.info("Skipping top level resource " + name + ", not handled by this version of Minecraft");
                    continue;
                }

                final JsonObject contents = member.getValue().asObject();
                final int size = contents.get("size").asInt();
                // Add this resource to the list of resources
                entries.put(name, new ResourceEntry(name, Integer.toString(size), null));
            }

            final String[] keysAsArray = entries.keySet().toArray(new String[0]);
            final int size = keysAsArray.length;
            final String[] names = new String[size];
            System.arraycopy(keysAsArray, 0, names, 0, size);
            Arrays.sort(names);

            // Each resource is listed as (resource name),(resource size),(Unix timestamp)
            // TODO Real timestamp
            for (final String name : names) {
                final ResourceEntry entry = entries.get(name);
                builder.append(entry.name);
                builder.append(',');
                builder.append(entry.size);
                builder.append(",1245702004000\n");
            }
        }

        return builder.toString();
    }

    // https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html
    private static String jsonResourcesToAwsXml(JsonObject resources) {
        final StringBuilder builder = new StringBuilder();
        // These keys are mostly not used by Minecraft
        builder.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
            "<Name>MinecraftResources</Name>" +
            "<Prefix></Prefix>" +
            "<Marker></Marker>" +
            "<MaxKeys>1000</MaxKeys>" +
            "<IsTruncated>false</IsTruncated>"
        );

        if (!resources.isEmpty()) {
            final Iterator<JsonObject.Member> libraryIterator = resources.iterator();
            final Map<String, ResourceEntry> entries = new HashMap<String, ResourceEntry>();

            while (libraryIterator.hasNext()) {
                final JsonObject.Member member = libraryIterator.next();
                final String name = member.getName();

                if (!name.contains("/")) {
                    // Minecraft throws exceptions when it encounters top level resources for some reason
                    // TODO See if it's possible to fix this somehow?
                    LogWrapper.info("Skipping top level resource " + name + ", not handled by this version of Minecraft");
                    continue;
                }

                final JsonObject contents = member.getValue().asObject();
                final int size = contents.get("size").asInt();
                // TODO This was a MD5 hash, but the current JSON format uses SHA1
                final String etag = contents.get("hash").asString();
                // Add this resource to the list of resources
                entries.put(name, new ResourceEntry(name, Integer.toString(size), etag));
                // Add the directories for this resource
                final String[] pathParts = slashPattern.split(name);

                for (int i = 0; i < (pathParts.length - 1); i++) {
                    final StringBuilder pathBuilder = new StringBuilder();

                    for (int j = 0; j <= i; j++) {
                        pathBuilder.append(pathParts[j]).append("/");
                    }

                    final String folderName = pathBuilder.toString();
                    entries.put(folderName, new ResourceEntry(folderName, "0", "d41d8cd98f00b204e9800998ecf8427e"));
                }
            }

            final String[] keysAsArray = entries.keySet().toArray(new String[0]);
            final int size = keysAsArray.length;
            final String[] names = new String[size];
            System.arraycopy(keysAsArray, 0, names, 0, size);
            Arrays.sort(names);

            // TODO LastModified
            for (final String name : names) {
                final ResourceEntry entry = entries.get(name);
                builder.append("<Contents><Key>");
                builder.append(entry.name);
                builder.append("</Key><ETag>");
                builder.append(entry.etag);
                builder.append("</ETag><Size>");
                builder.append(entry.size);
                builder.append("</Size><StorageClass>STANDARD</StorageClass></Contents>");
            }
        }

        builder.append("</ListBucketResult>");
        return builder.toString();
    }

    private static class ResourceEntry {
        public final String name;
        public final String size;
        public final String etag;

        ResourceEntry(String name, String size, String etag) {
            this.name = name;
            this.size = size;
            this.etag = etag;
        }
    }
}
