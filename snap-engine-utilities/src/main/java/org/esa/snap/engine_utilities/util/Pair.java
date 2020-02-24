package org.esa.snap.engine_utilities.util;

/**
 * Created by jcoravu on 14/2/2020.
 */
public class Pair<First, Second> {

    private First first;
    private Second second;

    public Pair(First first, Second second) {
        this.first = first;
        this.second = second;
    }

    public First getFirst() {
        return first;
    }

    public Second getSecond() {
        return second;
    }

    public void setFirst(First first) {
        this.first = first;
    }

    public void setSecond(Second second) {
        this.second = second;
    }
}
