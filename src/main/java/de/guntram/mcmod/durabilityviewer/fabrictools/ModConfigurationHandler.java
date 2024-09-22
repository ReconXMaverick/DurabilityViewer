/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.durabilityviewer.fabrictools;

/**
 *
 * @author gbl
 */
public interface ModConfigurationHandler {
    void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event);
    default void onConfigChanging(ConfigChangedEvent.OnConfigChangingEvent event) {}
    Configuration getConfig();
    default IConfiguration getIConfig() { return getConfig(); }
}
