# RetroWrapper - NeRd fork

> A way to play fixed old versions of Minecraft from the vanilla launcher, MultiMC, and more.

[![Build status](https://github.com/NeRdTheNed/RetroWrapper/actions/workflows/gradle.yml/badge.svg)](https://github.com/NeRdTheNed/RetroWrapper/actions/workflows/gradle.yml)

## Usage

RetroWrapper is available as an installer for creating wrapped versions to use in the vanilla launcher, or a MultiMC-formatted instance. Head to the [releases page](https://github.com/NeRdTheNed/RetroWrapper/releases) to grab the newest version! If you find any issues I don't know about or I've accidentally introduced, please report them on the [issues page](https://github.com/NeRdTheNed/RetroWrapper/issues).

## What is RetroWrapper?

RetroWrapper is a collection of patches and fixes for old versions of Minecraft, to restore functionality and fix various compatibility issues. Features include:

Server / website emulation:
- Classic online level loading and saving emulation
- Modern server authentication support (with experimental classic server support)
- Skin, cape, and sound fixes (with offline cache)

Patches:
- Bit depth fix
- Classic mouse moving
- Indev loading / saving
- macOS native cursor crash fix and input patches
- Quit button fix
- (Experimental) M1 windowed colour fixes

Misc:
- Built-in update checker and crash report helper
- Java 5+ support (including Java 9+ support!)
- Multi-platform, launcher, and version support
- Support for using updated libraries in wrapped instances
- Support for using M1 LWJGL natives in wrapped instances

Additionally, RetroWrapper includes support for some more obscure features and versions, including the isometric viewer, Minecraft 4K support, and a single player teleport "hack" tool.

RetroWrapper includes an installer for use with the vanilla launcher, which supports Windows, macOS and Linux. MultiMC instances using RetroWrapper can be downloaded from the releases page as well.

## How does RetroWrapper work?

RetroWrapper is built on the same technology that the official Minecraft launcher uses for patching old versions of Minecraft: [LegacyLauncher](https://github.com/Mojang/LegacyLauncher). RetroWrapper uses tweak classes to implement many runtime patches, and to re-direct known URLs to the local server emulator. The server emulator runs locally on your PC, and acts as a replacement for online functionality, such as handling resource downloads, and server authentication.

As RetroWrapper uses LegacyLauncher, it is version independent, and able to run on a number of launchers.

## Credits

Binary distributions of this software bundle classes from the Apache Commons project, which are licensed under the Apache License Version 2.0. A full copy of this license can be found in COMMONS-LICENSE.txt, which should be in the top-level directory of this repository. Binary distributions also include this information under META-INF.

## Original Readme (outdated)
Enables you to play _fixed_ old versions of minecraft without ever touching .jar files, works even when offline!

Needs Java 7 or higher!!

**WHAT IS DONE**
- Fixed indev loading
- Skins (with offline cache!)
- Sounds
- Saving
- Online Saving
- Mouse movement on very old classic

**HOW TO USE (automatic)**

Download latest version from releases and launch it.

Select version you want to wrap and click 'Install'

**ISOMETRIC VIEWER**

Only for inf-20100627 and inf-20100618.

Patch that version, and edit inf-20100627-wrapped.json

Change tweakClass com.zero.retrowrapper.RetroTweaker to tweakClass com.zero.retrowrapper.IsomTweaker

Done

**SINGLEPLAYER HACKS**

- Teleport hack (useful for checking farlands!)

Works all the way from 0.27 to Release 1.0, havent tested other versions but propably it works too.

You need to add -Dretrowrapper.hack=true to Java arguments in your launcher.

**HOW TO USE (manual)**

Download retrowrapper-1.2.jar from releases.

Navigate to .minecraft/libraries/com/

Create new folder 'zero' and navigate to it

Create new folder 'retrowrapper' inside 'zero' and navigate to it

Create new folder '1.2' inside 'retrowrapper' and navigate to it

Copy retrowrapper-1.2.jar to '1.2'

Now go into .minecraft/versions/

Copy that folder you want to patch and add -retro to its name (eg. c0.30_01 to c0.30_01-retro)

Go inside that folder and add -retro to all filenames inside it

Edit <version>.json and

- add -retro to id (eg. replace **"id": "c0.30_01c",* with *"id": "c0.30_01c-retro",**)
- replace **"libraries":** with **"libraries": [{"name": "com.zero:retrowrapper:1.2"},**
- replace **--tweakClass net.minecraft.launchwrapper....VanillaTweaker** with **--tweakClass com.zero.retrowrapper.RetroTweaker**

Launch Minecraft and choose newly created version!





Uses minimal-json by ralfstx
