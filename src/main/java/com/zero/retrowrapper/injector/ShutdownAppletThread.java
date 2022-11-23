package com.zero.retrowrapper.injector;

import java.applet.Applet;

final class ShutdownAppletThread extends Thread {
    private final Applet applet;

    public ShutdownAppletThread(Applet applet) {
        this.applet = applet;
    }

    public void run() {
        applet.stop();
    }
}
