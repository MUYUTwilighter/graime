package cool.muyucloud.graime;

import cool.muyucloud.graime.util.Clock;
import org.jetbrains.annotations.TestOnly;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

@TestOnly
public class Main {
    public static void main(String[] args) {
        testSingleModel();
        testMultiModel();

    }

    public static void testSingleModel() {
        Clock.reset();
        System.out.println("====Testing single model====");
        Test test = new Test(Path.of("models/root"));
        test.jump(10000);
        float totalAcc = 0;
        List<Number> results = new LinkedList<>();
        for (int i = 0; i < 10; ++i) {
            test.update(700);
            float acc = test.input(300);
            results.add(acc);
            System.out.printf("round: %s, acc: %s%n", i, acc);
            totalAcc += acc;
        }
        dumpResult("test/single_r1.csv", results);
        System.out.println("Round 1, Avg Acc: " + totalAcc / 10);
        totalAcc = 0;
        test.jumpTo(10000);
        results.clear();
        for (int i = 0; i < 10; ++i) {
            test.update(700);
            float acc = test.input(300);
            results.add(acc);
            System.out.printf("round: %s, acc: %s%n", i, acc);
            totalAcc += acc;
            Clock.forward(86400000);
        }
        dumpResult("test/single_r2.csv", results);
        System.out.println("Round 2, Avg Acc: " + totalAcc / 10);
    }

    private static void testMultiModel() {
        Clock.reset();
        System.out.println("====Testing multi model====");
        Test test = new Test(Path.of("models/root"));
        test.update(10000);
        test.reset();
        test.stepInto("testApp");
        List<Number> results = new LinkedList<>();
        float totalAcc = 0;
        System.out.println(">> Before scene backward <<");
        for (int i = 0; i < 10; ++i) {
            test.stepInto("testBox" + i);
            float acc = test.input(700);
            System.out.printf("scene: %s, acc: %s%n", i, acc);
            results.add(acc);
            totalAcc += acc;
            test.stepInto("..");
            test.jump(300);
            Clock.forward(86400000);
        }
        dumpResult("test/multi_r1.csv", results);
        System.out.println("Round 1, Avg Acc: " + totalAcc / 10);
        totalAcc = 0;
        results.clear();
        test.jumpTo(700);
        System.out.println(">> After scene backward <<");
        for (int i = 0; i < 10; ++i) {
            test.stepInto("testBox" + i);
            float acc = test.input(300);
            System.out.printf("scene: %s, acc: %s%n", i, acc);
            results.add(acc);
            totalAcc += acc;
            test.stepInto("..");
            test.jump(700);
            Clock.forward(86400000);
        }
        dumpResult("test/multi_r2.csv", results);
        System.out.println("Round 2, Avg Acc: " + totalAcc / 10);
    }

    private static void dumpResult(String path, List<Number> data) {
        try (OutputStream stream = new FileOutputStream(path)) {
            StringBuilder builder = new StringBuilder("batch, accuracy\n");
            for (int i = 1; i <= data.size(); ++i) {
                builder.append(i).append(", ").append(data.get(i - 1)).append('\n');
            }
            builder.deleteCharAt(builder.length() - 1);
            stream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}