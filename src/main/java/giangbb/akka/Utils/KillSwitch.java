package giangbb.akka.Utils;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.*;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

//A KillSwitch allows the completion of operators of FlowShape from the outside.
// It consists of a flow element that can be linked to an operator of FlowShape needing completion control. The KillSwitch interface allows to:
// - complete the stream(s) via shutdown()
// - fail the stream(s) via abort(Throwable error)
public class KillSwitch {
    //region UniqueKillSwitch
    //UniqueKillSwitch allows to control the completion of ONE materialized Graph of FlowShape
    public static void uniqueKillSwitchShutdown(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);


        final Source<Integer, NotUsed> countingSrc = Source.range(1,100).delay(Duration.ofSeconds(1), DelayOverflowStrategy.backpressure());
        final Sink<Integer, CompletionStage<Integer>> lastSink = Sink.last();

        final Pair<UniqueKillSwitch,CompletionStage<Integer>> stream = countingSrc.viaMat(KillSwitches.single(), Keep.right()).toMat(lastSink,Keep.both()).run(materializer);

        UniqueKillSwitch killSwitch = stream.first();
        CompletionStage<Integer> completionStage = stream.second();

        boolean loop = true;
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                if (cmd.equals("stop")){
                    loop = false;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        killSwitch.shutdown();
        try{
            int last = completionStage.toCompletableFuture().get();
            System.out.println("last:"+last);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void uniqueKillSwitchAbbort(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        //UniqueKillSwitch allows to control the completion of one materialized Graph of FlowShape
        final Source<Integer, NotUsed> countingSrc = Source.range(1,100).delay(Duration.ofSeconds(1), DelayOverflowStrategy.backpressure());
        final Sink<Integer, CompletionStage<Integer>> lastSink = Sink.last();

        final Pair<UniqueKillSwitch,CompletionStage<Integer>> stream = countingSrc.viaMat(KillSwitches.single(), Keep.right()).toMat(lastSink,Keep.both()).run(materializer);

        UniqueKillSwitch killSwitch = stream.first();
        CompletionStage<Integer> completionStage = stream.second();

        boolean loop = true;
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                if (cmd.equals("stop")){
                    loop = false;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        killSwitch.abort(new Exception("Boom!!!!!"));
        try{
            int last = completionStage.toCompletableFuture().exceptionally( e -> {
                e.printStackTrace();
                return -1;
            }).get();
            System.out.println("last:"+last);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //endregion

    //region SharedKillSwitch
    //SharedKillSwitch allows to control the completion of an arbitrary number operators of FlowShape.
    // It can be materialized multiple times via its flow method, and all materialized operators linked to it are controlled by the switch
    public static void sharedKillSwitchShutdown(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        final Source<Integer, NotUsed> countingSource = Source.range(0,100)
                                                                .delay(Duration.ofSeconds(1),DelayOverflowStrategy.backpressure());
        final Sink<Integer, CompletionStage<Integer>> lastSink = Sink.last();
        final SharedKillSwitch killSwitch = KillSwitches.shared("my-kill-switch");

        final CompletionStage<Integer> completionStage = countingSource.viaMat(killSwitch.flow() ,Keep.right()).toMat(lastSink,Keep.right()).run(materializer);

        final CompletionStage<Integer> completionStageDelayed = countingSource.delay(Duration.ofSeconds(1),DelayOverflowStrategy.backpressure())
                .viaMat(killSwitch.flow() ,Keep.right()).toMat(lastSink,Keep.right()).run(materializer);

        boolean loop = true;
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                if (cmd.equals("stop")){
                    loop = false;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        killSwitch.shutdown();
        try{
            int finalCount = completionStage.toCompletableFuture().get();
            int finalCountDelayed = completionStageDelayed.toCompletableFuture().get();
            System.out.println("finalCount:"+finalCount);
            System.out.println("finalCountDelayed:"+finalCountDelayed);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sharedKillSwitchAbbort(){
        ActorSystem actorSystem = ActorSystem.create("GiangbbSystem");
        Materializer materializer = ActorMaterializer.create(actorSystem);

        final Source<Integer, NotUsed> countingSource = Source.range(0,100)
                .delay(Duration.ofSeconds(1),DelayOverflowStrategy.backpressure());
        final Sink<Integer, CompletionStage<Integer>> lastSink = Sink.last();
        final SharedKillSwitch killSwitch = KillSwitches.shared("my-kill-switch");

        final CompletionStage<Integer> completionStage = countingSource.viaMat(killSwitch.flow() ,Keep.right()).toMat(lastSink,Keep.right()).run(materializer);

        final CompletionStage<Integer> completionStageDelayed = countingSource.delay(Duration.ofSeconds(1),DelayOverflowStrategy.backpressure())
                .viaMat(killSwitch.flow() ,Keep.right()).toMat(lastSink,Keep.right()).run(materializer);

        boolean loop = true;
        while (loop){
            try{
                System.out.println("Input a command:");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String cmd = in.readLine();

                if (cmd.equals("stop")){
                    loop = false;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        killSwitch.abort(new Exception("Boom!!!!"));
        try{
            int finalCount = completionStage.toCompletableFuture().exceptionally(e -> {
                e.printStackTrace();
                return -1;
            }).get();
            int finalCountDelayed = completionStageDelayed.toCompletableFuture().exceptionally( e -> {
                e.printStackTrace();
                return -1;
            }).get();
            System.out.println("finalCount:"+finalCount);
            System.out.println("finalCountDelayed:"+finalCountDelayed);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //endregion
}
