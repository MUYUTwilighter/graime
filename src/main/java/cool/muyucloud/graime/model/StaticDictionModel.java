package cool.muyucloud.graime.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@TestOnly
public class StaticDictionModel extends ScoreProducer implements LexiconObtainable {
    private final Map<String, Map<String, Float>> map = new HashMap<>();
    private boolean dirty = false;

    @Override
    public @NotNull String getIdentifier() {
        return "test";
    }

    @Override
    protected void create() {
        this.dirty = true;
    }

    @Override
    public void load(@NotNull Path path) {
        try (InputStream stream = new FileInputStream(path.toFile())) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /* Read file */
        this.dirty = true;
    }

    @Override
    public void dump(@NotNull Path path) {
        File file = path.resolve(this.getIdentifier() + ScoreProducer.POST_FIX).toFile();
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            new FileOutputStream(path.resolve(this.getIdentifier() + ScoreProducer.POST_FIX).toFile()).close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Map<String, Float> getScores(@NotNull String pinyin) {
        Map<String, Float> candidates = this.map.getOrDefault(pinyin, new HashMap<>());
        this.map.put(pinyin, candidates);
        return candidates;
    }

    @Override
    public void update(@NotNull String pinyin, @NotNull String selection) {
        this.dirty = true;
        Map<String, Float> candidates = this.map.getOrDefault(pinyin, new HashMap<>());
        this.map.put(pinyin, candidates);
        float score = candidates.getOrDefault(selection, 0.1F);
        score *= 1.1F;
        candidates.put(selection, score);
    }

    @Override
    public @NotNull ScoreProducer copy() {
        StaticDictionModel copied = new StaticDictionModel();
        copied.dirty = true;
        for (Map.Entry<String, Map<String, Float>> entry : this.map.entrySet()) {
            String pinyin = entry.getKey();
            Map<String, Float> candidates = entry.getValue();
            Map<String, Float> thatCandidates = new HashMap<>();
            for (Map.Entry<String, Float> stringFloatEntry : candidates.entrySet()) {
                String candidate = stringFloatEntry.getKey();
                float score = stringFloatEntry.getValue();
                thatCandidates.put(candidate, score);
            }
            copied.map.put(pinyin, thatCandidates);
        }
        return copied;
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer producer) throws ClassCastException {
        return this.mergeWith(producer, 0.5F);
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer... producers) throws ClassCastException {
        ScoreProducer merged = this.copy();
        for (int i = 0; i < producers.length; ++i) {
            ScoreProducer producer = producers[i];
            merged.mergeWith(producer, 1F / (i + 1));
        }
        return merged;
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull Collection<ScoreProducer> producers) throws ClassCastException {
        ScoreProducer merged = this.copy();
        int i = 0;
        for (ScoreProducer producer : producers) {
            merged.mergeWith(producer, 1F / (i + 1));
            ++i;
        }
        return merged;
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer producer, float weight) throws ClassCastException {
        if (this.canOneWayMergeWith(producer)) {
            return this.mergeWithLexicon((LexiconObtainable) producer, weight);
        } else {
            return producer.mergeWith(this);
        }
    }

    private @NotNull StaticDictionModel mergeWithLexicon(@NotNull LexiconObtainable producer, float weight) throws ClassCastException {
        StaticDictionModel mergedProducer = (StaticDictionModel) this.copy();
        Map<String, Map<String, Float>> merged = mergedProducer.getLexicon();
        for (Map.Entry<String, Map<String, Float>> entry : producer.getLexicon().entrySet()) {
            String pinyin = entry.getKey();
            Map<String, Float> candidates = entry.getValue();
            Map<String, Float> mergedCandidates = merged.getOrDefault(pinyin, new HashMap<>());
            merged.put(pinyin, mergedCandidates);
            for (Map.Entry<String, Float> candidatesEntry : candidates.entrySet()) {
                String candidate = candidatesEntry.getKey();
                Float score = candidatesEntry.getValue();
                Float mergedScore = mergedCandidates.getOrDefault(candidate, null);
                if (mergedScore == null) {
                    mergedScore = score;
                } else {
                    mergedScore = score * weight + mergedScore * (1 - weight);
                }
                mergedCandidates.put(candidate, mergedScore);
            }
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
        return this.map;
    }
}
