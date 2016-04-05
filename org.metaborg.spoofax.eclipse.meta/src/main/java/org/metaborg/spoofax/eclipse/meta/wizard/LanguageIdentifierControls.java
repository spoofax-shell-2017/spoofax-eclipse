package org.metaborg.spoofax.eclipse.meta.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Strings;

public class LanguageIdentifierControls {
    public final Group container;

    public final Text inputGroupId;
    public final Text inputId;
    public final Text inputVersion;
    public final Text inputName;

    private boolean idModified = false;
    private boolean nameModified = false;
    private boolean versionModified = false;
    private boolean groupIdModified = false;

    private boolean ignoreEvents = false;


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

        new Label(container, SWT.NONE).setText("&Identifier:");
        inputId = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if(!Strings.isNullOrEmpty(initialId)) {
            inputId.setText(initialId);
            idModified = true;
        }
        inputId.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                if(ignoreEvents) {
                    return;
                }
                idModified = true;
                parentListener.modifyText(e);
            }
        });

        new Label(container, SWT.NONE).setText("&Name:");
        inputName = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if(!Strings.isNullOrEmpty(initialName)) {
            inputName.setText(initialName);
            nameModified = true;
        }
        inputName.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                if(ignoreEvents) {
                    return;
                }
                nameModified = true;
                parentListener.modifyText(e);
            }
        });

        new Label(container, SWT.NONE).setText("&Version:");
        inputVersion = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputVersion.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if(!Strings.isNullOrEmpty(initialVersion)) {
            inputVersion.setText(initialVersion);
            versionModified = true;
        }
        inputVersion.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                if(ignoreEvents) {
                    return;
                }
                versionModified = true;
                parentListener.modifyText(e);
            }
        });

        new Label(container, SWT.NONE).setText("&Group identifier:");
        inputGroupId = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputGroupId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if(!Strings.isNullOrEmpty(initialGroupId)) {
            inputGroupId.setText(initialGroupId);
            groupIdModified = true;
        }
        inputGroupId.addModifyListener(new ModifyListener() {
            @Override public void modifyText(ModifyEvent e) {
                if(ignoreEvents) {
                    return;
                }
                groupIdModified = true;
                parentListener.modifyText(e);
            }
        });
    }


    public void setIgnoreEvents(boolean ignoreEvents) {
        this.ignoreEvents = ignoreEvents;
    }


    public boolean groupIdModified() {
        return groupIdModified;
    }

    public String groupId() {
        return inputGroupId.getText();
    }

    public boolean idModified() {
        return idModified;
    }

    public String id() {
        return inputId.getText();
    }

    public boolean versionModified() {
        return versionModified;
    }

    public String version() {
        return inputVersion.getText();
    }

    public boolean nameModified() {
        return nameModified;
    }

    public String name() {
        return inputName.getText();
    }
}
