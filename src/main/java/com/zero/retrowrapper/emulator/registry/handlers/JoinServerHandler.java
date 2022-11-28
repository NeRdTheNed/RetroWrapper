package com.zero.retrowrapper.emulator.registry.handlers;

import java.io.IOException;
import java.io.OutputStream;

import com.zero.retrowrapper.emulator.registry.EmulatorHandler;
import com.zero.retrowrapper.injector.RetroTweakInjectorTarget;
import com.zero.retrowrapper.util.NetworkUtil;

import net.minecraft.launchwrapper.LogWrapper;

public final class JoinServerHandler extends EmulatorHandler {

    private static byte[] OK = "OK".getBytes();
    private static byte[] NOT_OK = "Bad login".getBytes();

    public JoinServerHandler(String url) {
        super(url);
    }

    public void handle(OutputStream os, String get, byte[] data) throws IOException {
        String username = RetroTweakInjectorTarget.username;
        String sessionId = RetroTweakInjectorTarget.sessionId;
        String serverId = "";
        final String urlParams = get.split("\\?")[1];

        for (final String param : urlParams.split("&")) {
            final String[] split = param.split("=");
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
