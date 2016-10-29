package org.metaborg.spoofax.eclipse.meta.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.metaborg.meta.core.wizard.CreateLanguageSpecWizard;
import org.metaborg.meta.core.wizard.UpgradeLanguageSpecWizard;

public class UpgradeLangSpecWizardPage extends WizardPage {
    private final String initialGroupId;
    private final String initialId;
    private final String initialVersion;
    private final String initialName;

    private LanguageIdentifierControls languageIdentifierControls;
    private UpgradeLanguageSpecWizard upgradeLanguageSpecWizard;

    private boolean ignoreEvents = false;


    public UpgradeLangSpecWizardPage(String initialGroupId, String initialId, String initialVersion,
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
        // Create a container for this page.
        final Composite container = new Composite(parent, SWT.NONE);
        final GridLayout layout = new GridLayout();
        container.setLayout(layout);

        final Label description = new Label(container, SWT.NULL);
        description.setText(
            "Language identifiers are retrieved automatically if possible. If not, please copy them from the main ESV and/or pom.xml file.");

        // Create language identifier controls, with as parent our container.
        languageIdentifierControls = new LanguageIdentifierControls(initialGroupId, initialId, initialVersion,
            initialName, container, new ModifyListener() {
                @Override public void modifyText(ModifyEvent e) {
                    if(ignoreEvents) {
                        return;
                    }

                    final boolean valid = validatePage();
                    setPageComplete(valid);
                }
            });

        // Instantiate and implement 'create language specification' wizard helper.
        upgradeLanguageSpecWizard = new UpgradeLanguageSpecWizard() {
            @Override protected boolean inputNameModified() {
                return languageIdentifierControls.nameModified();
            }

            @Override protected String inputName() {
                return languageIdentifierControls.name();
            }

            @Override protected boolean inputIdModified() {
                return languageIdentifierControls.idModified();
            }

            @Override protected String inputId() {
                return languageIdentifierControls.id();
            }

            @Override protected boolean inputGroupIdModified() {
                return languageIdentifierControls.groupIdModified();
            }

            @Override protected String inputGroupId() {
                return languageIdentifierControls.groupId();
            }

            @Override protected boolean inputVersionModified() {
                return languageIdentifierControls.versionModified();
            }

            @Override protected String inputVersion() {
                return languageIdentifierControls.version();
            }


            @Override protected void setGroupId(String groupId) {
                setIgnoreEvents(true);
                languageIdentifierControls.inputGroupId.setText(groupId);
                setIgnoreEvents(false);
            }

            @Override protected void setVersion(String version) {
                setIgnoreEvents(true);
                languageIdentifierControls.inputVersion.setText(version);
                setIgnoreEvents(false);
            }
        };
        upgradeLanguageSpecWizard.setDefaults();

        // Revalidate to prevent form from being completed when not completely filled in.
        final boolean valid = validatePage();
        setPageComplete(valid);

        // Set the current control to our container.
        setControl(container);
    }

    private boolean validatePage() {
        // Validation can be executed before control has been made, check for null to confirm.
        if(upgradeLanguageSpecWizard == null) {
            return true;
        }

        final CreateLanguageSpecWizard.ValidationResult result = upgradeLanguageSpecWizard.validate();
        if(result.complete) {
            return true;
        }

        if(!result.errors.isEmpty()) {
            final String error = result.errors.get(0);
            setErrorMessage(error);
        } else {
            setErrorMessage(null);
        }

        return false;
    }

    private void setIgnoreEvents(boolean ignoreEvents) {
        this.ignoreEvents = ignoreEvents;
        languageIdentifierControls.setIgnoreEvents(ignoreEvents);
    }
}
