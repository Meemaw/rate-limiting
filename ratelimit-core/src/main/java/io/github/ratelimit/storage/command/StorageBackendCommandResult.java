package io.github.ratelimit.storage.command;

import java.io.Serializable;

public class StorageBackendCommandResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final StorageBackendCommandResult<?> NOT_FOUND = new StorageBackendCommandResult<>(null, false);

    private T data;
    private boolean entriesFound;

    public StorageBackendCommandResult(T data, boolean entriesFound) {
        this.data = data;
        this.entriesFound = entriesFound;
    }

    public static <R> StorageBackendCommandResult<R> success(R data) {
        return new StorageBackendCommandResult<>(data, true);
    }

    @SuppressWarnings("unchecked")
    public static <R> StorageBackendCommandResult<R> entriesNotFound() {
        return (StorageBackendCommandResult<R>) NOT_FOUND;
    }

    public T getData() {
        return data;
    }

    public boolean containsEntries() {
        return entriesFound;
    }

}
