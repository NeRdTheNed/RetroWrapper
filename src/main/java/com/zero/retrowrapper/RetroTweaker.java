package com.zero.retrowrapper;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public final class RetroTweaker implements ITweaker {
    private List<String> args;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args = args;
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.registerTransformer("com.zero.retrowrapper.injector.RetroTweakInjector");
        classLoader.registerTransformer("com.zero.retrowrapper.injector.MouseTweakInjector");
    }

    @Override
    public String getLaunchTarget() {
        return "com.zero.retrowrapper.injector.RetroTweakInjectorTarget";
    }

    @Override
    public String[] getLaunchArguments() {
        return args.toArray(new String[args.size()]);
    }
}
