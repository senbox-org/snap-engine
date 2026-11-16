package com.bc.ceres.multilevel.support;

import com.bc.ceres.multilevel.MultiLevelModel;

import javax.media.jai.operator.FileLoadDescriptor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.nio.file.Path;

/**
 * A file based image pyramid. For each level an image file named
 * {@code <basename>.<level>.<extension>} is ecpected to exist in
 * the directory given by {@code location}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class FileMultiLevelSource extends AbstractMultiLevelSource {
    private final Path location;
    private final String basename;
    private final String extension;

    public FileMultiLevelSource(Path location, String basename, String extension, MultiLevelModel multiLevelModel) {
        super(multiLevelModel);
        this.location = location;
        this.basename = basename;
        this.extension = extension;
    }

    public Path getLocation() {
        return location;
    }

    public String getBasename() {
        return basename;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public RenderedImage createImage(int level) {
        final StringBuilder sb = new StringBuilder(basename);
        sb.append('.');
        sb.append(level);
        sb.append('.');
        sb.append(extension);
        final String fileName = sb.toString();
        final Path file = location.resolve(fileName);
        return FileLoadDescriptor.create(file.toString(), null, true, null).getRendering();
    }

    public static FileMultiLevelSource create(Path location, String extension, AffineTransform imageToModelTransform, int levelCount) {
        final DefaultMultiLevelModel defaultMultiLevelModel = new DefaultMultiLevelModel(levelCount, imageToModelTransform, null);
        final FileMultiLevelSource levelImageSource = new FileMultiLevelSource(location, location.getFileName().toString(), extension, defaultMultiLevelModel);
        Rectangle2D modelBounds = DefaultMultiLevelModel.getModelBounds(imageToModelTransform, levelImageSource.getImage(0));
        defaultMultiLevelModel.setModelBounds(modelBounds);
        return levelImageSource;
    }
}
