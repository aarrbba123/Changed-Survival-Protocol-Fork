This is a **fork** for the repository of the mod **Changed-Survival-Protocol**.
This fork of the mod is currently on version **1.3.0-fork13** and requires Changed-Minecraft-Mod version **0.15.4+**.

## How can I compile the mod?

Without an IDE, and assuming you have `git` installed and Java 17 as your default Java:
- Clone the repo `git clone https://github.com/stoneman2/Changed-Survival-Protocol.git`
- Navigate into the directory `cd Changed-Survival-Protocol`
- Run Gradle with `./gradlew jar` on Linux or macOS, or `gradlew.bat jar` on Windows
- Once completed, check `build/libs` for the result.

## Changed: Survival Protocol
An addon to the Changed minecraft mod prioritizing survival gameplay. This mod adds and overrides many features within the original mod.

### Reworked Infection Mechanics
When you are hit by a latex beast, you will gain `Coverage`.  
When `Coverage` reaches 100, you will gain `Infection`.  
When `Infection` reaches 100, you will transfur.  

This adds a way to survive and avoid a transfur in survival. Instead of instantly die / transfurring, you go through an infection phase first.  

While you have `Coverage`, you can enter a body of water to quickly lower your `Coverage`.  
While you have `Infection`, you can attempt to cure yourself with a dedicated cure gameplay loop.  

### Latex Infestations
Reworked the original's mod infestations considerably.  
Introducing the `Latex Heart` and `Latex Node`:  
These new blocks spawn randomly within new chunks, or very rarely within currently loaded chunks. (Configurable)  
These blocks are the source of the latex infection. When these blocks are around, they will begin spreading latex around the area, quickly coating everything around itself.  
To stop the infestation, you must first destroy all the `Latex Node`(s) and then destroy the `Latex Heart`.  
When the heart is destroyed, all latex in the area will start to decay away.  

### Lucidity and Latex Maintenance
Being a latex beast isn't easy, and you must maintain your form, or your inside-beast will break free.  
While you are in a latex form, you will have a `Lucidity` meter. You must maintain this meter to stay alive.  
When you are near your species' latex type or near a crowd of your species latexes\*, you will slowly regain lucidity.  
Eating food, assimilating / replicating will increase your lucidity.  
Sleeping near your species' latex type will rapidly replenish lucidity.  

But beware, unless you stabilize yourself, there is no avoiding losing your sanity. Lucidity drain rates scales up exponentially.  

Losing yourself as a latex beast spawns a Latex Heart where you die! Beware!  

\* Those who are transfurred without a latex type only need to be near friendly beasts to slowly regain lucidity.  

### Curing Loop
First, craft a Microscope and a scalpel. With the scalpel, extract a sample from yourself to get an `Unidentified Strand`.  
Then, use that strand in the microscope to find out which strand you are.  

With the identified strand, you must then craft the `Latex Centrifuge`.  
Using the Centrifuge, use the identified strand and other materials to develop a cure to your current strand, removing your `Infection`.  

You cannot cure yourself after being transfurred. It's too late for you.

### Stabilizing Loop
Just like the curing loop, you must first craft a Microscope. Using a syringe, extract latex to get a `latex syringe` corresponding to your form.  
Then, use the syringe in the microsope to find out which strand you are.

With the identified strand, you also must craft the `Latex Centrifuge`  
Using the Centrifuge, use the identified strand and other materials to develop a `Cultured Latex Strand`.

Then, you must attune your form with the cultured strand. You can do this by performing actions that restore your lucidity.  
Once fully attuned, the `Cultured Latex Strand` will turn into a `Stabilization Dose` which will stabilize you.

Be warned, as once you stabilize yourself, you will not be able to turn back into a human by any normal means, even when using items from other mods!

### New Defaults
There are now new defaults to the gamerules. By default, all transformations are permanent, but you will always keep your consciousness, to play into the new Lucidity mechanics.
