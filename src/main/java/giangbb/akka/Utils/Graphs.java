package giangbb.akka.Utils;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.*;
import akka.stream.javadsl.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

//https://github.com/akka/akka/blob/v2.5.23/akka-stream-tests/src/test/java/akka/stream/javadsl/GraphDslTest.java
public class Graphs {

    /*
                                     ------------f2---->
                                    |                   |
                                    |                   |
    (in) ---f1---->(broadcast)----->                     -->(merge)--------f4---->(out)
                                    |                   |
                                    |                   |
                                    -------------f3---->
    */
    public static void simpleGraph1Sink(){
        ActorSystem actorSystem = ActorSystem.create("GianbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        Source<Integer,NotUsed> source = Source.range(0,10);
        Sink<List<String>, CompletionStage<List<String>>> sink = Sink.head();

//        ActorRef actorTest = actorSystem.actorOf(Props.create(TestActor.class, () -> new TestActor(materializer)  ));
//        Sink<List<String>, NotUsed> sink = Sink.actorRef(actorTest,"Finished!");


        Flow<Integer,Integer,NotUsed> f1 = Flow.of(Integer.class).map(e -> e+10);
        Flow<Integer,String,NotUsed> f2 = Flow.of(Integer.class).map(e -> e+20).map(e -> "f2-"+e);
        Flow<Integer,String,NotUsed> f3 = Flow.of(Integer.class).map(e -> e+30).map(e -> "f3-"+e);
        Flow<String,String,NotUsed> f4 = Flow.of(String.class).map(e -> "f4-"+e);

        RunnableGraph<CompletionStage<List<String>>> graph = RunnableGraph.fromGraph(
                GraphDSL        //create() function binds sink, out which is sink's out port and builder DSL
                        .create( // we need to reference out's shape in the builder DSL below (in to() function)
                            sink,
                            (builder,out) -> { // variables: builder (GraphDSL.Builder) and out (SinkShape)
                                final UniformFanOutShape<Integer,Integer> broadcast = builder.add(Broadcast.create(2));
                                final UniformFanInShape<String,String> merge = builder.add(Merge.create(2));
                                final Outlet<Integer> in = builder.add(source).out();

                                builder.from(in)
                                        .via(builder.add(f1))
                                        .viaFanOut(broadcast)
                                        .via(builder.add(f2))
                                        .viaFanIn(merge)
                                        .via(builder.add(f4.grouped(1000)))
                                        .to(out);   // to() expects a SinkShape
                                builder.from(broadcast)
                                        .via(builder.add(f3))
                                        .toFanIn(merge);
                                return ClosedShape.getInstance();
                            }
                        )
        );

        try{
            List<String> list = graph.run(materializer).toCompletableFuture().get();
            System.out.println("list: "+list);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void simpleGraph2Sink(){
        final ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        final Materializer materializer = ActorMaterializer.create(actorSystem);

        Sink<Integer, CompletionStage<Integer>> topHeadSink = Sink.head();
        Sink<Integer, CompletionStage<Integer>> bottomHeadSink = Sink.head();

        Flow<Integer, Integer, NotUsed> sharedDoubler =
                Flow.of(Integer.class).map(elem -> elem * 2);

        RunnableGraph<Pair<CompletionStage<Integer>, CompletionStage<Integer>>> graph =
                RunnableGraph.<Pair<CompletionStage<Integer>, CompletionStage<Integer>>>fromGraph(
                        GraphDSL.create(
                                topHeadSink, // import this sink into the graph
                                bottomHeadSink, // and this as well
                                Keep.both(),
                                (b, top, bottom) -> {
                                    final UniformFanOutShape<Integer, Integer> bcast = b.add(Broadcast.create(2));

                                    b.from(b.add(Source.single(1)))
                                            .viaFanOut(bcast)
                                            .via(b.add(sharedDoubler))
                                            .to(top);
                                    b.from(bcast).via(b.add(sharedDoubler)).to(bottom);
                                    return ClosedShape.getInstance();
                                }));


        Pair<CompletionStage<Integer>, CompletionStage<Integer>> pair = graph.run(materializer);

        try{
            int first = pair.first().toCompletableFuture().get(3, TimeUnit.SECONDS);
            int second = pair.second().toCompletableFuture().get(3, TimeUnit.SECONDS);

            System.out.println("first: "+first);
            System.out.println("second: "+second);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void graphReadMaterializeValueOfManySinks(){
        //In some cases we may have a list of graph elements, for example if they are dynamically created.
        // If these graphs have similar signatures, we can construct a graph collecting all their materialized values as a collection

        ActorSystem actorSystem = ActorSystem.create("GianbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        // create the source
        Source<String,NotUsed> in = Source.from(Arrays.asList("ax", "bx", "cv", "cx"));

        // generate the sinks from code
        List<String> prefixes = Arrays.asList("a", "b", "c");
        List<Sink<String,CompletionStage<String>>> listSink = new ArrayList<>();
        for (String prefix: prefixes){
            Sink<String, CompletionStage<String>> sink = Flow.of(String.class).filter(strt -> strt.startsWith(prefix)).toMat(Sink.head(),Keep.right());
            listSink.add(sink);
        }

        RunnableGraph<List<CompletionStage<String>>> graph = RunnableGraph.<List<CompletionStage<String>>>fromGraph(
                GraphDSL.create(
                        listSink,
                        (GraphDSL.Builder<List<CompletionStage<String>>> builder,  List<SinkShape<String>> outs) -> {
                            final UniformFanOutShape<String,String> bcast = builder.add(Broadcast.create(outs.size()));

                            final Outlet<String> source = builder.add(in).out();

                            builder.from(source).viaFanOut(bcast);
                            for (SinkShape<String> sink:outs){
                                builder.from(bcast).to(sink);
                            }
                            return ClosedShape.getInstance();
                        }
                )
        );

        List<CompletionStage<String>> completionStageList = graph.run(materializer);
        for (CompletionStage<String> completionStage:completionStageList){
            try{
                String materialRes = completionStage.toCompletableFuture().get(3, TimeUnit.SECONDS);
                System.out.println("materialRes: "+materialRes);
            }catch (Exception e){
                e.printStackTrace();
            }
        }


    }



}
