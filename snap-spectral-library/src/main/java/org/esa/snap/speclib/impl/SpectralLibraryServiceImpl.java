package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.api.SpectralLibraryService;
import org.esa.snap.speclib.api.SpectralProfileExtractor;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SpectralLibraryServiceImpl implements SpectralLibraryService {


    private final SpectralProfileExtractor extractor;
    private final ConcurrentHashMap<UUID, SpectralLibrary> libraries = new ConcurrentHashMap<>();


    public SpectralLibraryServiceImpl() {
        this(new SpectralProfileExtractorImpl( new SpectralSampleProviderImpl()));
    }

    public SpectralLibraryServiceImpl(SpectralProfileExtractor extractor) {
        this.extractor = Objects.requireNonNull(extractor, "extractor must not be null");
    }


    @Override
    public SpectralLibrary createLibrary(String name) {
        Objects.requireNonNull(name, "name must not be null");
        SpectralLibrary lib = SpectralLibrary.create(name);
        this.libraries.put(lib.getId(), lib);
        return lib;
    }

    @Override
    public Optional<SpectralLibrary> getLibrary(UUID libraryId) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        return Optional.ofNullable(this.libraries.get(libraryId));
    }

    @Override
    public List<SpectralLibrary> listLibraries() {
        List<SpectralLibrary> snapshot = new ArrayList<>(this.libraries.values());
        snapshot.sort(Comparator
                .comparing(SpectralLibrary::getName, Comparator.nullsFirst(String::compareTo))
                .thenComparing(SpectralLibrary::getId));
        return Collections.unmodifiableList(snapshot);
    }

    @Override
    public boolean deleteLibrary(UUID libraryId) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        return this.libraries.remove(libraryId) != null;
    }

    @Override
    public void addProfile(UUID libraryId, SpectralProfile profile) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");

        SpectralLibrary lib = this.libraries.get(libraryId);
        if (lib == null) {
            throw new NoSuchElementException("library not found: " + libraryId);
        }
        synchronized (lib) {
            lib.addProfile(profile);
        }
    }

    @Override
    public boolean removeProfile(UUID libraryId, UUID profileId) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        Objects.requireNonNull(profileId, "profileId must not be null");

        SpectralLibrary lib = this.libraries.get(libraryId);
        if (lib == null) {
            return false;
        }
        synchronized (lib) {
            return lib.removeProfile(profileId);
        }
    }

    @Override
    public Optional<SpectralProfile> findProfile(UUID libraryId, UUID profileId) {
        Objects.requireNonNull(libraryId, "libraryId must not be null");
        Objects.requireNonNull(profileId, "profileId must not be null");

        SpectralLibrary lib = this.libraries.get(libraryId);
        if (lib == null) {
            return Optional.empty();
        }
        synchronized (lib) {
            return lib.findProfile(profileId);
        }
    }

    @Override
    public Optional<SpectralProfile> extractProfile(String name, List<Band> bands, int x, int y, int level, String unit) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(bands, "bands must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        return this.extractor.extract(name, bands, x, y, level, unit);
    }
}
