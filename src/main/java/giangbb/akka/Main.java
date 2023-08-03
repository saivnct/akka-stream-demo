package giangbb.akka;

import giangbb.akka.Utils.*;
import giangbb.akka.config.SpringConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;


//https://github.com/akka/akka/tree/master/akka-docs/src/test/java/jdocs/stream
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting application context");

        @SuppressWarnings("resource")
        AbstractApplicationContext ctx = new AnnotationConfigApplicationContext(
                SpringConfig.class);

        ctx.registerShutdownHook();

        //NOTE:
        //Source<Type, Mat value>.runwith(Sink<Type, Mat Value>) -> Mat value of Sink
        //Sink<Type, Mat value>.runwith(Source<Type, Mat Value>) -> Mat value of Source
        //Flow<Type In, Type Out, Mat value>.runwith(Source<Type, Mat Value>, Sink<Type, Mat Value>) -> Pair<Mat value of Source, Mat value of Sink>


        //NOTE:
        //RunnableGraph<Mat value of Source> graph2 = Source<Type, Mat value>
        //                .via(Flow<Type In, Type Out, Mat value>)
        //                .to(Sink<Type, Mat value>)
        // => When chaining operations on a Source or Flow, the type of the auxiliary information—called the “materialized value”—is given by the leftmost starting point
        // => To make RunnableGraph return the materialized value of a Sink:
        //RunnableGraph<Mat value of Sink> graph2 = Source<Type, Mat value>
        //                .via(Flow<Type In, Type Out, Mat value>)
        //                .toMat(Sink<Type, Mat value>, Keep.right());
        //




//        FlowBasic.demoSimpleStream1();
//        FlowBasic.demoSimpleStream2();
//        FlowBasic.demoCancellable();
//        FlowBasic.demoRunableGraph();
//        FlowBasic.demoCombiningMaterializedValues();
//        FlowBasic.demoSourcePreMaterialization();
//        FlowBasic.demoMaterializerLifeCycle();

//        Graphs.simpleGraph1Sink();
//        Graphs.simpleGraph2Sink();
//        Graphs.graphReadMaterializeValueOfManySinks();

//        PartialGraphs.graphGetMaxFrom3Source();
//        PartialGraphs.constructingSourceShape();
//        PartialGraphs.constructingFlowShape();
//        PartialGraphs.combineSource();
        PartialGraphs.combineSink();

//        Modularity.createNesting();
//        Modularity.composingComplexSystem();
//        Modularity.partialGrapth();
//        Modularity.combinePartialGrapth();

//        KillSwitch.uniqueKillSwitchShutdown();
//        KillSwitch.uniqueKillSwitchAbbort();
//        KillSwitch.sharedKillSwitchShutdown();
//        KillSwitch.sharedKillSwitchAbbort();

//        Hub.mergeHubTest();
//        Hub.broadCastHubTest();
//        Hub.simplePublishSubscribeService();
//        Hub.statelessPartitionHubTest();
//        Hub.statefulPartitionHubTest();
    }





}










