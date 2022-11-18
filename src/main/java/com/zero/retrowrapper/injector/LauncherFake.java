package com.zero.retrowrapper.injector;

import java.applet.Applet;
import java.applet.AppletStub;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.zero.retrowrapper.emulator.EmulatorConfig;

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
            return new URL("http://127.0.0.1:" + EmulatorConfig.getInstance().getPort() + "/game/");
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getParameter(String paramName) {
        if (params.containsKey(paramName)) {
            return params.get(paramName);
        }

        System.err.println("Client asked for parameter: " + paramName);
        return null;
    }
}
