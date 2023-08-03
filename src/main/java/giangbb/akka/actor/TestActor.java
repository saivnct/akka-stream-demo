package giangbb.akka.actor;

import akka.actor.AbstractActor;

public class TestActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Object.class, obj -> {
            System.out.println("TestActor recieved: "+obj);
        }).build();
    }
}
