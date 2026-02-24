package org.esa.snap.speclib.io.csv.util;

import java.util.List;
import java.util.Objects;


public record CsvTable(List<String> header, List<List<String>> rows) {


    public CsvTable {
        Objects.requireNonNull(header, "header must not be null");
        Objects.requireNonNull(rows, "rows must not be null");
    }
}
