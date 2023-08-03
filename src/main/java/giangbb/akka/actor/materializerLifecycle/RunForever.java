package giangbb.akka.actor.materializerLifecycle;

import akka.actor.AbstractActor;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunForever extends AbstractActor {
    private static final Logger logger = LogManager.getLogger(RunForever.class);

    final Materializer materializer;

    public RunForever(Materializer materializer) {
        //Stream run by this materializer will not be terminated when this actor is terminated
        //it will be terminated when the actor system is terminated
        this.materializer = materializer;
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        logger.debug("RunForever - preStart");
        //start a stream forever
        Source.repeat("RunForever - Hello")
                .runWith(
                        Sink.onComplete(
                                tryDone -> {
                                    logger.debug("RunForever - Terminated stream: {}", tryDone);
                                }
                        ),materializer
                );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(String.class, str -> {
            logger.debug("RunForever recieved: {}", str);
            if (str.equals("stop")){
                // this will NOT terminate the stream (it's bound to the system!)
                context().stop(self());
            }
        }).build();
    }
}
