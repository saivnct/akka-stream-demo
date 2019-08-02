package giangbb.akka;

import giangbb.akka.Utils.Graphs;
import giangbb.akka.Utils.PartialGraphs;
import giangbb.akka.config.SpringConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;


//https://github.com/akka/akka/tree/master/akka-docs/src/test/java/jdocs/stream
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting application context");

        @SuppressWarnings("resource")
        AbstractApplicationContext ctx = new AnnotationConfigApplicationContext(
                SpringConfig.class);

        ctx.registerShutdownHook();

//        FlowBasic.demoSimpleStream();
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
    }





}










