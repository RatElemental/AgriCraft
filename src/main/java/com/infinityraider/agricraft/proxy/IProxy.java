package com.infinityraider.agricraft.proxy;

import com.agricraft.agricore.core.AgriCore;
import com.infinityraider.agricraft.AgriCraft;
import com.infinityraider.agricraft.config.Config;
import com.infinityraider.agricraft.impl.v1.PluginHandler;
import com.infinityraider.agricraft.impl.v1.CoreHandler;
import com.infinityraider.infinitylib.proxy.base.IProxyBase;
import com.infinityraider.infinitylib.utility.ReflectionHelper;
import net.minecraft.command.ICommand;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.function.Function;

public interface IProxy extends IProxyBase<Config> {

    @Override
    default Function<ForgeConfigSpec.Builder, Config> getConfigConstructor() {
        return Config.Common::new;
    }

    @Override
    default void preInitStart(FMLPreInitializationEvent event) {
        CoreHandler.preInit(event);
        registerEventHandler(AgriCraft.instance);
        PluginHandler.preInit(event);
    }

    @Override
    default void initStart(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(AgriCraft.instance, new GuiHandler());
        PluginHandler.init();
        initCustomWoodTypes();
    }

    @Override
    default void initEnd(FMLInitializationEvent event) {
        CoreHandler.init();
    }

    @Override
    default void postInitStart(FMLPostInitializationEvent event) {
        PluginHandler.postInit();
        AgriOreDict.upgradeOreDict();
    }

    default void registerVillagerSkin(int id, String resource) {
    }

    default void initCustomWoodTypes() {
        CustomWoodTypeRegistry.init();
    }

    @Override
    default void registerCapabilities() {
    }

    @Override
    default void registerEventHandlers() {
    }

    @Override
    default void activateRequiredModules() {
    }

    @Override
    default void initConfiguration(FMLPreInitializationEvent event) {
    }

    // Since apparently translation is now client-side only.
    // This is why we can't have nice things.
    default String translateToLocal(String string) {
        // The {**} is a hack to get TOP integration to work.
        return "{*" + string + "*}";
    }

    default String getLocale() {
        // Whatever...
        return "en_US";
    }
    
    @Override
    default void onServerStarting(FMLServerStartingEvent event) {
        // This is to be moved to infinity lib in a future version, I would expect.
        AgriCore.getLogger("agricraft").info("Registering AgriCraft Commands.");
        ReflectionHelper.forEachValueIn(AgriCraft.instance.getModCommandRegistry(), ICommand.class, event::registerServerCommand);
    }
}
