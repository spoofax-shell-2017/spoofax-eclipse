package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.metaborg.core.MetaborgException;

public interface IBootstrapChange {
    void apply() throws MetaborgException;

    void unapply() throws MetaborgException;
}
