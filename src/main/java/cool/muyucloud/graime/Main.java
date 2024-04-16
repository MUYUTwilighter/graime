package cool.muyucloud.graime;

import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;

@TestOnly
public class Main {
    public static void main(String[] args) {
        testSingleModel();
        testMultiModel();
    }

    public static void testSingleModel() {
        Test test = new Test(Path.of("models/test"));
        test.update(7000);
        System.out.println(test.input(3000));
    }

    private static void testMultiModel() {
        Test test = new Test(Path.of("models/test"));
        test.stepInto("testApp");
        float score = 0;
        for (int i = 0; i < 25; ++i) {
            test.stepInto("testBox" + i);
            test.update(50);
            score += test.input(30);
            test.stepInto("..");
            test.jump(20);
        }
        test.jumpTo(80);
        for (int i = 0; i < 25; ++i) {
            test.stepInto("testBox" + i);
            score += test.input(20);
            test.jump(80);
        }
        System.out.println(score / 50);
    }
}