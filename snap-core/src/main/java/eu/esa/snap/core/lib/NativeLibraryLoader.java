package eu.esa.snap.core.lib;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * NativeLibraryLoader class used on auxdata/native-libraries/NativeLibraryLoader.jar
 *
 * Note. The name and access modifier are changed to prevent loading by application Classloader, to be loaded by custom Classloader 'NativeLibraryClassLoader' from NativeLibraryLoader.jar
 */
final class NativeLibraryLoader1 {

    /**
     * Loads native library on current ClassLoader
     *
     * @param nativeLibraryFilePath the path for native library
     */
    public static void loadNativeLibrary(Path nativeLibraryFilePath) {
        System.load(nativeLibraryFilePath.toAbsolutePath().toString());
    }

    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                Path p = Paths.get(args[0]);
                if (Files.exists(p)) {
                    NativeLibraryLoader1.loadNativeLibrary(p);
                    System.out.println("success!");
                } else {
                    System.err.println("fail: not found!");
                }
            } else {
                System.err.println("fail: missing path!");
            }
        } catch (Exception e) {
            System.err.println("fail: error!\n" + e.getMessage());
        }
    }
}

/*
    Build the NativeLibraryLoader.jar (from directory of NativeLibraryLoader.java):
    I. Prepare the source
    1) rename class from NativeLibraryLoader1 to NativeLibraryLoader
    2) add 'public' access modifier to the class
    II. Build the JAR using Command Prompt
    1) mkdir "./tmp"
    2) javac -d "./tmp" ./NativeLibraryLoader.java
    3) cd "./tmp"
    4) jar cf "../../../../../../../resources/auxdata/NativeLibraryLoader.jar" *
    5) cd "../"
    6) rmdir /q/s "./tmp"
 */