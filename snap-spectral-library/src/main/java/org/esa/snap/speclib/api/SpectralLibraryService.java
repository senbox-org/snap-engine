package org.esa.snap.speclib.api;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.speclib.model.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface SpectralLibraryService {


    SpectralLibrary createLibrary(String name, SpectralAxis axis, String defaultYUnit);
    Optional<SpectralLibrary> getLibrary(UUID libraryId);
    List<SpectralLibrary> listLibraries();
    boolean deleteLibrary(UUID libraryId);
    Optional<SpectralLibrary> renameLibrary(UUID libraryId, String newName);

    void addProfile(UUID libraryId, SpectralProfile profile);
    record BulkAddResult(int added, int skippedExisting) {}
    BulkAddResult addProfiles(UUID libraryId, List<SpectralProfile> profiles);

    boolean removeProfile(UUID libraryId, UUID profileId);
    Optional<SpectralProfile> findProfile(UUID libraryId, UUID profileId);

    Optional<SpectralProfile> extractProfile(String name, SpectralAxis axis, List<Band> bands, int x, int y, int level, String yUnit, String productId);

    List<SpectralProfile> extractProfiles(String baseName, SpectralAxis axis, List<Band> bands, List<PixelPos> pixels, int level, String yUnit, String productId);

    void addAttributeToLibrary(UUID libraryId, AttributeDef def, AttributeValue valueForExistingProfilesIfMissing);
    boolean renameProfile(UUID libraryId, UUID profileId, String newName);
    boolean setProfileAttribute(UUID libraryId, UUID profileId, String key, AttributeValue value);
}
