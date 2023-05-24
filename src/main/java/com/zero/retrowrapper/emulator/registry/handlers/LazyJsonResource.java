package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.zero.retrowrapper.emulator.RetroEmulator;
import com.zero.retrowrapper.util.FileUtil;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

class LazyJsonResource extends LazyInitializer<JsonObject> {
    private final String indexName;
    private final String indexDir;
    private final String indexURL;

    LazyJsonResource(final String indexName, final String indexDir, final String indexURL) {
        this.indexName = indexName;
        this.indexDir = indexDir;
        this.indexURL = indexURL;
    }

    @Override
    protected JsonObject initialize() throws ConcurrentException {
        final JsonObject tempObjects = downloadSoundData(indexName, indexDir, indexURL);
        return tempObjects != null ? tempObjects : Json.object();
    }

    private static JsonObject downloadSoundData(String indexName, String indexDir, String indexURL) {
        final File jsonCached = new File(RetroEmulator.getInstance().getCacheDirectory() + File.separator + "indexes" + File.separator + indexDir + File.separator + indexName, indexName + ".json");
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
}
