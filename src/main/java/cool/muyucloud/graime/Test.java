package cool.muyucloud.graime;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import cool.muyucloud.graime.util.BiType;
import cool.muyucloud.graime.util.Clock;
import cool.muyucloud.graime.util.SceneTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;

@TestOnly
public class Test {
    private static final List<BiType<String, String>> CORPUS;

    static {
        try (Reader reader = new FileReader("test/noted_corpus.json", StandardCharsets.UTF_8)) {
            JsonArray array = new Gson().fromJson(reader, JsonArray.class);
            CORPUS = new ArrayList<>(array.size());
            int i = 0;
            for (JsonElement element : array) {
                JsonArray raw = element.getAsJsonArray();
                BiType<String, String> wordAndPinyin = new BiType<>();
                wordAndPinyin.setA(raw.get(0).getAsString());
                wordAndPinyin.setB(raw.get(1).getAsString());
                CORPUS.add(wordAndPinyin);
                ++i;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int corpusSize() {
        return CORPUS.size();
    }

    private final SceneTree sceneTree;
    private int index = 0;
    private Random random = new SecureRandom();

    public Test(Path path) {
        this.sceneTree = new SceneTree(path);
    }

    public void stepInto(@NotNull String... paths) {
        Path path = this.sceneTree.getCurrentPath();
        for (String i : paths) {
            if (i.equals("..")) {
                path = path.getParent();
                this.sceneTree.stepInto(path);
            } else {
                path = path.resolve(i);
                sceneTree.stepInto(path);
            }
        }
    }

    public float input(int count) {
        float score = 0F;
        int success = 0;
        for (int i = this.index; i < this.index + count && i < CORPUS.size(); ++i) {
            BiType<String, String> record = CORPUS.get(i);
            Map<String, Float> scores = sceneTree.getScores(record.getB());
            List<Map.Entry<String, Float>> sorted = new ArrayList<>(scores.entrySet());
            sorted.sort(Map.Entry.comparingByValue());
            int p = 0;
            for (Map.Entry<String, Float> entry : sorted) {
                boolean result = entry.getKey().equals(record.getA());
                if (result) {
                    score += Math.max(1F / (p + 1), 0);
                    success++;
                    break;
                }
                p++;
            }
            sceneTree.updateProducer(record.getB(), record.getA());
            Clock.forward(Math.abs(random.nextInt() % 10000));
        }
        this.index += count;
        return score / success;
    }

    public void update(int count) {
        for (int i = this.index; i < this.index + count && i < CORPUS.size(); ++i) {
            BiType<String, String> record = CORPUS.get(i);
            this.sceneTree.updateProducer(record.getB(), record.getA());
            Clock.forward(Math.abs(random.nextInt() % 10000));
        }
        this.index += count;
    }

    public void reset() {
        this.index = 0;
    }

    public void jump(int count) {
        this.index += count;
        this.index = this.index >= CORPUS.size() ? (CORPUS.size() - 1) : this.index;
    }

    public void jumpTo(int index) {
        this.index = index >= CORPUS.size() ? (CORPUS.size() - 1) : index;
    }
}
