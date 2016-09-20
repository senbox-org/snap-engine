/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author akheckel
 */
public class MyMaskColor implements Iterable<Color>, Iterator<Color>{
    ArrayList<Color> cl = new ArrayList<Color>();
    private int index = 0;

    public MyMaskColor() {
        cl.add(Color.BLUE);
        cl.add(Color.CYAN);
        cl.add(Color.GREEN);
        cl.add(Color.YELLOW);
        cl.add(Color.ORANGE);
        cl.add(Color.RED);
        cl.add(Color.MAGENTA);
        cl.add(Color.GRAY);
        int n = cl.size();
        for (int i=0; i<n; i++) cl.add(cl.get(i).brighter());
        for (int i=0; i<n; i++) cl.add(cl.get(i).darker());
    }

    @Override
    public Iterator<Color> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Color next() {
        if (index == cl.size()-1){
            index = 0;
        }
        return cl.get(index++);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }


}
