package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
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
    public List<SpectralProfile> extractProfiles(String baseName,
                                                 SpectralAxis axis,
                                                 List<Band> bands,
                                                 List<PixelPos> pixels,
                                                 int level,
                                                 String yUnit,
                                                 String productId) {

        Objects.requireNonNull(baseName, "baseName must not be null");
        Objects.requireNonNull(axis, "axis must not be null");
        Objects.requireNonNull(bands, "bands must not be null");
        Objects.requireNonNull(pixels, "pixels must not be null");
        Objects.requireNonNull(yUnit, "yUnit must not be null");
        Objects.requireNonNull(productId, "productId must not be null");

        if (pixels.isEmpty() || bands.isEmpty()) {
            return List.of();
        }

        return extractor.extractBulk(baseName, axis, bands, pixels, level, yUnit, productId);
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

    @Override
    public boolean renameProfile(UUID libraryId, UUID profileId, String newName) {
        Objects.requireNonNull(libraryId); Objects.requireNonNull(profileId); Objects.requireNonNull(newName);
        final boolean[] changed = {false};

        libraries.computeIfPresent(libraryId, (id, lib) -> {
            var copy = new ArrayList<>(lib.getProfiles());
            for (int i=0;i<copy.size();i++) {
                var p = copy.get(i);
                if (profileId.equals(p.getId())) {
                    if (!newName.equals(p.getName())) {
                        copy.set(i, new SpectralProfile(p.getId(), newName, p.getSignature(), p.getAttributes(), p.getSourceRef().orElse(null)));
                        changed[0] = true;
                    }
                    break;
                }
            }
            return changed[0]
                    ? new SpectralLibrary(lib.getId(), lib.getName(), lib.getAxis(), lib.getDefaultYUnit().orElse(null), copy, lib.getSchema())
                    : lib;
        });
        return changed[0];
    }

    @Override
    public boolean setProfileAttribute(UUID libraryId, UUID profileId, String key, AttributeValue value) {
        Objects.requireNonNull(libraryId); Objects.requireNonNull(profileId); Objects.requireNonNull(key); Objects.requireNonNull(value);
        final boolean[] changed = {false};

        libraries.computeIfPresent(libraryId, (id, lib) -> {
            var copy = new ArrayList<>(lib.getProfiles());
            for (int i=0;i<copy.size();i++) {
                var p = copy.get(i);
                if (profileId.equals(p.getId())) {
                    copy.set(i, p.withAttribute(key, value));
                    changed[0] = true;
                    break;
                }
            }
            return changed[0]
                    ? new SpectralLibrary(lib.getId(), lib.getName(), lib.getAxis(), lib.getDefaultYUnit().orElse(null), copy, lib.getSchema())
                    : lib;
        });
        return changed[0];
    }
}
