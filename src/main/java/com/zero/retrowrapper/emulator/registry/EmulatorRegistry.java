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
    private final List<IHandler> handlers = new ArrayList<>();

    private void register(EmulatorHandler handler) {
        handlers.add(handler);
    }

    public IHandler getHandlerByUrl(String url) {
        for (final IHandler handler : handlers) {
            if (url.contains(handler.getUrl())) {
                return handler;
            }
        }

        return null;
    }

    public void registerAll() {
        register(new GameHandler());
        register(new SaveHandler());
        register(new LoadHandler());
        register(new ListmapsHandler());
        register(new ResourcesHandler());
        register(new ResourcesHandlerBeta());
        register(new SkinOrCapeHandler("/skin/", false));
        register(new SkinOrCapeHandler("/MinecraftSkins/", false));
        register(new SkinOrCapeHandler("/capes/", true));
        register(new SkinOrCapeHandler("/cloak/get.jsp?user=", true));
        register(new SkinOrCapeHandler("/MinecraftCloaks/", true));
    }
}
