package org.metaborg.spoofax.eclipse.editor.outline;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistry;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class QuickOutlineHandler extends AbstractHandler {
    private static final ILogger logger = LoggerUtils.logger(QuickOutlineHandler.class);

    private final IEclipseEditorRegistry<?> editorRegistry;


    public QuickOutlineHandler() {
        final Injector injector = SpoofaxPlugin.injector();

        this.editorRegistry =
            injector.getInstance(Key.get(new TypeLiteral<IEclipseEditorRegistry<IStrategoTerm>>() {}));
    }


    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final IEclipseEditor<?> editor = editorRegistry.currentEditor();
        if(editor == null) {
            logger.debug("Cannot open quick outline; there is no Spoofax editor open currently");
            return null;
        }

        editor.openQuickOutline();

        return null;
    }
}
