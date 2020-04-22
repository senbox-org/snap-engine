package org.esa.snap.binning.operator.formatter;

public class DefaultFormatterPlugin implements FormatterPlugin {

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public Formatter create() {
        return new DefaultFormatter();
    }
}
