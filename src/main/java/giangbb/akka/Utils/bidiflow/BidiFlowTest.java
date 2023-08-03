package giangbb.akka.Utils.bidiflow;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.BidiShape;
import akka.stream.FlowShape;
import akka.stream.javadsl.*;
import akka.util.ByteString;

import java.util.concurrent.CompletionStage;

public class BidiFlowTest {

    /**
     * A bidirectional flow of elements that consequently has two inputs and two
     * outputs, arranged like this:
     *
     * {{{
     *        +------+
     *  In1 ~>|      |~> Out1
     *        | bidi |
     * Out2 <~|      |<~ In2
     *        +------+
     * }}}
     */

    public static void bidiFlow(){
        // Create an Akka system
        final ActorSystem actorSystem = ActorSystem.create("GianbbSystem");

        //C1: Create bidiflow using graph
        BidiFlow<Message, ByteString,ByteString,Message, NotUsed> codecVerbose = BidiFlow.fromGraph(
                GraphDSL.create(
                        b -> {
                            FlowShape<Message,ByteString> top = b.add(Flow.of(Message.class).map(message -> PingPongCodec.toBytes(message)));
                            FlowShape<ByteString,Message> bottom = b.add(Flow.of(ByteString.class).map(bytestr -> PingPongCodec.fromBytes(bytestr)));
                            return BidiShape.fromFlows(top,bottom);
                        }
                )
        );

        //C2: Create bidiflow using convenience Function
        BidiFlow<Message, ByteString, ByteString, Message, NotUsed> codec = BidiFlow.fromFunctions(message -> PingPongCodec.toBytes(message),bytestr -> PingPongCodec.fromBytes(bytestr));
    }
}
