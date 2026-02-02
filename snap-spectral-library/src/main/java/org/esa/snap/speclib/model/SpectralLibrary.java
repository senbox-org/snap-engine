package org.esa.snap.speclib.model;

import java.util.*;


public class SpectralLibrary {


    private final UUID id;
    private String name;
    private final List<SpectralProfile> profiles;


    public SpectralLibrary(UUID id, String name, List<SpectralProfile> profiles) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");

        Objects.requireNonNull(profiles, "profiles must not be null");
        this.profiles = new ArrayList<>(profiles.size());
        for (SpectralProfile p : profiles) {
            addProfile(p);
        }
    }

    public SpectralLibrary(UUID id, String name) {
        this(id, name, List.of());
    }

    public static SpectralLibrary create(String name) {
        return new SpectralLibrary(UUID.randomUUID(), name);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public List<SpectralProfile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    public int size() {
        return profiles.size();
    }

    public void addProfile(SpectralProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        UUID pid = profile.getId();
        if (containsProfile(pid)) {
            throw new IllegalArgumentException("profile with id already exists: " + pid);
        }
        profiles.add(profile);
    }

    public boolean removeProfile(UUID profileId) {
        Objects.requireNonNull(profileId, "profileId must not be null");
        return profiles.removeIf(p -> profileId.equals(p.getId()));
    }

    public boolean containsProfile(UUID profileId) {
        Objects.requireNonNull(profileId, "profileId must not be null");
        for (SpectralProfile p : profiles) {
            if (profileId.equals(p.getId())) {
                return true;
            }
        }
        return false;
    }

    public Optional<SpectralProfile> findProfile(UUID profileId) {
        Objects.requireNonNull(profileId, "profileId must not be null");
        for (SpectralProfile p : profiles) {
            if (profileId.equals(p.getId())) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

}
