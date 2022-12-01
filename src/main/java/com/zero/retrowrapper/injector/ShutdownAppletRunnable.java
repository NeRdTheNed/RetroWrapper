package com.zero.retrowrapper.injector;

import java.applet.Applet;

final class ShutdownAppletRunnable implements Runnable {
    private final Applet applet;

    public ShutdownAppletRunnable(Applet applet) {
        this.applet = applet;
    }

    public void run() {
        applet.stop();
    }
}
