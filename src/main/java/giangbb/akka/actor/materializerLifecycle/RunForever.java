package giangbb.akka.actor.materializerLifecycle;

import akka.actor.AbstractActor;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

public class RunForever extends AbstractActor {
    final Materializer materializer;

    public RunForever(Materializer materializer) {
        this.materializer = materializer;
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        //start a stream forever
        Source.repeat("RunWithMyself - Hello")
                .runWith(
                        Sink.onComplete(
                                tryDone -> {
                                    System.out.println("RunForever - Terminated stream: "+ tryDone);
                                }
                        ),materializer
                );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(String.class, str -> {
            System.out.println("RunForever recieved: "+str);
            if (str.equals("stop")){
                // this will NOT terminate the stream (it's bound to the system!)
                context().stop(self());
            }
        }).build();
    }
}
