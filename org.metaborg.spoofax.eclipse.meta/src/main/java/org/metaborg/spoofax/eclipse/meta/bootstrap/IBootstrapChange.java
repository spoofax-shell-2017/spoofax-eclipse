package org.metaborg.spoofax.eclipse.meta.bootstrap;

public interface IBootstrapChange {
    void apply() throws Exception;

    void unapply() throws Exception;
}
