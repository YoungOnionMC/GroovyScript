package com.cleanroommc.groovyscript.compat.mods.botania;

import com.cleanroommc.groovyscript.compat.mods.ModPropertyContainer;

public class Botania extends ModPropertyContainer {

    public final PetalApothecary petalApothecary = new PetalApothecary();
    public final PureDaisy pureDaisy = new PureDaisy();
    public final RunicAltar runicAltar = new RunicAltar();
    public final ManaInfusion manaInfusion = new ManaInfusion();

    public Botania() {
        addRegistry(petalApothecary);
        addRegistry(pureDaisy);
        addRegistry(runicAltar);
        addRegistry(manaInfusion);
    }
}
