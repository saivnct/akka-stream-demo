package giangbb.akka.model;


import java.util.Iterator;


// create an indefinite source of integer numbers
public class Ints implements Iterator<Integer> {
    private int next = 0;
    private final int max ;

    public Ints(int max) {
        this.max = max;
    }

    @Override
    public boolean hasNext() {
        return next < max;
    }

    @Override
    public Integer next() {
        return next++;
    }
}
