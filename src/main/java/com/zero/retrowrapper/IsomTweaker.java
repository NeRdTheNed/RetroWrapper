package com.zero.retrowrapper;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public final class IsomTweaker implements ITweaker {
    private List<String> args;

    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args = args;
    }

    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.registerTransformer("com.zero.retrowrapper.injector.IsomTweakInjector");
    }

    public String getLaunchTarget() {
        return "com.zero.retrowrapper.injector.IsomTweakInjector";
    }

    public String[] getLaunchArguments() {
        return args.toArray(new String[args.size()]);
    }
}
