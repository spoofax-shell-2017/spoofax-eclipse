package org.metaborg.spoofax.eclipse;

import org.eclipse.core.commands.Command;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.metaborg.spoofax.eclipse.util.CommandStateUtils;

import com.google.inject.Inject;

public class SpoofaxPreferences {
    private final ICommandService commandService;

    private final Command disableBuildCommand;
    private final Command disableIncrementalBuildCommand;
    private final Command disableEditorAnalysisCommand;
    private final Command delayEditorAnalysisCommand;


    @Inject public SpoofaxPreferences() {
        this.commandService = PlatformUI.getWorkbench().getService(ICommandService.class);

        this.disableBuildCommand = commandService.getCommand("org.metaborg.spoofax.eclipse.command.disable.build");
        this.disableIncrementalBuildCommand =
            commandService.getCommand("org.metaborg.spoofax.eclipse.command.disable.incrementalbuild");
        this.disableEditorAnalysisCommand =
            commandService.getCommand("org.metaborg.spoofax.eclipse.command.disable.editoranalysis");
        this.delayEditorAnalysisCommand =
            commandService.getCommand("org.metaborg.spoofax.eclipse.command.delay.editoranalysis");
    }


    public boolean disableBuild() {
        return CommandStateUtils.toggleState(disableBuildCommand);
    }

    public boolean disableIncrementalBuild() {
        return CommandStateUtils.toggleState(disableIncrementalBuildCommand);
    }

    public boolean disableEditorAnalysis() {
        return CommandStateUtils.toggleState(disableEditorAnalysisCommand);
    }
    
    public boolean delayEditorAnalysis() {
        return CommandStateUtils.toggleState(delayEditorAnalysisCommand);
    }
}
