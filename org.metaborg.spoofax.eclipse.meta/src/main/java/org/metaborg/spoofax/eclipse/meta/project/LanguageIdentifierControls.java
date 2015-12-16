package org.metaborg.spoofax.eclipse.meta.project;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;

public class LanguageIdentifierControls {
    public final Group container;

    private final Text inputGroupId;
    private final Text inputId;
    private final Text inputVersion;
    private final Text inputName;

    private boolean groupIdModified = false;
    private boolean idModified = false;
    private boolean versionModified = false;
    private boolean nameModified = false;


    public LanguageIdentifierControls(Composite parent, ModifyListener parentListener) {
        this("", "", "", "", parent, parentListener);
    }

    public LanguageIdentifierControls(String initialGroupId, String initialId, String initialVersion,
        String initialName, Composite parent, final ModifyListener parentListener) {
        container = new Group(parent, SWT.NONE);
        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        container.setLayout(layout);
        container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        container.setText("Language identification");

        new Label(container, SWT.NONE).setText("&Group ID:");
        inputGroupId = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputGroupId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputGroupId.setText(initialGroupId);
        inputGroupId.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                groupIdModified = true;
                parentListener.modifyText(e);
            }
        });

        new Label(container, SWT.NONE).setText("&ID:");
        inputId = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputId.setText(initialId);
        inputId.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                idModified = true;
                parentListener.modifyText(e);
            }
        });

        new Label(container, SWT.NONE).setText("&Version:");
        inputVersion = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputVersion.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputVersion.setText(initialVersion);
        inputVersion.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                versionModified = true;
                parentListener.modifyText(e);
            }
        });

        new Label(container, SWT.NONE).setText("&Name:");
        inputName = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputName.setText(initialName);
        inputName.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                nameModified = true;
                parentListener.modifyText(e);
            }
        });
    }


    public String groupId() {
        return inputGroupId.getText();
    }

    public String id() {
        return inputId.getText();
    }

    public String version() {
        return inputVersion.getText();
    }

    public String name() {
        return inputName.getText();
    }


    public boolean validate(WizardPage page) {
        final String groupId = groupId();
        if(groupIdModified) {
            if(groupId.isEmpty()) {
                page.setErrorMessage("Group ID must be filled in");
                return false;
            } else if(!LanguageIdentifier.validId(groupId)) {
                page.setErrorMessage("Group ID is invalid; " + LanguageIdentifier.errorDescription);
                return false;
            }
        }

        final String id = id();
        if(idModified) {
            if(id.isEmpty()) {
                page.setErrorMessage("ID must be filled in");
                return false;
            } else if(!LanguageIdentifier.validId(id)) {
                page.setErrorMessage("ID is invalid; " + LanguageIdentifier.errorDescription);
                return false;
            }
        }

        final String version = version();
        if(versionModified) {
            if(version.isEmpty()) {
                page.setErrorMessage("Version must be filled in");
                return false;
            } else if(!LanguageVersion.valid(version)) {
                page.setErrorMessage("Version is invalid; " + LanguageVersion.errorDescription);
                return false;
            }
        }

        final String name = name();
        if(nameModified) {
            if(name.isEmpty()) {
                page.setErrorMessage("Name must be filled in");
                return false;
            } else if(!LanguageIdentifier.validId(name)) {
                page.setErrorMessage("Name is invalid; " + LanguageIdentifier.errorDescription);
                return false;
            }
        }

        return true;
    }
}
