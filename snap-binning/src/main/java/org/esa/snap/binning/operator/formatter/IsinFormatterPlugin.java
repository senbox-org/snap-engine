package org.esa.snap.binning.operator.formatter;

public class IsinFormatterPlugin implements FormatterPlugin{

    @Override
    public String getName() {
        return "isin";
    }

    @Override
    public Formatter create() {
        return new IsinFormatter();
    }
}
