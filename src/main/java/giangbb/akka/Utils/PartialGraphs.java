package giangbb.akka.Utils;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Pair;
import akka.stream.*;
import akka.stream.javadsl.*;
import giangbb.akka.actor.TestActor;
import giangbb.akka.model.Ints;

import java.util.ArrayList;
import java.util.concurrent.CompletionStage;

public class PartialGraphs {
    //Sometimes it is not possible (or needed) to construct the entire computation graph in one place,
    // but instead construct all of its different phases in different places and in the end connect them all into a complete graph and run it.


    /*
    ---------(source1)------------>|
                                   |
                                   v
                                (maxOf2)--------------------->|
                                  ^                           |
                                 |                            v
    ---------(source2)--------->|                         (maxOf2)------------>(out)
                                                              ^
                                                             |
    ---------(source1)--------------------------------------|
     */
    public static void graphGetMaxFrom3Source(){
        ActorSystem actorSystem = ActorSystem.create("GianbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        Graph<FanInShape2<Integer,Integer,Integer>, NotUsed> maxOf2 = ZipWith.create((Integer in1,Integer in2) -> Math.max(in1,in2));

        Graph<UniformFanInShape<Integer,Integer>,NotUsed> maxOf3 = GraphDSL.create(
                buider -> {
                    FanInShape2<Integer,Integer,Integer> maxOf2_1 = buider.add(maxOf2);
                    FanInShape2<Integer,Integer,Integer> maxOf2_2 = buider.add(maxOf2);

                    buider.from(maxOf2_1.out()).toInlet(maxOf2_2.in0());
                    // return the shape, which has three inputs and one output
                    return new UniformFanInShape<Integer, Integer>(maxOf2_2.out(),new Inlet[]{maxOf2_1.in0(),maxOf2_1.in1(),maxOf2_2.in1()});
                }
        );

        Sink<Integer, CompletionStage<Integer>> sink = Sink.head();

        RunnableGraph<CompletionStage<Integer>> graph = RunnableGraph.<CompletionStage<Integer>>fromGraph(
                GraphDSL.create(
                        sink,
                        (builder,out) -> {
                            UniformFanInShape<Integer,Integer> maxOf3Shape = builder.add(maxOf3);

                            final Outlet<Integer> in1 = builder.add(Source.single(1)).out();
                            final Outlet<Integer> in2 = builder.add(Source.single(3)).out();
                            final Outlet<Integer> in3 = builder.add(Source.single(2)).out();
                            builder.from(in1).viaFanIn(maxOf3Shape).to(out);
                            builder.from(in2).viaFanIn(maxOf3Shape);
                            builder.from(in3).viaFanIn(maxOf3Shape);

//                            builder.from(builder.add(Source.single(1))).toInlet(maxOf3Shape.in(0));
//                            builder.from(builder.add(Source.single(3))).toInlet(maxOf3Shape.in(1));
//                            builder.from(builder.add(Source.single(2))).toInlet(maxOf3Shape.in(2));
//                            builder.from(maxOf3Shape.out()).to(out);

                            return ClosedShape.getInstance();
                        }
                )
        );

        CompletionStage<Integer> completionStage = graph.run(materializer);
        try{
            System.out.println("max value = "+completionStage.toCompletableFuture().get());
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    //region Constructing Source Sink Flow
    //Instead of treating a Graph as a collection of flows and junctions which may not yet all be connected
    // it is sometimes useful to expose such a complex graph as a simpler structure, such as a Source, Sink or Flow
    //In fact, these concepts can be expressed as special cases of a partially connected graph:
    //Source is a partial graph with exactly one output, that is it returns a SourceShape
    //Sink is a partial graph with exactly one input, that is it returns a SinkShape
    //Flow is a partial graph with exactly one input and exactly one output, that is it returns a FlowShape

    public static void constructingSourceShape(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        Source<Integer,NotUsed> ints = Source.fromIterator(() -> new Ints());

        Source<Pair<Integer,Integer>,NotUsed> pairSource = Source.fromGraph(
                GraphDSL.create(
                        (builder) -> {
                            FanInShape2<Integer,Integer,Pair<Integer,Integer>> myZip = builder.add(Zip.create());

                            Outlet<Integer> s1 = builder.add(ints.filter(a -> a%2 == 0)).out();
                            Outlet<Integer> s2 = builder.add(ints.filter(a -> a%2 != 0)).out();
                            builder.from(s1).toInlet(myZip.in0());
                            builder.from(s2).toInlet(myZip.in1());
                            return SourceShape.of(myZip.out());
                        }
                )
        );

        pairSource.runWith(Sink.foreach(pair -> System.out.println("num1:"+ pair.first()+" num2:"+pair.second())),materializer);

    }

    public static void constructingFlowShape(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);
        Flow<Integer,Pair<Integer,String>,NotUsed> myflow = Flow.fromGraph(
                GraphDSL.create(
                        builder -> {
                            UniformFanOutShape<Integer,Integer> broadcast = builder.add(Broadcast.create(2));
                            FanInShape2<Integer,String,Pair<Integer,String>> zip = builder.add(Zip.create());

                            builder.from(broadcast).toInlet(zip.in0());
                            builder.from(broadcast).via(builder.add(Flow.of(Integer.class).map(i -> String.format("'%d'",i)))).toInlet(zip.in1());
                            return FlowShape.of(broadcast.in(),zip.out());
                        }
                )
        );

        Source.fromIterator( ()->new Ints())
                .via(myflow)
                .runWith(
                        Sink.foreach(a->System.out.println("number:"+a.first()+" - string:"+a.second()))
                ,materializer);


    }
    //endregion

    //region Combine Souce/Sink

    //There is a simplified API you can use to combine sources and sinks with junctions like: Broadcast<T>, Balance<T>, Merge<In> and Concat<A> without the need for using the Graph DSL.
    // The combine method takes care of constructing the necessary graph underneath
    public static void combineSource(){
        //In following example we combine two sources into one (fan-in)
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);


        Source<Integer,NotUsed> source1 = Source.fromIterator(()->new Ints()).filter(a -> a%2 == 0);
        Source<Integer,NotUsed> source2 = Source.fromIterator(()->new Ints()).filter(a -> a%2 != 0);

        Source<Integer,NotUsed> source = Source.combine(source1,source2,new ArrayList<>(),i -> Merge.<Integer>create(i));

        source.runWith(Sink.foreach(a -> System.out.println("num: "+a)),materializer);
    }

    public static void combineSink(){
        //In following example we combine two sinks into one (fan-out)
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        ActorRef testActor = actorSystem.actorOf(Props.create(TestActor.class, () -> new TestActor(materializer)));
        Sink<Integer, NotUsed> sendRemotely = Sink.actorRef(testActor,"Finished");

        Sink<Integer,CompletionStage<Done>> showConsole = Sink.foreach(a -> System.out.println("number:"+a));

        Sink<Integer,NotUsed> sinks = Sink.combine(sendRemotely,showConsole, new ArrayList<>(), i -> Broadcast.create(i));

        Source.fromIterator(() -> new Ints()).runWith(sinks,materializer);
    }


    //endregion


}
