package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.api.SpectralProfileExtractor;
import org.esa.snap.speclib.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SpectralLibraryServiceImpl implements SpectralLibraryService {


    private final SpectralProfileExtractor extractor;
    private final ConcurrentHashMap<UUID, SpectralLibrary> libraries = new ConcurrentHashMap<>();


    public SpectralLibraryServiceImpl() {
        this(new SpectralProfileExtractorImpl(new SpectralSampleProviderImpl()));
    }

    public SpectralLibraryServiceImpl(SpectralProfileExtractor extractor) {
        this.extractor = Objects.requireNonNull(extractor, "extractor must not be null");
    }


    @Override
    public SpectralLibrary createLibrary(String name, SpectralAxis axis, String defaultYUnit) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(axis, "axis must not be null");
        SpectralLibrary lib = SpectralLibrary.create(name, axis, defaultYUnit);
        libraries.put(lib.getId(), lib);
        return lib;
    }

    @Override
    public Optional<SpectralLibrary> getLibrary(UUID libraryId) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        return Optional.ofNullable(libraries.get(libraryId));
    }

    @Override
    public List<SpectralLibrary> listLibraries() {
        List<SpectralLibrary> snapshot = new ArrayList<>(libraries.values());
        snapshot.sort(Comparator
                .comparing(SpectralLibrary::getName, Comparator.nullsFirst(String::compareTo))
                .thenComparing(SpectralLibrary::getId));
        return Collections.unmodifiableList(snapshot);
    }

    @Override
    public boolean deleteLibrary(UUID libraryId) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        return libraries.remove(libraryId) != null;
    }

    @Override
    public Optional<SpectralLibrary> renameLibrary(UUID libraryId, String newName) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        Objects.requireNonNull(newName, "newName must not be null");

        SpectralLibrary updated = libraries.computeIfPresent(libraryId, (id, lib) -> lib.withName(newName));
        return Optional.ofNullable(updated);
    }

    @Override
    public void addProfile(UUID libraryId, SpectralProfile profile) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");

        SpectralLibrary updated = libraries.compute(libraryId, (id, lib) -> {
            if (lib == null) {
                throw new NoSuchElementException("library not found: " + libraryId);
            }
            return lib.withProfileAdded(profile);
        });

        Objects.requireNonNull(updated);
    }

    @Override
    public boolean removeProfile(UUID libraryId, UUID profileId) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        Objects.requireNonNull(profileId, "profileId must not be null");

        final boolean[] removed = {false};
        libraries.computeIfPresent(libraryId, (id, lib) -> {
            SpectralLibrary next = lib.withProfileRemoved(profileId);
            removed[0] = (next != lib);
            return next;
        });
        return removed[0];
    }

    @Override
    public Optional<SpectralProfile> findProfile(UUID libraryId, UUID profileId) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        Objects.requireNonNull(profileId, "profileId must not be null");

        SpectralLibrary lib = libraries.get(libraryId);
        if (lib == null) {
            return Optional.empty();
        }
        return lib.findProfile(profileId);
    }

    @Override
    public Optional<SpectralProfile> extractProfile(String name,
                                                    SpectralAxis axis,
                                                    List<Band> bands,
                                                    int x,
                                                    int y,
                                                    int level,
                                                    String yUnit,
                                                    String productId) {
        return extractor.extract(name, axis, bands, x, y, level, yUnit, productId);
    }

    @Override
    public void addAttributeToLibrary(UUID libraryId, AttributeDef def, AttributeValue fillValue) {
        Objects.requireNonNull(libraryId); Objects.requireNonNull(def); Objects.requireNonNull(fillValue);

        libraries.compute(libraryId, (id, lib) -> {
            if (lib == null) {
                throw new NoSuchElementException("library not found: " + libraryId);
            }

            AttributeSchema newSchema = new AttributeSchema(lib.getSchema().asMap());
            newSchema.put(def);

            List<SpectralProfile> newProfiles = lib.getProfiles().stream()
                    .map(p -> p.getAttribute(def.getKey()).isPresent() ? p : p.withAttribute(def.getKey(), fillValue))
                    .toList();

            return new SpectralLibrary(lib.getId(), lib.getName(), lib.getAxis(),
                    lib.getDefaultYUnit().orElse(null), newProfiles, newSchema);
        });
    }
}
