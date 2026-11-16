package com.bc.ceres.swing.demo;

import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.support.DefaultFigureFactory;

import javax.swing.JOptionPane;
import java.nio.file.Path;

public class FigureEditorDemo extends FigureEditorApp {

    public static void main(String[] args) {
        run(new FigureEditorDemo());
    }

    @Override
    protected FigureFactory getFigureFactory() {
        return new DefaultFigureFactory();
    }

    @Override
    protected void loadFigureCollection(Path file, FigureCollection figureCollection) {
        JOptionPane.showMessageDialog(getFrame(), "Not implemented in demo.");
    }

    @Override
    protected void storeFigureCollection(FigureCollection figureCollection, Path file) {
        JOptionPane.showMessageDialog(getFrame(), "Not implemented in demo.");
    }
}
