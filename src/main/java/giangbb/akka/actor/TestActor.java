package giangbb.akka.actor;

import akka.actor.AbstractActor;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

public class TestActor extends AbstractActor {
    final Materializer materializer;

    public TestActor(Materializer materializer) {
        this.materializer = materializer;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Object.class, obj -> {
            System.out.println("TestActor recieved: "+obj);
        }).build();
    }
}
