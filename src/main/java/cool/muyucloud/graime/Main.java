package cool.muyucloud.graime;

import cool.muyucloud.graime.model.ScoreProducer;
import cool.muyucloud.graime.model.TimeWeightedDictionModel;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        File file = new File("models/test/default.model");
        ScoreProducer producer = new TimeWeightedDictionModel(file);
        producer.dump(file);
    }

    public static void testSceneTree() {
        Map<String, String> train = new HashMap<>();
        train.put("abc", "test");
        train.put("pinyin", "candidate");

        Test test = new Test(Path.of("models/test"));
        test.testStepInto("testApp", "testBox");
        test.testInput("abc", "pinyin");
        test.testUpdate(train);
        test.testStepInto("testApp", "testBox1");
        test.testInput("abc", "pinyin");
        test.testDump();
    }
}