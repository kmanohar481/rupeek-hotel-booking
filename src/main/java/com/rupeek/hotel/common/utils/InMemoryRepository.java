package com.rupeek.hotel.common.utils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Shared plumbing for the in-memory repositories.
 */
public abstract class InMemoryRepository<T> {

    private final Map<String, T> store = new ConcurrentHashMap<>();
    private final Function<T, String> idOf;

    protected InMemoryRepository(Function<T, String> idOf) {
        this.idOf = idOf;
    }

    public T save(T entity) {
        store.put(idOf.apply(entity), entity);
        return entity;
    }

    public Optional<T> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    protected Stream<T> stream() {
        return store.values().stream();
    }
}
