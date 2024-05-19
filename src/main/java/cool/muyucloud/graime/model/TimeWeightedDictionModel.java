package cool.muyucloud.graime.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cool.muyucloud.graime.annotation.ImplementedProducer;
import cool.muyucloud.graime.util.BiType;
import cool.muyucloud.graime.util.Clock;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@ImplementedProducer("default")
public class TimeWeightedDictionModel extends ScoreProducer implements LexiconObtainable {
    private static final Gson GSON = new Gson();

    private Map<String, Map<String, BiType<Float, Long>>> map;
    private boolean dirty;

    public TimeWeightedDictionModel() {
        super();
        this.dirty = true;
    }

    public TimeWeightedDictionModel(Path path) {
        super(path);
        this.dirty = false;
    }

    public TimeWeightedDictionModel(File file) {
        super(file);
        this.dirty = false;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "default";
    }

    @Override
    protected void create() {
        this.map = new HashMap<>();
    }

    @Override
    public void load(@NotNull File file) {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            this.map = new HashMap<>();
            JsonObject map = GSON.fromJson(reader, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                String pinyin = entry.getKey();
                JsonObject candidatesRaw = entry.getValue().getAsJsonObject();
                Map<String, BiType<Float, Long>> candidates = new HashMap<>();
                for (Map.Entry<String, JsonElement> record : candidatesRaw.entrySet()) {
                    String candidate = record.getKey();
                    JsonArray biTypeRaw = record.getValue().getAsJsonArray();
                    BiType<Float, Long> biType = new BiType<>(biTypeRaw.get(0).getAsFloat(), biTypeRaw.get(1).getAsLong());
                    candidates.put(candidate, biType);
                }
                this.map.put(pinyin, candidates);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /* Read file */
        this.dirty = false;
    }

    @Override
    public void dump(@NotNull Path path) {
        this.dump(path.resolve(this.getIdentifier() + POST_FIX).toFile());
    }

    @Override
    public void dump(@NotNull File file) {
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new FileSystemException("Failed to create file.");
                }
            }
            StringBuilder builder = new StringBuilder("{");
            for (Map.Entry<String, Map<String, BiType<Float, Long>>> entry : this.map.entrySet()) {
                String pinyin = entry.getKey();
                Map<String, BiType<Float, Long>> candidates = entry.getValue();
                builder.append('"').append(pinyin).append("\":{");
                for (Map.Entry<String, BiType<Float, Long>> record : candidates.entrySet()) {
                    String candidate = record.getKey();
                    Float score = record.getValue().getA();
                    Long time = record.getValue().getB();
                    builder.append('"').append(candidate)
                        .append("\":[")
                        .append(score).append(',').append(time)
                        .append("],");
                }
                // remove redundant ','
                int len = builder.length();
                builder.deleteCharAt(len - 1);
                builder.append("},");
            }
            // remove redundant ','
            int len = builder.length();
            builder.deleteCharAt(len - 1);
            builder.append('}');
            // start dump
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write(builder.toString());
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Map<String, Float> getScores(@NotNull String pinyin) {
        Map<String, BiType<Float, Long>> candidates = this.map.getOrDefault(pinyin, new HashMap<>());
        this.map.put(pinyin, candidates);
        Map<String, Float> scores = new HashMap<>();
        for (Map.Entry<String, BiType<Float, Long>> entry : candidates.entrySet()) {
            String candidate = entry.getKey();
            BiType<Float, Long> record = entry.getValue();
            Long time = record.getB();
            Long thisTime = Clock.getTime();
            Float score = this.timeFade(record.getA(), time);
            record.setA(score);
            record.setB(thisTime);
            scores.put(candidate, score);
        }
        this.dirty = true;
        return scores;
    }

