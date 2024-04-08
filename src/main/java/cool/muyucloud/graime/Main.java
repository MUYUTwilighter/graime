package cool.muyucloud.graime;

import java.nio.file.Path;
import java.util.*;

public class Main {
    public static void main(String[] args) {
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