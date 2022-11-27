package com.zero.retrowrapper.emulator;

import java.applet.Applet;
import java.lang.reflect.Field;
import java.util.Random;

public final class EmulatorConfig {
    private static EmulatorConfig instance;

    public Field minecraftField;
    public Applet applet;

    private final int port;

    public String mobClass;

    private static final Random rand = new Random();

    private EmulatorConfig() {
        // TODO Is this a good way to determine the port?
        port = rand.nextInt(3000) + 25566;
    }

    public int getPort() {
        return port;
    }

    // TODO Is this threadsafe, and does it need to be?
    public static EmulatorConfig getInstance() {
        if (instance == null) {
            instance = new EmulatorConfig();
        }

        return instance;
    }
}
