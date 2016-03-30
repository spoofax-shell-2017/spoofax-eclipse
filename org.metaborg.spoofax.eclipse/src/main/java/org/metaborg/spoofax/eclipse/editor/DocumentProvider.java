package org.metaborg.spoofax.eclipse.editor;

import java.io.InputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class DocumentProvider extends FileDocumentProvider {
    private static final ILogger logger = LoggerUtils.logger(DocumentProvider.class);

    private final IEclipseResourceService resourceService;


    public DocumentProvider(IEclipseResourceService resourceService) {
        this.resourceService = resourceService;
    }


    @Override protected IDocument createDocument(Object element) throws CoreException {
        final IDocument superDocument = super.createDocument(element);
        if(superDocument != null) {
            return superDocument;
        }

        if(element instanceof IEditorInput) {
            final IDocument document = createEmptyDocument();
            final IEditorInput input = (IEditorInput) element;
            final FileObject resource = resourceService.resolve(input);
            if(resource == null) {
                final String message =
                    logger.format("Cannot create document for input {}, could not resolve input to file object",
                        element);
                logger.error(message);
                throw new CoreException(StatusUtils.error(message));
            }

            try {
                final InputStream stream = resource.getContent().getInputStream();
                String encoding = getEncoding(element);
                if(encoding == null) {
                    encoding = getDefaultEncoding();
                }
                setDocumentContent(document, stream, encoding);
                setupDocument(element, document);
                return document;
            } catch(FileSystemException e) {
                final String message = logger.format("Cannot create document for input {}", element);
                logger.error(message, e);
                throw new CoreException(StatusUtils.error(message, e));
            }
        }

        return null;
    }

    @Override protected IDocument createEmptyDocument() {
        return super.createEmptyDocument();
    }
}
