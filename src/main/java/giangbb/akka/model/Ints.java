package giangbb.akka.model;


import java.util.Iterator;


// create an indefinite source of integer numbers
public class Ints implements Iterator<Integer> {
    private int next = 0;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Integer next() {
        return next++;
    }
}
