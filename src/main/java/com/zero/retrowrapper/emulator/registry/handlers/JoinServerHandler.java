package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.injector.RetroTweakInjectorTarget;
import com.zero.retrowrapper.util.NetworkUtil;

import net.minecraft.launchwrapper.LogWrapper;

public final class JoinServerHandler extends EmulatorHandler {

    private static final byte[] OK = "OK".getBytes();
    private static final byte[] NOT_OK = "Bad login".getBytes();

    private static final Pattern questionMarkPattern = Pattern.compile("\\?");
    private static final Pattern andPattern = Pattern.compile("&");
    private static final Pattern equalsPattern = Pattern.compile("=");

    public JoinServerHandler(String url) {
        super(url);
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        String username = RetroTweakInjectorTarget.username;
        String sessionId = RetroTweakInjectorTarget.sessionId;
        String serverId = "";

        for (final String param : andPattern.split(questionMarkPattern.split(get)[1])) {
            final String[] split = equalsPattern.split(param);
            final String key = split[0];
            final String value = split[1];

            if ("user".equals(key)) {
                username = value;
            } else if ("sessionId".equals(key)) {
                sessionId = value;
            } else if ("serverId".equals(key)) {
                serverId = value;
            }
        }

        LogWrapper.info("Connecting to server " + serverId);
        os.write(NetworkUtil.joinServer(sessionId, username, serverId) ? OK : NOT_OK);
    }

}
