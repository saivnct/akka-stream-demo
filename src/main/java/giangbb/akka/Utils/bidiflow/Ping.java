package giangbb.akka.Utils.bidiflow;

public class Ping implements Message{
    final int id;

    public Ping(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Ping) {
            return ((Ping) o).id == id;
        } else return false;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
