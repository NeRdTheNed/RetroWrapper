package com.zero.retrowrapper.injector;

import java.applet.Applet;
import java.applet.AppletStub;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.launchwrapper.LogWrapper;

public final class LauncherFake extends Applet implements AppletStub {
    private static final long serialVersionUID = 1L;

    private final Map<String, String> params;

    public LauncherFake(Map<String, String> params, Applet applet) {
        this.params = params;
    }

    public void appletResize(int width, int height) {
        // This space left intentionally blank
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
        validate();
    }

    public boolean isActive() {
        return true;
    }

    public URL getDocumentBase() {
        return getBase();
    }

    public URL getCodeBase() {
        return getBase();
    }

    private static URL getBase() {
        try {
            return new URL("http://127.0.0.1:" + RetroTweakInjectorTarget.localServerPort + "/game/");
        } catch (final MalformedURLException e) {
            LogWrapper.severe("Local server URL was malformed?: " + ExceptionUtils.getStackTrace(e));
        }

        return null;
    }

    public String getParameter(String paramName) {
        final String ret = params.containsKey(paramName) ? params.get(paramName) : null;

        if (ret == null) {
            LogWrapper.warning("Client asked for unknown parameter: " + paramName);
        } else {
            LogWrapper.info("Client asked for parameter: " + paramName + ", returned: " + ret);
        }

        return ret;
    }
}
