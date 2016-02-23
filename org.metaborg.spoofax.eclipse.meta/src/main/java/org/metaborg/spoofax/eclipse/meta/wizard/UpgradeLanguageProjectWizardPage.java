package org.metaborg.spoofax.eclipse.meta.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class UpgradeLanguageProjectWizardPage extends WizardPage {
    private final String initialGroupId;
    private final String initialId;
    private final String initialVersion;
    private final String initialName;

    private LanguageIdentifierControls languageIdentifierControls;


    public UpgradeLanguageProjectWizardPage(String initialGroupId, String initialId, String initialVersion,
        String initialName) {
        super("page1");

        this.initialGroupId = initialGroupId;
        this.initialId = initialId;
        this.initialVersion = initialVersion;
        this.initialName = initialName;

        setTitle("Upgrade language project");
        setDescription("This wizard upgrades a language project to the newest version");
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

        final Label description = new Label(container, SWT.NULL);
        description.setText("Language identifiers are retrieved automatically if possible. If not, please copy them from the main ESV and/or pom.xml file.");

        languageIdentifierControls =
            new LanguageIdentifierControls(initialGroupId, initialId, initialVersion, initialName, container,
                new ModifyListener() {
                    @Override public void modifyText(ModifyEvent e) {
                        boolean valid = languageIdentifierControls.validate(UpgradeLanguageProjectWizardPage.this);
                        setPageComplete(valid);
                    }
                });
        
        setControl(container);
    }
}
