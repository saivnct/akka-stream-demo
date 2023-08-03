package giangbb.akka.Utils;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.*;
import akka.stream.javadsl.*;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

public class Modularity {


    public static void createNesting(){
        ActorSystem actorSystem = ActorSystem.create("giangbbSystem");

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

        CompletionStage<Integer> completionStage = runnableGraph.run(actorSystem);

        completionStage.thenRun(() -> actorSystem.terminate());

        try {
            System.out.println("result:"+completionStage.toCompletableFuture().get());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void composingComplexSystem(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");

        //C1 - Using the implicit port numbering feature (to make the graph more readable and similar to the diagram)
        // and we imported Source s, Sink s and Flow s explicitly
        RunnableGraph<NotUsed> graph1 = RunnableGraph.fromGraph(
                GraphDSL.create( builder -> {
                    final Outlet<Integer> A = builder.add(Source.single(0)).out();
                    final UniformFanOutShape<Integer,Integer> B = builder.add(Broadcast.create(2));
                    final UniformFanInShape<Integer, Integer> C = builder.add(Merge.create(2));
                    final FlowShape<Integer,Integer> D = builder.add(Flow.of(Integer.class).map(i -> i+1));
                    final UniformFanOutShape<Integer,Integer> E = builder.add(Balance.create(2));
                    final UniformFanInShape<Integer,Integer> F = builder.add(Merge.create(2));
                    final FlowShape<Integer,Integer> H = builder.add(Flow.of(Integer.class).map( i -> {
                        System.out.println("H1: "+i);
                        return i;
                    }));

                    final Inlet<Integer> G = builder.add(Sink.<Integer>foreach(i -> System.out.println("G1: "+i))).in();

                    builder.from(F).via(H).viaFanIn(C);
                    builder.from(A).viaFanOut(B).viaFanIn(C).viaFanIn(F);
                    builder.from(B).via(D).viaFanOut(E).viaFanIn(F);
                    builder.from(E).toInlet(G);
                    return ClosedShape.getInstance();
                })
        );
        graph1.run(actorSystem);

        //C2 - It is possible to refer to the ports explicitly, and it is not necessary to import our linear operators via add()
        RunnableGraph<NotUsed> graph2 = RunnableGraph.fromGraph(
                GraphDSL.create( builder -> {
                    final Outlet<Integer> A = builder.add(Source.single(0)).out();
                    final UniformFanOutShape<Integer,Integer> B = builder.add(Broadcast.create(2));
                    final UniformFanInShape<Integer, Integer> C = builder.add(Merge.create(2));
                    final FlowShape<Integer,Integer> D = builder.add(Flow.of(Integer.class).map(i -> i+1));
                    final UniformFanOutShape<Integer,Integer> E = builder.add(Balance.create(2));
                    final UniformFanInShape<Integer,Integer> F = builder.add(Merge.create(2));
                    final FlowShape<Integer,Integer> H = builder.add(Flow.of(Integer.class).map( i -> {
                        System.out.println("H2: "+i);
                        return i;
                    }));
                    final Inlet<Integer> G = builder.add(Sink.<Integer>foreach(i -> System.out.println("G2: "+i))).in();

                   builder.from(F.out()).toInlet(H.in());
                   builder.from(H.out()).toInlet(C.in(0));
                   builder.from(C.out()).toInlet(F.in(0));
                   builder.from(A).toInlet(B.in());
                   builder.from(B.out(0)).toInlet(C.in(1));
                   builder.from(B.out(1)).toInlet(D.in());
                   builder.from(D.out()).toInlet(E.in());
                   builder.from(E.out(0)).toInlet(F.in(1));
                   builder.from(E.out(1)).toInlet(G);
                   return ClosedShape.getInstance();
                })
        );
        graph2.run(actorSystem);

    }


    public static void partialGrapth(){
        //We will  create a reusable component with the graph DSL.
        // The way to do it is to use the create() factory method on GraphDSL.
        // If we remove the sources and sinks from the previous example, what remains is a partial graph

        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");

        Graph<FlowShape<Integer,Integer>,NotUsed> partial = GraphDSL.create( builder -> {
            final UniformFanOutShape<Integer,Integer> B = builder.add(Broadcast.create(2));
            final UniformFanInShape<Integer, Integer> C = builder.add(Merge.create(2));
            final FlowShape<Integer,Integer> D = builder.add(Flow.of(Integer.class).map(i -> i+1));
            final UniformFanOutShape<Integer,Integer> E = builder.add(Balance.create(2));
            final UniformFanInShape<Integer,Integer> F = builder.add(Merge.create(2));
            final FlowShape<Integer,Integer> H = builder.add(Flow.of(Integer.class).map( i -> {
                System.out.println("H: "+i);
                return i;
            }));


            builder.from(F.out()).toInlet(H.in());
            builder.from(H.out()).toInlet(C.in(0));
            builder.from(C.out()).toInlet(F.in(0));
            builder.from(B.out(0)).toInlet(C.in(1));
            builder.from(B.out(1)).toInlet(D.in());
            builder.from(D.out()).toInlet(E.in());
            builder.from(E.out(0)).toInlet(F.in(1));



//            builder.from(H).viaFanIn(C);
//            builder.from(B).viaFanIn(C).viaFanIn(F).via(H);
//            builder.from(B).via(D).viaFanOut(E).viaFanIn(F);

            return new FlowShape<Integer, Integer>(B.in(),E.out(1));
        });

        //FlowShape partial - It is not possible to use it as a Flow yet, though (i.e. we cannot call .filter() on it), but Flow has a fromGraph() method that adds the DSL to a FlowShape

        //The resulting graph is already a properly wrapped module, so there is no need to call named() to encapsulate the graph,
        // but it is a good practice to give names to modules to help debugging

        //Since our partial graph has the right shape, it can be already used in the simpler, linear DSL
        Source.single(0).via(partial).to(Sink.foreach( i -> System.out.println("i: "+i))).run(actorSystem);
    }

    public static void combinePartialGrapth(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");

        Graph<FlowShape<Integer,Integer>,NotUsed> partial = GraphDSL.create( builder -> {
            final UniformFanOutShape<Integer,Integer> B = builder.add(Broadcast.create(2));
            final UniformFanInShape<Integer, Integer> C = builder.add(Merge.create(2));
            final FlowShape<Integer,Integer> D = builder.add(Flow.of(Integer.class).map(i -> i+1));
            final UniformFanOutShape<Integer,Integer> E = builder.add(Balance.create(2));
            final UniformFanInShape<Integer,Integer> F = builder.add(Merge.create(2));
            final FlowShape<Integer,Integer> H = builder.add(Flow.of(Integer.class).map( i -> {
//                System.out.println("H: "+i);
                return i;
            }));

            builder.from(H).viaFanIn(C);
            builder.from(B).viaFanIn(C).viaFanIn(F).via(H);
            builder.from(B).via(D).viaFanOut(E).viaFanIn(F);

            return new FlowShape<Integer, Integer>(B.in(),E.out(1));
        });


        Flow<Integer,Integer,NotUsed> flow = Flow.fromGraph(partial);

        Source<Integer, NotUsed> source = Source.fromGraph(
                GraphDSL.create(
                    builder -> {
                        final UniformFanInShape<Integer,Integer> merger = builder.add(Merge.create(2));
                        final Outlet<Integer> source1 = builder.add(Source.single(0)).out();
                        final Outlet<Integer> source2 = builder.add(Source.range(0,100)).out();
                        final FlowShape<Integer,Integer> f = builder.add(Flow.of(Integer.class).map(
                                i -> {
                                    System.out.println("Source-flow-i:"+i);
                                    return i;
                                }
                        ));

                        builder.from(source2).toFanIn(merger);
                        builder.from(source1).toFanIn(merger);
                        builder.from(merger).toInlet(f.in());

                        return new SourceShape<Integer>(f.out());
                    }
                )
        );


        Sink<Integer,NotUsed> sink = Sink.fromGraph(
                GraphDSL.create(
                        builder -> {
                            FlowShape<Integer,Integer> f1 = builder.add(Flow.of(Integer.class).map(i -> i*2));

                            Inlet<Integer> sink1 = builder.add(Sink.<Integer>foreach( i -> System.out.println("sink i:"+i))).in();

                            builder.from(f1).toInlet(sink1);
                            return new SinkShape<Integer>(f1.in());
                        }
                )
        );


        RunnableGraph<NotUsed> graph = source.via(flow).to(sink);
        graph.run(actorSystem);
    }




}
