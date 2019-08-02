package giangbb.akka.actor.materializerLifecycle;

import akka.actor.AbstractActor;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;


public class RunWithMyself extends AbstractActor {
    //bound the stream’s lifecycle to the surrounding actor’s lifecycle
    //This is a very useful technique if the stream is closely related to the actor, e.g. when the actor represents a user or other entity, that we continuously query using the created stream – and it would not make sense to keep the stream alive when the actor has terminated already
    //The streams termination will be signalled by an “Abrupt termination exception” signaled by the stream
    ActorMaterializer materializer = ActorMaterializer.create(context());

    @Override
    public void preStart() throws Exception {
        super.preStart();
        //start a stream forever
        Source.repeat("RunWithMyself - Hello")
                .runWith(
                        Sink.onComplete(
                                tryDone -> {
                                    System.out.println("RunWithMyself - Terminated stream: "+ tryDone);
                                }
                        ),materializer
                );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(String.class, str -> {
            System.out.println("RunWithMyself recieved: "+str);
            if (str.equals("stop")){
                //terminate this actor
                // this WILL terminate the above stream as well, because the materializer is created in this context
                context().stop(self());
            }
        }).build();
    }
}
