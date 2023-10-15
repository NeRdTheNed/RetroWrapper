package com.zero.retrowrapper;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.zero.retrowrapper.injector.M1ColorTweakInjector;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;

public final class RetroTweaker implements ITweaker {

    public enum M1PatchMode {
        OnlyM1MacOS,
        EnableWindowed,
        EnableWindowedInverted,
        ForceEnable,
        ForceDisable
    }

    public static final M1PatchMode m1PatchMode;

    public static String profile;
    private List<String> args;

    static {
        M1PatchMode tempPatchMode = M1PatchMode.OnlyM1MacOS;

        try {
            final String patchMode = System.getProperties().getProperty("retrowrapper.forceM1PatchToValue");

            if (patchMode != null) {
                tempPatchMode = M1PatchMode.valueOf(patchMode);
            }
        } catch (final Exception e) {
            LogWrapper.warning("Issue getting system properties: " + ExceptionUtils.getStackTrace(e));
        }

        m1PatchMode = tempPatchMode;
    }

    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        if (profile != null) {
            if (RetroTweaker.profile != null) {
                LogWrapper.warning("Profile changed? Was " + RetroTweaker.profile);
            }

            RetroTweaker.profile = profile.replace("-wrapped", "").replace("-launcher", "");
            LogWrapper.fine("Setting profile to " + RetroTweaker.profile);
        }

        this.args = args;
    }

    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        boolean fmlPatch = false;

        try {
            fmlPatch = Boolean.parseBoolean(System.getProperties().getProperty("retrowrapper.enableFMLPatch"));
        } catch (final Exception e) {
            LogWrapper.warning("Issue getting system properties: " + ExceptionUtils.getStackTrace(e));
        }

        if (fmlPatch) {
            // FML compatibility patch
            classLoader.registerTransformer("com.zero.retrowrapper.injector.FML125Injector");
        }

        // URL replacements
        classLoader.registerTransformer("com.zero.retrowrapper.injector.URLTweakInjector");
        classLoader.registerTransformer("com.zero.retrowrapper.injector.RetroTweakInjector");
        // Patches to use a 24 bit depth buffer when creating displays
        classLoader.registerTransformer("com.zero.retrowrapper.injector.DisplayTweakInjector");

        // Patches to fix crash bugs on macOS related to mouse movement + some tweaks to display the cursor correctly
        if (SystemUtils.IS_OS_MAC) {
            classLoader.registerTransformer("com.zero.retrowrapper.injector.MouseTweakInjector");
        }

        // TODO
        boolean experimental = false;

        try {
            experimental = System.getProperties().getProperty("retrowrapper.enableExperimentalPatches") != null;
        } catch (final Exception e) {
            LogWrapper.warning("Issue getting system properties: " + ExceptionUtils.getStackTrace(e));
        }

        if (m1PatchMode == M1PatchMode.ForceEnable) {
            M1ColorTweakInjector.isMinecraftFullscreen = false;
        }

        if ((m1PatchMode == M1PatchMode.ForceEnable) || ((("aarch64".equals(SystemUtils.OS_ARCH) && SystemUtils.IS_OS_MAC) || (m1PatchMode == M1PatchMode.EnableWindowed) || (m1PatchMode == M1PatchMode.EnableWindowedInverted)) && experimental && (m1PatchMode != M1PatchMode.ForceDisable))) {
            classLoader.registerTransformer("com.zero.retrowrapper.injector.M1ColorTweakInjector");
        }
    }

    public String getLaunchTarget() {
        return "com.zero.retrowrapper.injector.RetroTweakInjectorTarget";
    }

    public String[] getLaunchArguments() {
        return args.toArray(new String[0]);
    }
}
