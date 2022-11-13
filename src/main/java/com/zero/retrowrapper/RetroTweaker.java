package com.zero.retrowrapper;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

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
        // URL replacements
        classLoader.registerTransformer("com.zero.retrowrapper.injector.RetroTweakInjector");
        // Patches to use a 24 bit depth buffer when creating displays
        classLoader.registerTransformer("com.zero.retrowrapper.injector.DisplayTweakInjector");

        // Patches to fix crash bugs on MacOS related to mouse movement + some tweaks to display the cursor correctly
        if (SystemUtils.IS_OS_MAC) {
            classLoader.registerTransformer("com.zero.retrowrapper.injector.MouseTweakInjector");
        }
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
