package org.metaborg.spoofax.eclipse.util;

import org.eclipse.core.runtime.IStatus;

public class ValueStatus implements IStatus {
    private final Object value;


    public ValueStatus(Object value) {
        this.value = value;
    }


    public Object getValue() {
        return value;
    }

    @Override public IStatus[] getChildren() {
        return null;
    }

    @Override public int getCode() {
        return 0;
    }

    @Override public Throwable getException() {
        return null;
    }

    @Override public String getMessage() {
        return null;
    }

    @Override public String getPlugin() {
        return null;
    }

    @Override public int getSeverity() {
        return IStatus.OK;
    }

    @Override public boolean isMultiStatus() {
        return false;
    }

    @Override public boolean isOK() {
        return true;
    }

    @Override public boolean matches(int severityMask) {
        return true;
    }
}
