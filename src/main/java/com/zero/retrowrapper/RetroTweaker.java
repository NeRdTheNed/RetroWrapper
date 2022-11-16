package com.zero.retrowrapper;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import com.zero.retrowrapper.injector.M1ColorTweakInjector;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public final class RetroTweaker implements ITweaker {

    public enum M1PatchMode {
        OnlyM1MacOS,
        EnableWindowed,
        EnableWindowedInverted,
        ForceEnable,
        ForceDisable
    }

    public static M1PatchMode m1PatchMode = M1PatchMode.OnlyM1MacOS;

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
            // TODO
            boolean experimental = false;

            try {
                experimental = System.getProperties().getProperty("retrowrapper.enableExperimentalPatches") != null;
                final String patchMode = System.getProperties().getProperty("retrowrapper.forceM1PatchToValue");

                if (patchMode != null) {
                    m1PatchMode = M1PatchMode.valueOf(patchMode);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }

            if (RetroTweaker.m1PatchMode == M1PatchMode.ForceEnable) {
                M1ColorTweakInjector.isMinecraftFullscreen = false;
            }

            if ((RetroTweaker.m1PatchMode == M1PatchMode.ForceEnable) || (("aarch64".equals(SystemUtils.OS_ARCH) || (m1PatchMode == M1PatchMode.EnableWindowed) || (m1PatchMode == M1PatchMode.EnableWindowedInverted)) && experimental && (RetroTweaker.m1PatchMode != M1PatchMode.ForceDisable))) {
                classLoader.registerTransformer("com.zero.retrowrapper.injector.M1ColorTweakInjector");
            }
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
