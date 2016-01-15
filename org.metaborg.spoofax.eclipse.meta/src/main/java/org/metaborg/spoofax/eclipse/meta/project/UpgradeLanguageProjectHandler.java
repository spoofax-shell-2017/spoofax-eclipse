package org.metaborg.spoofax.eclipse.meta.project;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.core.project.ILanguageSpecService;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.configuration.ILanguageSpecConfigService;
import org.metaborg.core.project.settings.IProjectSettingsService;
import org.metaborg.spoofax.core.project.ISpoofaxLanguageSpecPathsService;
import org.metaborg.spoofax.core.project.configuration.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.AbstractHandlerUtils;

import com.google.inject.Injector;

public class UpgradeLanguageProjectHandler extends AbstractHandler {
    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ILanguageSpecService languageSpecService;
    private final ILanguageSpecConfigService configService;
    private final ISpoofaxLanguageSpecConfigBuilder configBuilder;
    private final ISpoofaxLanguageSpecPathsService pathsService;
    private final IProjectSettingsService projectSettingsService;
    private final ITermFactoryService termFactoryService;


    public UpgradeLanguageProjectHandler() {
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.languageSpecService = injector.getInstance(ILanguageSpecService.class);
        this.configService = injector.getInstance(ILanguageSpecConfigService.class);
        this.configBuilder = injector.getInstance(ISpoofaxLanguageSpecConfigBuilder.class);
        this.pathsService = injector.getInstance(ISpoofaxLanguageSpecPathsService.class);
        this.projectSettingsService = injector.getInstance(IProjectSettingsService.class);
        this.termFactoryService = injector.getInstance(ITermFactoryService.class);
    }


    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final IProject project = AbstractHandlerUtils.toProject(event);
        if(project == null) {
            return null;
        }

        final UpgradeLanguageProjectWizard wizard =
            new UpgradeLanguageProjectWizard(
                    resourceService,
                    projectService,
                    languageSpecService,
                    configService,
                    configBuilder,
                    pathsService,
                    projectSettingsService,
                    termFactoryService,
                    project);
        final Shell shell = HandlerUtil.getActiveWorkbenchWindow(event).getShell();
        final WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.open();
        return null;
    }
}
