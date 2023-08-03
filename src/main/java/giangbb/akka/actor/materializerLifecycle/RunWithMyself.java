package giangbb.akka.actor.materializerLifecycle;

import akka.actor.AbstractActor;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RunWithMyself extends AbstractActor {
    private static final Logger logger = LogManager.getLogger(RunWithMyself.class);
    //bound the stream’s lifecycle to the surrounding actor’s lifecycle
    //This is a very useful technique if the stream is closely related to the actor, e.g. when the actor represents a user or other entity, that we continuously query using the created stream – and it would not make sense to keep the stream alive when the actor has terminated already
    //The streams termination will be signalled by an “Abrupt termination exception” signaled by the stream
    Materializer materializer = Materializer.createMaterializer(context());   // -> Stream run by this materializer will be terminated when this actor is terminated

    @Override
    public void preStart() throws Exception {
        super.preStart();
        logger.debug("RunWithMyself - preStart");
        //start a stream forever
        Source.repeat("RunWithMyself - Hello")
                .runWith(
                        Sink.onComplete(
                                tryDone -> {
                                    logger.debug("RunWithMyself - Terminated stream: {}", tryDone);
                                }
                        ),materializer
                );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(String.class, str -> {
            logger.debug("RunWithMyself recieved: {}", str);
            if (str.equals("stop")){
                //terminate this actor
                // this WILL terminate the above stream as well, because the materializer is created in this context
                context().stop(self());
            }
        }).build();
    }
}
