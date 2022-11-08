package com.zero.retrowrapper.emulator.registry;

import java.util.ArrayList;
import java.util.List;

import com.zero.retrowrapper.emulator.registry.handlers.GameHandler;
import com.zero.retrowrapper.emulator.registry.handlers.ListmapsHandler;
import com.zero.retrowrapper.emulator.registry.handlers.LoadHandler;
import com.zero.retrowrapper.emulator.registry.handlers.ResourcesHandler;
import com.zero.retrowrapper.emulator.registry.handlers.ResourcesHandlerBeta;
import com.zero.retrowrapper.emulator.registry.handlers.SaveHandler;
import com.zero.retrowrapper.emulator.registry.handlers.SkinOrCapeHandler;

public final class EmulatorRegistry {
    private static final List<IHandler> handlers;

    static {
        handlers = new ArrayList<IHandler>();
        handlers.add(new GameHandler());
        handlers.add(new SaveHandler());
        handlers.add(new LoadHandler());
        handlers.add(new ListmapsHandler());
        handlers.add(new ResourcesHandler());
        handlers.add(new ResourcesHandlerBeta());
        handlers.add(new SkinOrCapeHandler("/skin/", false));
        handlers.add(new SkinOrCapeHandler("/MinecraftSkins/", false));
        handlers.add(new SkinOrCapeHandler("/cloak/get.jsp?user=", true));
        handlers.add(new SkinOrCapeHandler("/MinecraftCloaks/", true));
    }

    public static IHandler getHandlerByUrl(String url) {
        for (final IHandler handler : handlers) {
            if (url.contains(handler.getUrl())) {
                return handler;
            }
        }

        return null;
    }
}
