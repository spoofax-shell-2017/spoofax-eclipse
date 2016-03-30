package org.metaborg.spoofax.eclipse.build;

import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;

public class SpoofaxProjectBuilder extends
    ProjectBuilder<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate, ISpoofaxTransformUnit<?>> {

    public SpoofaxProjectBuilder() {
        super(SpoofaxPlugin.spoofax().processorRunner);
    }
}
