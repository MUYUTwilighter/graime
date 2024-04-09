package cool.muyucloud.graime.model;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public class HiddenMarkovModel extends ScoreProducer {
    @Override
    public @NotNull String getIdentifier() {
        return "hmm";
    }

    @Override
    protected void create() {

    }

    @Override
    public void load(@NotNull Path path) {

    }

    @Override
    public void dump(@NotNull Path path) {

    }

    @Override
    public @NotNull Map<String, Float> getScores(@NotNull String pinyin) {
        return null;
    }

    @Override
    public void update(@NotNull String pinyin, @NotNull String selection) {

    }

    @Override
    public @NotNull ScoreProducer copy() {
        return null;
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer producer, float weight) throws ClassCastException {
        return null;
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer producer) throws ClassCastException {
        return null;
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer... producers) throws ClassCastException {
        return null;
    }

    @Override
    public @NotNull ScoreProducer mergeWith(@NotNull Collection<ScoreProducer> producers) throws ClassCastException {
        return null;
    }

    @Override
    protected boolean canOneWayMergeWith(ScoreProducer producer) {
        return false;
    }

    @Override
    public boolean isDirty() {
        return false;
    }
}
