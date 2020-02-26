package org.esa.snap.binning.operator.formatter;

public interface FormatterPlugin {

    String getName();

    Formatter create();
}
