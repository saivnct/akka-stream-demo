package giangbb.akka.Utils.bidiflow;

public class Pong implements Message{
    final int id;

    public Pong(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pong) {
            return ((Pong) o).id == id;
        } else return false;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
