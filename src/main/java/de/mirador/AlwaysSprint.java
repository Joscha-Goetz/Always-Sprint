package de.mirador;

//? if fabric {
import net.fabricmc.api.ModInitializer;
//?} else {
/*import net.neoforged.fml.common.Mod;
*///?}
//? if >=1.18 {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

//? if neoforge
/*@Mod("always_sprint")*/
public class AlwaysSprint /*? if fabric {*/implements ModInitializer/*?}*/ {
    //? if >=1.18 {
    public static final Logger LOGGER = LoggerFactory.getLogger("always-sprint");
    //?} else
    /*public static final Logger LOGGER = LogManager.getLogger("always-sprint");*/
    public static final String VERSION = /*$ mod_version*/ "2.0.1";
    public static final String MINECRAFT = /*$ minecraft*/ "26.3";

    //? if fabric {
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Always Sprint Mod");
    }
    //?} else {
    /*public AlwaysSprint() {
        LOGGER.info("Initializing Always Sprint Mod");
    }
    *///?}
}
