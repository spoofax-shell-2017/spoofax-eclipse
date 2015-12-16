package org.metaborg.spoofax.eclipse.job;

/**
 * Collection of global scheduling rules.
 */
public class GlobalSchedulingRules {
    private final LockRule startupLock = new LockRule("Startup write lock");
    private final LockRule languageServiceLock = new LockRule("Language service lock");
    private final LockRule strategoLock = new LockRule("Stratego lock");


    /**
     * Returns the startup read/write lock rule, acquired during start up to load all languages in the workspace
     * dynamically. Use {@link #startupReadLock()} to get a read-only lock rule.
     * 
     * @return Startup read/write lock scheduling rule.
     */
    public LockRule startupWriteLock() {
        return startupLock;
    }

    /**
     * Returns a new read-only lock rule, which blocks during startup, and never blocks after that. Use to schedule jobs
     * after startup, when all languages in the workspace have been loaded dynamically.
     * 
     * @return New startup read-only lock scheduling rule.
     */
    public ReadLockRule startupReadLock() {
        return new ReadLockRule(startupLock, "Startup read lock");
    }

    /**
     * Returns the read/write lock rule for exclusive access to the language service, which is not thread-safe. Use to
     * schedule jobs that require the language service.
     * 
     * @return Language service read/write lock scheduling rule.
     */
    public LockRule languageServiceLock() {
        return languageServiceLock;
    }

    /**
     * Returns the read/write lock rule for exclusive access to Stratego calls, which are (apparently) not thread-safe.
     * Use to schedule jobs that do Stratego calls.
     * 
     * @return Stratego read/write lock scheduling rule.
     */
    public LockRule strategoLock() {
        return strategoLock;
    }
}
