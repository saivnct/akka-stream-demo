package giangbb.akka.Utils;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.japi.Pair;
import akka.stream.*;
import akka.stream.javadsl.*;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class Hub {
    public static void mergeHubTest(){
        //A MergeHub allows to implement a dynamic FAN-IN junction point in a graph
        // where elements coming from different producers are emitted in a First-Comes-First-Served fashion.
        // If the consumer cannot keep up then all of the producers are backpressured.
        // The hub itself comes as a Source to which the single consumer can be attached.
        // It is not possible to attach any producers until this Source has been materialized (started).
        // This is ensured by the fact that we only get the corresponding Sink as a materialized value

        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        Sink<String, CompletionStage<Done>> consumer = Sink.foreach(s -> System.out.println("consumer receive:"+s));
        // Attach a MergeHub Source to the consumer.
        // This will materialize to a corresponding Sink.
        RunnableGraph<Sink<String, NotUsed>> runnableGraph = MergeHub.of(String.class,16).to(consumer);

        // By running/materializing the consumer we get back a Sink, and hence
        // now have access to feed elements into it. This Sink can be materialized
        // any number of times, and every element that enters the Sink will
        // be consumed by our consumer.
        Sink<String, NotUsed> toConsumer = runnableGraph.run(materializer);

        boolean loop = true;
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                if (cmd.equals("stop")){
                    loop = false;
                    continue;
                }

                Source.single(cmd).runWith(toConsumer,materializer);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void broadCastHubTest(){
        //A BroadcastHub can be used to consume elements from a common producer by a dynamic set of consumers.
        // The rate of the producer will be automatically adapted to the slowest consumer.
        // In this case, the hub is a Sink to which the single producer must be attached first.
        // Consumers can only be attached once the Sink has been materialized (i.e. the producer has been started)

        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        Source<String, Cancellable> producer = Source.tick(Duration.ofSeconds(1),Duration.ofSeconds(1),"New Message" );

        // Attach a BroadcastHub Sink to the producer. This will materialize to a
        // corresponding Source.
        // (We need to use toMat and Keep.right since by default the materialized
        // value to the left is used)
        RunnableGraph<Source<String,NotUsed>> runnableGraph = producer.toMat( BroadcastHub.of(String.class,256), Keep.right());

        // By running/materializing the producer, we get back a Source, which
        // gives us access to the elements published by the producer.
        Source<String, NotUsed> fromProducer = runnableGraph.run(materializer);

        fromProducer.runForeach(msg -> System.out.println("consumer1: " + msg), materializer);
        fromProducer.runForeach(msg -> System.out.println("consumer2: " + msg), materializer);
        //The resulting Source can be materialized any number of times, each materialization effectively attaching a new subscriber.
        // If there are no subscribers attached to this hub then it will not drop any elements but instead backpressure the upstream producer until subscribers arrive.
        // This behavior can be tweaked by using the operators .buffer for example with a drop strategy, or attaching a subscriber that drops all messages.
        // If there are no other subscribers, this will ensure that the producer is kept drained (dropping all elements)
        // and once a new subscriber arrives it will adaptively slow down, ensuring no more messages are dropped.
    }

    public static void simplePublishSubscribeService(){
        //an example that builds a Flow representing a publish-subscribe channel.
        // The input of the Flow is published to all subscribers while the output streams all the elements published
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);


        //First, we connect a MergeHub and a BroadcastHub together to form a publish-subscribe channel.
        // Once we materialize this small stream,
        // we get back a pair of Source and Sink that together define the publish and subscribe sides of our channel
        Pair<Sink<String,NotUsed>,Source<String,NotUsed>> sinkAndSource =
                MergeHub.of(String.class,16)
                        .toMat(BroadcastHub.of(String.class,256),Keep.both()).run(materializer);

        Sink<String,NotUsed> sink = sinkAndSource.first();
        Source<String,NotUsed> source = sinkAndSource.second();

        // Ensure that the Broadcast output is dropped if there are no listening parties.
        // If this dropping Sink is not attached, then the broadcast hub will not drop any
        // elements itself when there are no subscribers, backpressuring the producer instead.
        source.runWith(Sink.ignore(),materializer);
        source.runWith(Sink.foreach(s -> System.out.println("origin sink:"+s)),materializer);

        //We now wrap the Sink and Source in a Flow using Flow.fromSinkAndSource.
        // This bundles up the two sides of the channel into one and forces users of it to always define a publisher and subscriber side
        // (even if the subscriber side is dropping).
        // It also allows us to attach a KillSwitch as a BidiStage which in turn makes it possible to close both the original Sink and Source at the same time.
        // Finally, we add backpressureTimeout on the consumer side to ensure that subscribers that block the channel for more than 3 seconds are forcefully removed (and their stream failed)
        Flow<String,String, UniqueKillSwitch> busFlow =
                Flow.fromSinkAndSource(sink,source)
                    .joinMat(KillSwitches.singleBidi(),Keep.right())
                    .backpressureTimeout(Duration.ofSeconds(1));


        //The resulting Flow now has a type of Flow[String, String, UniqueKillSwitch]
        // representing a publish-subscribe channel which can be used any number of times to attach new producers or consumers
        //In addition, it materializes to a UniqueKillSwitch (see UniqueKillSwitch) that can be used to deregister a single user externally
        UniqueKillSwitch killSwitch = Source.repeat("Hello World 0!").delay(Duration.ofSeconds(5), DelayOverflowStrategy.backpressure())
                .throttle(1,Duration.ofSeconds(1))
                .viaMat(busFlow,Keep.right())
                .to(Sink.foreach(s -> System.out.println("Sink0:"+s))).run(materializer);


        //region Example 1: add new sources sinks
//        List<UniqueKillSwitch> uniqueKillSwitchList = new ArrayList<UniqueKillSwitch>();
//        boolean loop = true;
//        int i=0;
//        while (loop){
//            try{
//                System.out.println("Input a command:");
//                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//                String cmd = in.readLine();
//
//                if (cmd.equals("stop")){
//                    loop = false;
//                    continue;
//                }
//
//                if (cmd.equals("add")){
//                    final int a = i+1;
//                    i++;
//                    UniqueKillSwitch killSwitch2 = Source.repeat("Hello World "+a+"!").delay(Duration.ofSeconds(5), DelayOverflowStrategy.backpressure())
//                            .viaMat(busFlow,Keep.right())
//                            .to(Sink.foreach(s -> System.out.println("Sink"+a+":"+s))).run(materializer);
//                    uniqueKillSwitchList.add(killSwitch2);
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//
//        System.out.println("SHUTDOWN THE KILLSWITCH!!!!!!!!");
//        for (UniqueKillSwitch uniqueKillSwitch:uniqueKillSwitchList){
//            uniqueKillSwitch.shutdown();
//        }
        //endregion

        //region Example 2: add new sources
        int i=0;
        boolean loop = true;
        List<UniqueKillSwitch> uniqueKillSwitchList = new ArrayList<UniqueKillSwitch>();
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                if (cmd.equals("stop")){
                    loop = false;
                    continue;
                }

                if (cmd.equals("add")){
                    i++;
                    UniqueKillSwitch killSwitch2 = Source.repeat("Hello World "+i+"!")
                            .delay(Duration.ofSeconds(5), DelayOverflowStrategy.backpressure())
                            .throttle(1,Duration.ofSeconds(1))
                            .viaMat(KillSwitches.single(),Keep.right())
                            .to(sink).run(materializer);
                    uniqueKillSwitchList.add(killSwitch2);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println("SHUTDOWN THE KILLSWITCH!!!!!!!!");
        for (UniqueKillSwitch uniqueKillSwitch:uniqueKillSwitchList){
            uniqueKillSwitch.shutdown();
        }
        //endregion

    }

    public static void statelessPartitionHubTest(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        //A PartitionHub can be used to route elements from a common producer to a dynamic set of consumers.
        // The selection of consumer is done with a function. Each element can be routed to only one consumer.
        //The rate of the producer will be automatically adapted to the slowest consumer.
        // In this case, the hub is a Sink to which the single producer must be attached first.
        // Consumers can only be attached once the Sink has been materialized

        // A simple producer that publishes a new "message-n" every second
        Source<String,Cancellable> producer = Source.tick(Duration.ofSeconds(1),Duration.ofSeconds(1),"Message")
                .zipWith(Source.range(0,100), (a,b) -> a+"-"+b);

        // Attach a PartitionHub Sink to the producer. This will materialize to a
        // corresponding Source.
        // (We need to use toMat and Keep.right since by default the materialized
        // value to the left is used)
        RunnableGraph<Source<String,NotUsed>> runnableGraph = producer
                .toMat(
                        //The partitioner function takes two parameters; the first is the number of active consumers and the second is the stream element.
                        // The function should return the index of the selected consumer for the given element, i.e. int greater than or equal to 0 and less than number of consumers.
                        PartitionHub.of(String.class,
                                (size,element) -> Math.abs(element.hashCode()%size),
                                2,      //It is possible to define how many initial consumers that are required before it starts emitting any messages to the attached consumers. While not enough consumers have been attached messages are buffered and when the buffer is full the upstream producer is backpressured. No messages are dropped.
                                256), Keep.right()
                );

        // By running/materializing the producer, we get back a Source, which
        // gives us access to the elements published by the producer.
        Source<String,NotUsed> fromProducer = runnableGraph.run(materializer);


        //The resulting Source can be materialized any number of times, each materialization effectively attaching a new consumer.
        // If there are no consumers attached to this hub then it will not drop any elements but instead backpressure the upstream producer until consumers arrive.
        // This behavior can be tweaked by using an operator, for example .buffer with a drop strategy, or attaching a consumer that drops all messages.
        // If there are no other consumers, this will ensure that the producer is kept drained (dropping all elements)
        // and once a new consumer arrives and messages are routed to the new consumer it will adaptively slow down, ensuring no more messages are dropped.
        fromProducer.runForeach(msg -> System.out.println("consumer1: " + msg), materializer);
        boolean loop = true;
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                if (cmd.equals("stop")){
                    loop = false;
                    continue;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println("ATTACH NEW SINK!!!!");
        fromProducer.runForeach(msg -> System.out.println("consumer2: " + msg), materializer);

    }


    public static void statefulPartitionHubTest(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);
        //prefer routing to the fastest consumers.
        // The ConsumerInfo has an accessor queueSize that is approximate number of buffered elements for a consumer.
        // Larger value than other consumers could be an indication of that the consumer is slow.
        // Note that this is a moving target since the elements are consumed concurrently.
        // Here is an example of a hub that routes to the consumer with least buffered elements:

        Source<Integer, NotUsed> producer = Source.range(0, 100);

        // ConsumerInfo.queueSize is the approximate number of buffered elements for a consumer.
        // Note that this is a moving target since the elements are consumed concurrently.
        RunnableGraph<Source<Integer,NotUsed>> runnableGraph = producer
                .toMat(
                        PartitionHub.ofStateful(Integer.class,
                                () ->
                                        (info,element) -> {
                                            final List<Object> ids = info.getConsumerIds();
                                            int minValue = info.queueSize(0);
                                            long fastest = info.consumerIdByIdx(0);
                                            for (int i = 1; i < ids.size(); i++) {
                                                int value = info.queueSize(i);
                                                if (value < minValue) {
                                                    minValue = value;
                                                    fastest = info.consumerIdByIdx(i);
                                                }
                                            }
                                            return fastest;
                                        },
                                2,      //It is possible to define how many initial consumers that are required before it starts emitting any messages to the attached consumers. While not enough consumers have been attached messages are buffered and when the buffer is full the upstream producer is backpressured. No messages are dropped.
                                8),
                        Keep.right()
                );

        Source<Integer,NotUsed> fromProducer = runnableGraph.run(materializer);


        fromProducer.runForeach(msg -> System.out.println("consumer1: " + msg), materializer);
        fromProducer
                .throttle(10, Duration.ofMillis(100))
                .runForeach(msg -> System.out.println("consumer2: " + msg), materializer);

    }

}
