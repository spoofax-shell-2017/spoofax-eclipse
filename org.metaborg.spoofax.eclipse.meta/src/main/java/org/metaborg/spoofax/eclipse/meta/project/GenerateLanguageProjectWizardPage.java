package org.metaborg.spoofax.eclipse.meta.project;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class GenerateLanguageProjectWizardPage extends WizardNewProjectCreationPage {
    private LanguageIdentifierControls languageIdentifierControls;


    public GenerateLanguageProjectWizardPage() {
        super("page1");

        setTitle("Create Spoofax language project");
        setDescription("This wizard creates a Spoofax language project");
    }


    public String groupId() {
        return languageIdentifierControls.groupId();
    }

    public String id() {
        return languageIdentifierControls.id();
    }

    public String version() {
        return languageIdentifierControls.version();
    }

    public String name() {
        return languageIdentifierControls.name();
    }


    @Override public void createControl(Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);
        final GridLayout layout = new GridLayout();
        container.setLayout(layout);

        super.createControl(container);
        // HACK: fix wrong layout data of the parent control
        getControl().setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        languageIdentifierControls = new LanguageIdentifierControls("", "", "", "", container, new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                boolean valid = validatePage();
                setPageComplete(valid);
            }
        });
        
        setControl(container);
    }

    @Override protected boolean validatePage() {
        return super.validatePage() && languageIdentifierControls.validate(this);
    }
}