    @Override
    public void update(@NotNull String pinyin, @NotNull String selection) {
        this.dirty = true;
        Map<String, BiType<Float, Long>> candidates = this.map.getOrDefault(pinyin, new HashMap<>());
        this.map.put(pinyin, candidates);
        BiType<Float, Long> record = candidates.getOrDefault(selection, new BiType<>(0.7F, Clock.getTime()));
        Long time = record.getB();
        float score = this.timeFade(record.getA(), time);
        record.setA(this.feedback(score));
        record.setB(Clock.getTime());
        candidates.put(selection, record);
        this.dirty = true;
    }

    @Override
    public @NotNull ScoreProducer copy() {
        TimeWeightedDictionModel copied = new TimeWeightedDictionModel();
        copied.dirty = true;
        for (Map.Entry<String, Map<String, BiType<Float, Long>>> entry : this.map.entrySet()) {
            String pinyin = entry.getKey();
            Map<String, BiType<Float, Long>> candidates = entry.getValue();
            Map<String, BiType<Float, Long>> thatCandidates = new HashMap<>();
            for (Map.Entry<String, BiType<Float, Long>> record : candidates.entrySet()) {
                String candidate = record.getKey();
                float score = record.getValue().getA();
                thatCandidates.put(candidate, new BiType<>(score, Clock.getTime()));
            }
            copied.map.put(pinyin, thatCandidates);
        }
        return copied;
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer producer, float weight) throws ClassCastException {
        if (this.canOneWayMergeWith(producer)) {
            return this.mergeWithLexicon((LexiconObtainable) producer, weight);
        } else if (producer.canOneWayMergeWith(this)) {
            return producer.mergeWith(this, 1 - weight);
        } else {
            throw new ClassCastException("Merge option cannot be applied between StaticDictionModel and " + producer);
        }
    }

    private @NotNull TimeWeightedDictionModel mergeWithLexicon(@NotNull LexiconObtainable producer, float weight) throws ClassCastException {
        TimeWeightedDictionModel mergedProducer = (TimeWeightedDictionModel) this.copy();
        Map<String, Map<String, BiType<Float, Long>>> merged = mergedProducer.map;
        for (Map.Entry<String, Map<String, Float>> entry : producer.getLexicon().entrySet()) {
            String pinyin = entry.getKey();
            Map<String, Float> candidates = entry.getValue();
            Map<String, BiType<Float, Long>> mergedCandidates = merged.getOrDefault(pinyin, new HashMap<>());
            for (Map.Entry<String, Float> candidatesEntry : candidates.entrySet()) {
                String candidate = candidatesEntry.getKey();
                Float score = candidatesEntry.getValue();
                BiType<Float, Long> mergedRecord = mergedCandidates.getOrDefault(candidate, new BiType<>(0F, Clock.getTime()));
                Float mergedScore = mergedRecord.getA();
                mergedScore = score * weight + mergedScore * (1 - weight);
                mergedRecord.setA(mergedScore);
                mergedCandidates.put(candidate, mergedRecord);
            }
            merged.put(pinyin, mergedCandidates);
        }
        return mergedProducer;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean canOneWayMergeWith(ScoreProducer producer) {
        return producer instanceof LexiconObtainable;
    }

    @Override
    public Map<String, Map<String, Float>> getLexicon() {
        Map<String, Map<String, Float>> lexicon = new HashMap<>();
        for (Map.Entry<String, Map<String, BiType<Float, Long>>> entry : this.map.entrySet()) {
            String pinyin = entry.getKey();
            Map<String, BiType<Float, Long>> candidates = entry.getValue();
            Map<String, Float> thatCandidates = new HashMap<>();
            for (Map.Entry<String, BiType<Float, Long>> record : candidates.entrySet()) {
                String candidate = record.getKey();
                float score = record.getValue().getA();
                thatCandidates.put(candidate, score);
            }
            lexicon.put(pinyin, thatCandidates);
        }
        return lexicon;
    }

    private float timeFade(float score, long time) {
        return (float) (score * Math.pow(2, - (double) (Clock.getTime() - time) / 86400000));
    }

    private float feedback(float score) {
        return (float) Math.sin(Math.PI * score / 2);
    }
}
