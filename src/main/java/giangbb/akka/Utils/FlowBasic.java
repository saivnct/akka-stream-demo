package giangbb.akka.Utils;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.Pair;
import akka.stream.*;
import akka.stream.javadsl.*;
import giangbb.akka.actor.materializerLifecycle.RunForever;
import giangbb.akka.actor.materializerLifecycle.RunWithMyself;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

//https://github.com/akka/akka/blob/master/akka-docs/src/test/java/jdocs/stream/FlowDocTest.java
public class FlowBasic {
    public static void demoSimpleStream(){
        // Create an Akka system
        final ActorSystem actorSystem = ActorSystem.create("GianbbSystem");

        final Materializer materializer = ActorMaterializer.create(actorSystem);


        Source<Integer, NotUsed> source = Source.range(1,100);
//        source.runWith(Sink.foreach(
//                i -> System.out.println("i: "+i)
//        ),materializer);
//        source.runForeach(i -> System.out.println("i: "+i),materializer);


        Source<BigInteger, NotUsed> factorials = source.scan(BigInteger.ONE, (accumulate, next) -> accumulate.multiply(BigInteger.valueOf(next)));
//        factorials.map( f -> "f: "+f+"\n").runForeach(str -> System.out.println(str),materializer);

        final CompletionStage<Done> done = factorials.zipWith(Source.range(0,99),(f, i) ->  String.format("%d! = %s", i, f) )
                .throttle(1, Duration.ofSeconds(1)) //force produce 1 element every 1 second
                .take(5)   //only take 10 element overall
                .runForeach(str -> System.out.println(str),materializer);

        done.thenRun(() -> actorSystem.terminate());
        try{
            done.toCompletableFuture().get();
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("DONE!");
    }

    public static void demoCancellable(){
        // Create an Akka system
        final ActorSystem actorSystem = ActorSystem.create("GianbbSystem");

        final Materializer materializer = ActorMaterializer.create(actorSystem);

        final Object tick = new Object();
        final Duration oneSecond = Duration.ofSeconds(1);

        Source<Object, Cancellable> timer = Source.tick(oneSecond,oneSecond,tick);
        Source<Object, Cancellable> timerMap = timer.map( t -> "tick");

        Cancellable timerCancellable = timer.to(Sink.foreach(
                t -> System.out.println(t)
        )).run(materializer);

        Cancellable timerMapCancellable = timerMap.to(Sink.foreach(
                t -> System.out.println(t)
        )).run(materializer);

        int i = 0;
        while (i<5){
            try{
                TimeUnit.SECONDS.sleep(1);
            }catch (Exception e){
                e.printStackTrace();
            }
            i++;
        }

        System.out.println("Stop timers!!!");
        timerCancellable.cancel();
        timerMapCancellable.cancel();


    }

    public static void demoRunableGraph(){
        // Create an Akka system
        final ActorSystem actorSystem = ActorSystem.create("GianbbSystem");

        final Materializer materializer = ActorMaterializer.create(actorSystem);

        // Explicitly creating and wiring up a Source, Sink and Flow
        RunnableGraph runnableGraph = Source.range(0,10)
                .via(Flow.of(Integer.class).map(elem -> elem*2))
                .to(Sink.foreach(
                        x -> System.out.println("x: "+x)
                ));
//        runnableGraph.run(materializer);

        // Starting from a Source
        final Source<Integer, NotUsed> source = Source.range(0,10).map(e -> e*3);
        RunnableGraph runnableGraph2 = source.to(Sink.foreach(
                x -> System.out.println("x: "+x)
        ));
//        runnableGraph2.run(materializer);

        // Starting from a Sink
        final  Sink<Integer, NotUsed> sink = Flow.of(Integer.class).map(e -> e*4).to(Sink.foreach(
                x -> System.out.println("x: "+x)
        ));
        RunnableGraph runnableGraph3 = Source.range(0,10).to(sink);
        runnableGraph3.run(materializer);

    }

    public static void demoCombiningMaterializedValues(){
        // Create an Akka system
        final ActorSystem actorSystem = ActorSystem.create("GianbbSystem");

        final Materializer materializer = ActorMaterializer.create(actorSystem);


        // An empty source that can be shut down explicitly from the outside
        Source<Integer, CompletableFuture<Optional<Integer>>> source = Source.<Integer>maybe();

        // A flow that internally throttles elements to 1/second, and returns a Cancellable
        // which can be used to shut down the stream
        final Duration oneSecond = Duration.ofSeconds(1);
        Flow<Integer,Integer, Cancellable> flow = Flow.fromGraph(
                GraphDSL.create(
                        Source.tick(oneSecond,oneSecond,""),
                        (b, ticksource) -> {
                            FanInShape2<String,Integer,Integer> zip = b.add(ZipWith.create(Keep.right()));
                            b.from(ticksource).toInlet(zip.in0());
                            return  FlowShape.of(zip.in1(),zip.out());
                        }
                )
        );

        // A sink that returns the first element of a stream in the returned Future
        Sink<Integer, CompletionStage<Integer>> sink = Sink.head();

        // By default, the materialized value of the leftmost stage is preserved
        RunnableGraph<CompletableFuture<Optional<Integer>>> r1 = source.via(flow).to(sink);

        // Simple selection of materialized values by using Keep.right
        RunnableGraph<Cancellable> r2 = source.viaMat(flow, Keep.right()).to(sink);
        RunnableGraph<CompletionStage<Integer>> r3 = source.via(flow).toMat(sink,Keep.right());

        // Using runWith will always give the materialized values of the stages added
        // by runWith() itself
        CompletionStage<Integer> r4 = source.via(flow).runWith(sink,materializer);
        CompletableFuture<Optional<Integer>> r5 = flow.to(sink).runWith(source,materializer);
        Pair<CompletableFuture<Optional<Integer>>,CompletionStage<Integer>> r6 =  flow.runWith(source,sink,materializer);

        // Using more complex combinations
        RunnableGraph<Pair<CompletableFuture<Optional<Integer>>,Cancellable>> r7 = source.viaMat(flow,Keep.both()).to(sink);

        RunnableGraph<Pair<CompletableFuture<Optional<Integer>>, CompletionStage<Integer>>> r8 = source.via(flow).toMat(sink,Keep.both());

        RunnableGraph< Pair<Pair<CompletableFuture<Optional<Integer>>,Cancellable>,CompletionStage<Integer>> > r9 = source.viaMat(flow,Keep.both()).toMat(sink,Keep.both());

        RunnableGraph< Pair<Cancellable,CompletionStage<Integer>> > r10 = source.viaMat(flow,Keep.right()).toMat(sink, Keep.both());

        // It is also possible to map over the materialized values. In r9 we had a
        // doubly nested pair, but we want to flatten it out
        RunnableGraph<Cancellable> r11 = r9.mapMaterializedValue(
                (nestedTuple) -> {
                    CompletableFuture<Optional<Integer>> ra = nestedTuple.first().first();
                    Cancellable rb = nestedTuple.first().second();
                    CompletionStage<Integer> rc = nestedTuple.second();
                    return  rb;
                }
        );
    }

    public static void demoSourcePreMaterialization(){
        //There are situations in which you require a Source materialized value before the Source gets hooked up to the rest of the graph.
        // This is particularly useful in the case of “materialized value powered” Sources, like: Source.queue, Source.actorRef or Source.maybe

        //By using the preMaterialize operator on a Source, you can obtain its materialized value and another Source.
        // The latter Source can be used to consume messages from the original Source. Note that this can be materialized multiple times

        final ActorSystem actorSystem = ActorSystem.create("GianbbSystem");
        final Materializer materializer = ActorMaterializer.create(actorSystem);

        Source<String, ActorRef> matValuePoweredSource = Source.actorRef(100, OverflowStrategy.dropHead());

        Pair<ActorRef,Source<String, NotUsed>> actorRefSourcePair = matValuePoweredSource.preMaterialize(materializer);

        actorRefSourcePair.first().tell("Hello!",ActorRef.noSender());

        for (int i = 0 ; i<100; i++){
            actorRefSourcePair.first().tell(""+i,ActorRef.noSender());
        }

        actorRefSourcePair.second().runWith(Sink.foreach(
                str -> System.out.println("Received: "+str)
        ), materializer);

        System.out.println("DONE!!!");

        boolean loop = true;
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                actorRefSourcePair.first().tell(cmd,ActorRef.noSender());

                if (cmd.equals("stop")){
                    loop = false;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void demoMaterializerLifeCycle() {
        //Warning
        //Do not create new actor materializers inside actors by passing the context.system to it.
        // This will cause a new ActorMaterializer to be created and potentially leaked (unless you shut it down explicitly) for each such actor.
        // It is instead recommended to either pass-in the Materializer or create one using the actor’s context.

        final ActorSystem actorSystem = ActorSystem.create("GianbbSystem");
        final Materializer materializer = ActorMaterializer.create(actorSystem);

        ActorRef actorRunWithMyself = actorSystem.actorOf(Props.create(RunWithMyself.class));
        ActorRef actorRunForever = actorSystem.actorOf(Props.create(RunForever.class, () -> new RunForever(materializer)  ));

        boolean loop = true;
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                actorRunWithMyself.tell(cmd,ActorRef.noSender());
                actorRunForever.tell(cmd,ActorRef.noSender());

                if (cmd.equals("stop")){
                    loop = false;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }


    }
}
