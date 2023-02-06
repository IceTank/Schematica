## Welcome to Schematica!

This is my fork of Schematica, with the goal to make it usable on Anarchy servers, 2b2t in particular, and to improve its overall quality.

This version is a Version of Schematica that has been modified and forked a lot of times. Here is a list off all contributed Versions:
- This version is a code fork of Theyoungster Schematica from [here](https://github.com/Theyoungster/Schematica)

- Which is itself a fork from EmotionalLove's Schematica [here](https://github.com/EmotionalLove/Schematica)

- Which was forked from the original Schematica Mod Developed by Lunatrius [here](https://github.com/Lunatrius/Schematica)

There are also code elements that where taken from Negative Entropy's [Schematica](https://github.com/Entropy5/Schematica)

### New Features and QoL changes

1. Stealth mode. This allows Schematica to work on 2b2t, using more strict placement requirements and rotation packets. (NOTE: STAIRS ARE BROKEN)
2. Placement Priority. When filling blocks in, place layer-by-layer, or fill in as pillars.
3. Directional Priority. Place blocks behind you as you walk, place the farthest blocks first, or just place along the X axis first.
4. Disable while moving. Stops printing while you're moving.
5. More flexible placement delay- includes decimals, and  can place every client tick.
6. Wallhack rendering. View missing and incorrect blocks by pressing the View Errors key (default LCONTROL)
7. Snap to Map. Great for map art!
8. Block supports. Things like sand, anvils and torches check  to see if the supporting block exists before placing.
9. Much more fluid UI, including merged Load and Manipulate Schematic keybinds and cleaner rotation buttons.
10. 2b inventory equip bypass (@IceTank)
11. Expand Schematica's Pick block feature on Schematic preview blocks. The Pick block button will try to equip the preview block you are looking at or any shulkers in your inventory that contains that block. No more inventory searching for shulkers that may or may not contain that item! (You may have to rebind Schematicas Pick block button to something like CTRL + Middle Click)
12. Auto align by formatted file name (Taken from @Entropy5's Schematica Fork [here](https://github.com/Entropy5/Schematica/commit/9a9a6fec061a727dcd8602100247e4e395cf5536))

### Compiling

[Setup Schematica](#setup-schematica)

[Compile Schematica](#compile-schematica)

[Updating Your Repository](#updating-your-repository)

#### Setup Schematica
This section assumes that you have Git and JDK installed, and you're using the command-line version of Git.

1. Open up your command line.
2. Navigate to a place where you want to download Schematica's source (eg `C:\Development\Github\Minecraft\`) by executing `cd [folder location]`. This location is known as `mcdev` from now on.
3. Execute `git clone https://github.com/Theyoungster/Schematica.git`. This will download Schematica's source into `mcdev`.
4. Right now, you should have a directory that looks something like:

***
    mcdev
    \-Schematica
        \-Schematica's files (should have build.gradle)
***

#### Compile Schematica
1. Execute `gradlew setupDevWorkspace`. This sets up Forge and downloads the necessary libraries to build Schematica. This might take some time, be patient.
    * You will generally only have to do this once until the Forge version in `gradle.properties` changes.
2. Execute `gradlew build`. If you did everything right, `BUILD SUCCESSFUL` will be displayed after it finishes. This should be relatively quick.
    * If you see `BUILD FAILED`, check the error output (it should be right around `BUILD FAILED`), fix everything (if possible), and try again.
3. Go to `mcdev\Schematica\build\libs`.
    * You should see a `.jar` file named `Schematica-#.#.#-#.#.#.#-universal.jar`.
4. Copy the jar into your Minecraft mods folder, and you are done!

#### Updating Your Repository
In order to get the most up-to-date builds, you'll have to periodically update your local repository.

1. Open up your command line.
2. Navigate to `mcdev` in the console.
3. Make sure you have not made any changes to the local repository, or else there might be issues with Git.
    * If you have, try reverting them to the status that they were when you last updated your repository.
4. Execute `git pull master`. This pulls all commits from the official repository that do not yet exist on your local repository and updates it.

Shamelessly based this README off [pahimar's version](https://github.com/pahimar/Equivalent-Exchange-3).
