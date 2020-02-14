package org.esa.snap.engine_utilities.util;

/**
 * Created by jcoravu on 14/2/2020.
 */
public class Pair<First, Second> {

    private final First first;
    private final Second second;

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
}
