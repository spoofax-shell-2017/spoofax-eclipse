package org.metaborg.spoofax.eclipse.editor;

import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Injector;

/**
 * Typedef class for {@link MetaBorgEditor} with Spoofax interfaces.
 */
public class SpoofaxEditor extends
    MetaBorgEditor<ISpoofaxInputUnit, ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate, IStrategoTerm> {
    public static final String id = SpoofaxPlugin.id + ".editor";

    
    @Override protected void injectGenericServices(Injector injectors) {
        final Spoofax spoofax = SpoofaxPlugin.spoofax();
        this.syntaxService = spoofax.syntaxService;
        this.analysisService = spoofax.analysisService;
        this.categorizerService = spoofax.categorizerService;
        this.stylerService = spoofax.stylerService;
        this.outlineService = spoofax.outlineService;
        this.completionService = spoofax.completionService;
        this.resolverService = spoofax.resolverService;
        this.hoverService = spoofax.hoverService;
        this.parseResultProcessor = spoofax.parseResultProcessor;
        this.analysisResultProcessor = spoofax.analysisResultProcessor;
    }
}
