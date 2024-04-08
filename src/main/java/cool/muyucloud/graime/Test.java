package cool.muyucloud.graime;

import cool.muyucloud.graime.util.SceneTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;
import java.util.Map;

@TestOnly
public class Test {
    private final SceneTree sceneTree;

    public Test(Path path) {
        this.sceneTree = new SceneTree(path);
    }

    public void testStepInto(@NotNull String... paths) {
        Path path = this.sceneTree.getName();
        for (String i : paths) {
            if (i.equals("..")) {
                path = path.getParent();
            } else {
                path = path.resolve(i);
                sceneTree.stepInto(path);
            }
        }
        sceneTree.dump();
    }

    public void testInput(@NotNull String... pinyins) {
        for (String pinyin : pinyins) {
            System.out.println(this.sceneTree.getScores(pinyin));
        }
    }

    public void testLoad() {
        sceneTree.load();
    }

    public void testDump() {
        sceneTree.dump();
    }

    public void testDumpAll() {
        sceneTree.dumpAll();
    }

    public void testUpdate(Map<String, String> results) {
        for (Map.Entry<String, String> entry : results.entrySet()) {
            String pinyin = entry.getKey();
            String selection = entry.getValue();
            sceneTree.updateProducer(pinyin, selection);
        }
    }
}
