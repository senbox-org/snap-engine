package org.esa.snap.speclib.model;

import java.util.*;


public class SpectralLibrary {


    private final UUID id;
    private final String name;
    private final SpectralAxis axis;
    private final String defaultYUnit;
    private final List<SpectralProfile> profiles;
    private final AttributeSchema schema;


    public SpectralLibrary(UUID id,
                           String name,
                           SpectralAxis axis,
                           String defaultYUnit,
                           List<SpectralProfile> profiles,
                           AttributeSchema schema) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.axis = Objects.requireNonNull(axis, "axis must not be null");
        this.defaultYUnit = defaultYUnit;
        this.schema = Objects.requireNonNull(schema, "schema must not be null");

        Objects.requireNonNull(profiles, "profiles must not be null");
        this.profiles = Collections.unmodifiableList(new ArrayList<>(profiles));

        for (SpectralProfile p : this.profiles) {
            if (p.size() != axis.size()) {
                throw new IllegalArgumentException("profile size does not match library axis: " + p.getName());
            }
        }
    }


    public static SpectralLibrary create(String name, SpectralAxis axis, String defaultYUnit) {
        return new SpectralLibrary(UUID.randomUUID(), name, axis, defaultYUnit, List.of(), new AttributeSchema());
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SpectralAxis getAxis() {
        return axis;
    }

    public Optional<String> getDefaultYUnit() {
        return Optional.ofNullable(defaultYUnit);
    }

    public List<SpectralProfile> getProfiles() {
        return profiles;
    }

    public AttributeSchema getSchema() {
        return schema;
    }

    public int size() {
        return profiles.size();
    }

    public SpectralLibrary withName(String newName) {
        return new SpectralLibrary(this.id, Objects.requireNonNull(newName), this.axis, this.defaultYUnit, this.profiles, this.schema);
    }

    public SpectralLibrary withProfileAdded(SpectralProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        if (profile.size() != axis.size()) {
            throw new IllegalArgumentException("profile size does not match library axis");
        }
        for (SpectralProfile p : profiles) {
            if (p.getId().equals(profile.getId())) {
                throw new IllegalArgumentException("profile with id already exists: " + profile.getId());
            }
        }
        ArrayList<SpectralProfile> copy = new ArrayList<>(profiles);
        copy.add(profile);

        AttributeSchema newSchema = new AttributeSchema(schema.asMap());
        newSchema.inferFromAttributes(profile.getAttributes());

        return new SpectralLibrary(this.id, this.name, this.axis, this.defaultYUnit, copy, newSchema);
    }

    public SpectralLibrary withProfileRemoved(UUID profileId) {
        Objects.requireNonNull(profileId, "profileId must not be null");
        ArrayList<SpectralProfile> copy = new ArrayList<>(profiles);
        boolean removed = copy.removeIf(p -> profileId.equals(p.getId()));
        if (!removed) {
            return this;
        }

        return new SpectralLibrary(this.id, this.name, this.axis, this.defaultYUnit, copy, this.schema);
    }

    public Optional<SpectralProfile> findProfile(UUID profileId) {
        Objects.requireNonNull(profileId, "profileId must not be null");
        for (SpectralProfile p : profiles) {
            if (profileId.equals(p.getId())) return Optional.of(p);
        }
        return Optional.empty();
    }

}
