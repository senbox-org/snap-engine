package org.esa.snap.speclib.api;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface SpectralLibraryService {


    SpectralLibrary createLibrary(String name);
    Optional<SpectralLibrary> getLibrary(UUID libraryId);
    List<SpectralLibrary> listLibraries();
    boolean deleteLibrary(UUID libraryId);
    void addProfile(UUID libraryId, SpectralProfile profile);
    boolean removeProfile(UUID libraryId, UUID profileId);
    Optional<SpectralProfile> findProfile(UUID libraryId, UUID profileId);

    Optional<SpectralProfile> extractProfile(String name, List<Band> bands, int x, int y, int level, String unit);
}
