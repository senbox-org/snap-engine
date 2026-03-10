package com.bc.ceres.util;

import java.lang.ref.Cleaner;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Central registry to (a) deterministically release resources via cleanup(owner),
 * and (b) provide a GC fallback via {@link Cleaner} if close/cleanup was forgotten.
 * Notes:
 * - The fallback is NOT deterministic and does NOT help on hard process termination.
 * - Cleanup actions must be idempotent (may run via cleanup(owner) or via Cleaner thread).
 */
public final class CleanerRegistry {


    private static final CleanerRegistry INSTANCE = new CleanerRegistry();
    private static final Logger LOG = Logger.getLogger(CleanerRegistry.class.getName());

    private final Cleaner cleaner;
    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
    private final Map<IdentityKey, Deque<Cleaner.Cleanable>> cleanablesByOwner = new HashMap<>();


    private CleanerRegistry() {
        this(Cleaner.create());
    }

    private CleanerRegistry(Cleaner cleaner) {
        this.cleaner = Objects.requireNonNull(cleaner, "cleaner");
    }

    // package-private for tests
    static CleanerRegistry newForTest() {
        return new CleanerRegistry();
    }


    public static CleanerRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a cleanup state for the given owner.
     * <p>
     * The cleanup state may run either when {@link #cleanup(Object)} is called explicitly
     * or when the owner becomes unreachable and the cleaner performs fallback cleanup.
     * <p>
     * The given {@link CleanUpState} must be a top-level class or a static nested class.
     * It must not be a lambda, anonymous class, local class, or hold a direct reference
     * to the owner.
     * <p>
     * Cleanup implementations must be idempotent, because cleanup may be triggered
     * deterministically via {@link #cleanup(Object)} or nondeterministically by the cleaner.
     *
     * @param owner the object whose lifecycle is being tracked
     * @param cleanUpState the cleanup state to execute at most once
     * @return the corresponding cleanable handle
     * @throws NullPointerException if {@code owner} or {@code cleanupState} is {@code null}
     * @throws IllegalArgumentException if {@code cleanupState} violates the registry constraints
     */
    public Cleaner.Cleanable register(Object owner, CleanUpState cleanUpState) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(cleanUpState, "CleanUpState");

        validateCleanupState(owner, cleanUpState);

        final Runnable safeAction = () -> {
            try {
                cleanUpState.run();
            } catch (Throwable t) {
                LOG.log(Level.FINE, "Cleanup action failed (ignored).", t);
            }
        };

        final Cleaner.Cleanable cleanable = cleaner.register(owner, safeAction);

        synchronized (this) {
            expungeStaleEntriesLocked();
            final IdentityKey key = new IdentityKey(owner, refQueue);
            cleanablesByOwner.computeIfAbsent(key, k -> new ArrayDeque<>()).addFirst(cleanable);
        }
        return cleanable;
    }


    /**
     * Deterministically runs and deregisters all cleanup actions registered for the given owner.
     * Safe to call multiple times.
     */
    public void cleanup(Object owner) {
        Objects.requireNonNull(owner, "owner");
        final Deque<Cleaner.Cleanable> toRun;

        synchronized (this) {
            expungeStaleEntriesLocked();
            toRun = cleanablesByOwner.remove(new IdentityKey(owner, null));
        }

        if (toRun == null || toRun.isEmpty()) {
            return;
        }

        for (Cleaner.Cleanable c : toRun) {
            try {
                c.clean();
            } catch (Throwable t) {
                LOG.log(Level.FINE, "Cleaner.clean() failed (ignored).", t);
            }
        }
    }


    private void expungeStaleEntriesLocked() {
        IdentityKey stale;
        while ((stale = (IdentityKey) refQueue.poll()) != null) {
            cleanablesByOwner.remove(stale);
        }
    }


    private static void validateCleanupState(Object owner, CleanUpState cleanUpState) {
        Class<?> stateClass = cleanUpState.getClass();

        if (stateClass.isSynthetic()) {
            throw new IllegalArgumentException("CleanUpState must not be a lambda or synthetic class: " + stateClass.getName());
        }
        if (stateClass.isAnonymousClass()) {
            throw new IllegalArgumentException("CleanUpState must not be an anonymous class: " + stateClass.getName());
        }
        if (stateClass.isLocalClass()) {
            throw new IllegalArgumentException("CleanUpState must not be a local class: " + stateClass.getName());
        }
        if (stateClass.isMemberClass() && !Modifier.isStatic(stateClass.getModifiers())) {
            throw new IllegalArgumentException("CleanUpState must be a static nested class or top-level class: " + stateClass.getName());
        }
        if (holdsDirectOwnerReference(owner, cleanUpState)) {
            throw new IllegalArgumentException("CleanUpState must not hold a direct reference to the owner: " + stateClass.getName());
        }
    }

    private static boolean holdsDirectOwnerReference(Object owner, CleanUpState cleanUpState) {
        Class<?> type = cleanUpState.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(cleanUpState);
                    if (value == owner) {
                        return true;
                    }
                } catch (RuntimeException | IllegalAccessException e) {
                    throw new IllegalArgumentException(
                            "Failed to inspect CleanUpState field '" + field.getName() + "' in " + cleanUpState.getClass().getName(), e);
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    /**
     * Weak identity key (does not rely on equals/hashCode of owner objects).
     */
    private static final class IdentityKey extends WeakReference<Object> {
        private final int hash;

        IdentityKey(Object referent, ReferenceQueue<Object> q) {
            super(referent, q);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ( !(obj instanceof IdentityKey other)) {
                return false;
            }

            Object a = get();
            Object b = other.get();

            if (a == null || b == null) {
                return false;
            }
            return a == b;
        }
    }
}
