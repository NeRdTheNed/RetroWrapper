package com.zero.retrowrapper.injector;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;

/** Based on the patch used by the Fabric loader */
public class FML125ClassLoader extends URLClassLoader {
    private URL[] localUrls;

    public FML125ClassLoader() {
        super(new URL[0], Launch.classLoader);
        localUrls = new URL[0];
    }

    @Override
    protected void addURL(URL url) {
        Launch.classLoader.addURL(url);
        final URL[] newLocalUrls = new URL[localUrls.length + 1];
        System.arraycopy(localUrls, 0, newLocalUrls, 0, localUrls.length);
        newLocalUrls[localUrls.length] = url;
        localUrls = newLocalUrls;
    }

    @Override
    public URL[] getURLs() {
        return localUrls;
    }

    @Override
    public URL findResource(final String name) {
        return getParent().getResource(name);
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        return getParent().getResources(name);
    }

    public void addFile(File modFile) throws MalformedURLException {
        try {
            addURL(modFile.toURI().toURL());
        } catch (final MalformedURLException e) {
            LogWrapper.warning(FML125Injector.FML_PATCH + "Issue when getting URL for mod file: " + ExceptionUtils.getStackTrace(e));
            throw new MalformedURLException(e.getMessage());
        }
    }

    public File getParentSource() {
        final File[] files = getParentSources();

        if ((files == null) || (files.length == 0)) {
            LogWrapper.warning(FML125Injector.FML_PATCH + "No parent source files!");
            return null;
        }

        for (final File file : files) {
            if ("minecraft.jar".equals(file.getName())) {
                return file;
            }
        }

        LogWrapper.warning(FML125Injector.FML_PATCH + "Could not find minecraft.jar in loader, returning " + files[0] + " instead");
        return files[0];
    }

    public File[] getParentSources() {
        final ArrayList<File> files = new ArrayList<File>();

        for (final URL url : Launch.classLoader.getSources()) {
            try {
                final File file = new File(url.toURI());

                if ("minecraft.jar".equals(file.getName())) {
                    files.add(0, file);
                } else {
                    files.add(file);
                }
            } catch (final URISyntaxException e) {
                LogWrapper.warning(FML125Injector.FML_PATCH + "Issue when getting URL for source file: " + ExceptionUtils.getStackTrace(e));
            }
        }

        return files.toArray(new File[0]);
    }

}
