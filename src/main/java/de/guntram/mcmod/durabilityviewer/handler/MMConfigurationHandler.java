package de.guntram.mcmod.durabilityviewer.handler;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.guntram.mcmod.durabilityviewer.DurabilityViewer;
import de.guntram.mcmod.durabilityviewer.fabrictools.ConfigurationProvider;
import de.guntram.mcmod.durabilityviewer.fabrictools.GuiModOptions;

public class MMConfigurationHandler implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> GuiModOptions.getGuiModOptions(screen, DurabilityViewer.MODNAME, ConfigurationProvider.getHandler(DurabilityViewer.MODNAME));
    }
}
