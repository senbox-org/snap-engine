package eu.esa.snap.sttm;

public class STTMExtractor {

    public static void main(String[] args) {

    }

    static void validateInput(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Must provide at least one project path.");
        }
    }
}
