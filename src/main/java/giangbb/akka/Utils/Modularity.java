package giangbb.akka.Utils;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;

import java.util.concurrent.CompletionStage;

public class Modularity {


    public static void createNesting(){
        ActorSystem actorSystem = ActorSystem.create("giangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        Source<Integer, NotUsed> nestedSource = Source.single(0) // An atomic source
                .map(i -> i+1) // an atomic processing stage
                .named("nestedSource"); // wraps up the current Source and gives it a name

        Flow<Integer,Integer,NotUsed> nestedFlow = Flow.of(Integer.class)
                                                        .filter(i -> i != 0) // an atomic processing stage
                                                        .map(i -> i+2) // another atomic processing stage
                                                        .named("nestedFlow"); // wraps up the Flow, and gives it a name

        Sink<Integer, CompletionStage<Integer>> nestedSink = nestedFlow.toMat(Sink.fold(0, (acc, i) -> acc+i), Keep.right()) // wire an atomic sink to the nestedFlow
                                                        .named("nestedSink"); // wrap it up

        // Create a RunnableGraph
        final RunnableGraph<CompletionStage<Integer>> runnableGraph = nestedSource.toMat(nestedSink,Keep.right());

        CompletionStage<Integer> completionStage = runnableGraph.run(materializer);

        try {
            System.out.println("result:"+completionStage.toCompletableFuture().get());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
