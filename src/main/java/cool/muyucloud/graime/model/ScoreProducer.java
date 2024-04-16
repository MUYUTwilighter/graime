package cool.muyucloud.graime.model;

import com.sun.jdi.request.DuplicateRequestException;
import cool.muyucloud.graime.annotations.ImplementedProducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

public abstract class ScoreProducer {
    public static final String POST_FIX = ".model";
    private static final Map<String, Class<? extends ScoreProducer>> REGISTRY = new HashMap<>();

    static {
        ServiceLoader.load(ScoreProducer.class).stream()
            .map(ServiceLoader.Provider::type)
            .filter(clazz -> clazz.isAnnotationPresent(ImplementedProducer.class) &&
                ScoreProducer.class.isAssignableFrom(clazz) &&
                !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
            .forEach(clazz -> {
                ImplementedProducer anno = clazz.getAnnotation(ImplementedProducer.class);
                register(anno.value(), clazz);
            });
    }

    /**
     * Register a fully implemented producer model class
     * that allows generic loader to autoload the producer model with correct load method.
     *
     * @param identifier The file name without post-fix
     *                   that help generic loader tell the type of the producer model.
     *                   It should match {@code ScoreProducer::isIdentifierValid} and not duplicated.
     * @param cl         Class of the producer model to register
     */
    public static void register(@NotNull String identifier, @NotNull Class<? extends ScoreProducer> cl) {
        if (REGISTRY.containsKey(identifier)) {
            throw new DuplicateRequestException("Duplicated identifier");
        }
        if (REGISTRY.containsValue(cl)) {
            throw new DuplicateRequestException("Duplicated producer class");
        }
        if (!isIdentifierValid(identifier)) {
            throw new IllegalArgumentException("Not a valid identifier");
        }
        REGISTRY.put(identifier, cl);
    }

    /**
     * Generically find the model file from a path
     * and classify the type of the producer model then load.
     *
     * @param path Path that might contain a model file.
     * @return The loaded producer model instance, {@code null} if identifier not found.
     */
    public static @Nullable ScoreProducer genericLoad(@NotNull Path path) {
        for (File file : Objects.requireNonNull(path.toFile().listFiles())) {
            if (!isModelFile(file)) {
                continue;
            }
            ScoreProducer producer = genericLoad(file);
            if (producer != null) {
                return producer;
            }
        }
        return null;
    }

    /**
     * Generically tells the type of the producer model and load.
     *
     * @param file The File instance of the model file ready to load.
     * @return The loaded producer model instance, {@code null} if identifier not found.
     */
    public static @Nullable ScoreProducer genericLoad(@NotNull File file) {
        String name = file.getName();
        String identifier = name.substring(0, name.lastIndexOf('.'));
        Class<? extends ScoreProducer> cl = REGISTRY.get(identifier);
        if (cl == null) {
            return null;
        }
        try {
            Constructor<? extends ScoreProducer> constructor = cl.getDeclaredConstructor(File.class);
            return constructor.newInstance(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a defaulted producer model instance of specific identifier.
     *
     * @param identifier Identifier to tell the type of producer models.
     * @return Newly created producer model, {@code null} if identifier not found.
     */
    public static @Nullable ScoreProducer create(String identifier) {
        Class<? extends ScoreProducer> cl = REGISTRY.get(identifier);
        if (cl == null) {
            return null;
        }
        try {
            Constructor<? extends ScoreProducer> constructor = cl.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get class of the corresponding producer.
     *
     * @param identifier Identifier
     * @return Class of the corresponding producer, null if not found.
     */
    public static @Nullable Class<? extends ScoreProducer> get(String identifier) {
        return REGISTRY.get(identifier);
    }

    private static boolean isModelFile(File file) {
        return file.isFile() && file.getName().matches("([A-za-z0-9]|\\.|_)+\\.model");
    }

    private static boolean isIdentifierValid(String identifier) {
        return identifier.matches("([A-za-z0-9]|\\.|_)+");
    }

    /**
     * Create a defaulted score producer instance.
     */
    public ScoreProducer() {
        this.create();
    }

    /**
     * Load the producer model from file system.<br/>
     * Specific filename will be autofill.
     *
     * @param path File instance of the model file.
     */
    public ScoreProducer(Path path) {
        this.load(path.resolve(this.getIdentifier() + POST_FIX).toFile());
    }

    /**
     * Load the producer model from file system.<br/>
     * Specific filename should be included.
     *
     * @param file File instance of the model file.
     */
    public ScoreProducer(File file) {
        this.load(file);
    }

    /**
     * Get Identifier of the class.
     *
     * @return Identifier.
     */
    public abstract @NotNull String getIdentifier();

    /**
     * Reset this into a defaulted score producer instance.
     */
    protected abstract void create();

    /**
     * Load the producer model from file system.<br/>
     * Specific filename should be included.
     *
     * @param file File instance of the model file.
     */
    public abstract void load(@NotNull File file);

    /**
     * Save and dump the producer model into target path.<br/>
     * Filename will be autofill.
     *
     * @param path Target path.
     */
    public abstract void dump(@NotNull Path path);

    /**
     * Save and dump the producer model into target location.<br/>
     * Filename will be included.
     *
     * @param file Target path.
     */
    public abstract void dump(@NotNull File file);

    /**
     * Query a sequence of possible candidate words of the pinyin input,
     * along with the scores of the words.
     *
     * @param pinyin Pinyin input.
     * @return A map of all the possible candidate words and their scores.
     */
    public abstract @NotNull Map<String, Float> getScores(@NotNull String pinyin);

    /**
     * Update (or train) the producer model with the user's selection.
     *
     * @param pinyin    Pinyin input by the user.
     * @param selection Candidate word that is selected by the user.
     */
    public abstract void update(@NotNull String pinyin, @NotNull String selection);

    /**
     * Fully copy a producer model.
     *
     * @return A fully copied producer model
     * that would no longer bring changes on the original one.
     */
    public abstract @NotNull ScoreProducer copy();

    /**
     * Merge this producer model with another and create a new one.<br/>
     * No effect on both the original producers.
     *
     * @param producer Another producer to be merged.
     * @param weight   Weight of another producer, in bound between 0 and 1.
     * @return Newly merged producer.
     */
    public abstract @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer producer, float weight) throws ClassCastException;


    /**
     * Merge this producer model with another and create a new one, using default weight.<br/>
     * No effect on both the original producers.<br/>
     *
     * @param producer Another producer to be merged.
     * @return Newly merged producer.
     */
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer producer) throws ClassCastException {
        return this.mergeWith(producer, 0.5F);
    }


    /**
     * Merge this producer model with others and create a new one.<br/>
     * All producers get same weight. <br/>
     * No effect on both the original producers.
     *
     * @param producers Other producers to be merged.
     * @return Newly merged producer.
     */
    public @NotNull ScoreProducer mergeWith(@NotNull ScoreProducer... producers) throws ClassCastException {
        ScoreProducer merged = this.copy();
        for (int i = 0; i < producers.length; ++i) {
            ScoreProducer producer = producers[i];
            merged.mergeWith(producer, 1F / (i + 1));
        }
        return merged;
    }


    /**
     * Merge this producer model with others and create a new one.<br/>
     * All producers get same weight. <br/>
     * No effect on both the original producers.
     *
     * @param producers Other producers to be merged.
     * @return Newly merged producer.
     */
    public @NotNull ScoreProducer mergeWith(@NotNull Collection<ScoreProducer> producers) throws ClassCastException {
        ScoreProducer merged = this.copy();
        int i = 0;
        for (ScoreProducer producer : producers) {
            merged.mergeWith(producer, 1F / (i + 1));
            ++i;
        }
        return merged;
    }

    /**
     * Whether this producer model can merge with the producer provided,
     * or the provided producer can merge with this producer model.
     *
     * @param producer Another producer to get merged.
     * @return {@code true} if this producer and the producer provided can get merged.
     */
    public boolean canMergeWith(ScoreProducer producer) {
        return this.canOneWayMergeWith(producer) || producer.canOneWayMergeWith(this);
    }

    /**
     * Whether the current producer model can merge with the producer provided.<br/>
     * Notice that this is only a one-way detect.
     *
     * @param producer Another producer to get merged.
     * @return {@code true} if this producer and the producer provided can get merged.
     */
    protected abstract boolean canOneWayMergeWith(ScoreProducer producer);

    /**
     * Whether the instance has been changed and should be dumped.
     *
     * @return {@code true} if the instance should be dumped.
     */
    public abstract boolean isDirty();
}
