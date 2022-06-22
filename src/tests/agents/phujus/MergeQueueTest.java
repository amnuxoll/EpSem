package tests.agents.phujus;

import agents.phujus.PhuJusAgent;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

import java.util.HashSet;

@EpSemTestClass
public class MergeQueueTest {

    @EpSemTest
    public void testMergeQueue() {
        PhuJusAgent.MergeQueue mq = new PhuJusAgent.MergeQueue();

        HashSet<Integer> toAdd = new HashSet<>();
        toAdd.add(3);
        toAdd.add(5);

        mq.add(toAdd);

        toAdd = new HashSet<>();
        toAdd.add(8);
        toAdd.add(0);

        mq.add(toAdd);

        toAdd = new HashSet<>();
        toAdd.add(9);

        mq.add(toAdd);

        toAdd = new HashSet<>();
        toAdd.add(10);
        toAdd.add(17);
        toAdd.add(2);

        mq.add(toAdd);

        toAdd = new HashSet<>();
        toAdd.add(2);
        toAdd.add(32);
        toAdd.add(3);
        toAdd.add(5);

        mq.add(toAdd);
        System.out.println("Here");
    }
}
