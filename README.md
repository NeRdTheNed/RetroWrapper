# RetroWrapper - NeRd fork

This is a fork of RetroWrapper, which the original author seems to have abandoned. RetroWrapper works as a proxy between the lack of old mojang servers and your older versions of minecraft, by emulating these servers. I'm going to sit down and get to grips with it at some point, but for now I might mainly focus on smaller and easier to understand issues. No promises.

Head to the releases tab to grab the newest version. 

Legacy readme preserved below.

# RetroWrapper
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
