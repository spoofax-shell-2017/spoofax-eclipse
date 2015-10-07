package org.metaborg.spoofax.eclipse.meta.language.upgrade;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class UpgradeLanguageProjectWizardPage extends WizardPage {
    private final String groupId;
    private final String id;
    private final String version;
    private final String name;

    public Text inputGroupId;
    public Text inputId;
    public Text inputVersion;
    public Text inputName;


    public UpgradeLanguageProjectWizardPage(String groupId, String id, String version, String name) {
        super("wizardPage");

        this.groupId = groupId;
        this.id = id;
        this.version = version;
        this.name = name;

        setTitle("Upgrade language project");
        setDescription("This wizard upgrades a language project to the newest version");
    }


    @Override public void createControl(Composite parent) {
        final Composite container = new Composite(parent, SWT.NULL);
        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.verticalSpacing = 9;
        container.setLayout(layout);

        final Label description = new Label(container, SWT.NULL);
        description.setText("Language properties are retrieved from the packed.esv file if it exists.\n"
            + "If it does not exist, please copy them from the generated.esv file.");
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_END);
        gridData.horizontalSpan = 2;
        gridData.horizontalAlignment = GridData.FILL;
        description.setLayoutData(gridData);

        new Label(container, SWT.NULL).setText("&Group ID:");
        inputGroupId = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputGroupId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputGroupId.setText(groupId);
        inputGroupId.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onChange();
            }
        });

        new Label(container, SWT.NULL).setText("&ID:");
        inputId = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputId.setText(id);
        inputId.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onChange();
            }
        });

        new Label(container, SWT.NULL).setText("&Version:");
        inputVersion = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputVersion.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputVersion.setText(version);
        inputVersion.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onChange();
            }
        });

        new Label(container, SWT.NULL).setText("&Name:");
        inputName = new Text(container, SWT.BORDER | SWT.SINGLE);
        inputName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputName.setText(name);
        inputName.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onChange();
            }
        });

        setControl(container);
        onChange();
    }


    private void onChange() {
        if(inputName.getText().isEmpty() || inputId.getText().isEmpty()) {
            setPageComplete(false);
        } else {
            setPageComplete(true);
        }
    }
}
