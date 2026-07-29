package org.esa.snap.core.dataio;

import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

public class ProductReaderUtilsTest {

    @Test
    public void testThatIO_TypesContains_Path_File_and_String_classes() {
        assertThat(ProductReaderUtils.IO_TYPES, is(new Class[]{Path.class, File.class, String.class}));
    }

    @Test
    public void testThatInputObjectsAreConvertedToPath_String_to_Path() {
        Path path = ProductReaderUtils.convertToPath("/some/path/Filename"); // StringCase
        assertThat(path, is(notNullValue()));
        assertThat(path.toString(), matchesPattern("[/\\\\]{1}some[\\\\/]{1}path[\\\\/]{1}Filename"));
    }

    @Test
    public void testThatInputObjectsAreConvertedToPath_File_to_Path() {
        Path path = ProductReaderUtils.convertToPath(new File("/some/other/path/Filename"));
        assertThat(path, is(notNullValue()));
        assertThat(path.toString(), matchesPattern("[/\\\\]{1}some[\\\\/]{1}other[\\\\/]{1}path[\\\\/]{1}Filename"));
    }

    @Test
    public void testThatInputObjectsAreConvertedToPath_Path_to_Path() {
        Path path = ProductReaderUtils.convertToPath(Paths.get("/some/path/Pathname"));
        assertThat(path, is(notNullValue()));
        assertThat(path.toString(), matchesPattern("[/\\\\]{1}some[\\\\/]{1}path[\\\\/]{1}Pathname"));
    }

    @Test
    public void testThatInvalidObjectsToBeConverted_ReturnNull() {
        Path path = ProductReaderUtils.convertToPath(new Color(2, 3, 4));
        assertThat(path, is(nullValue()));
    }
}