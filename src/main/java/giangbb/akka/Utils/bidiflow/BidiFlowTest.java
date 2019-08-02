package giangbb.akka.Utils.bidiflow;

import akka.NotUsed;
import akka.stream.BidiShape;
import akka.stream.FlowShape;
import akka.stream.javadsl.BidiFlow;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.GraphDSL;
import akka.util.ByteString;

public class BidiFlowTest {



    public static void bidiFlow(){

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
        BidiFlow<Message, ByteString,ByteString,Message, NotUsed> codec = BidiFlow.fromFunctions(message -> PingPongCodec.toBytes(message),bytestr -> PingPongCodec.fromBytes(bytestr));



    }
}
