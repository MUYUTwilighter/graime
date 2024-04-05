package cool.muyucloud.graime.struct;

import cool.muyucloud.graime.model.ScoreProducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class SceneTree {
    private final @NotNull Path parent;
    private final @NotNull Path name;
    private @NotNull Node root;
    private final @NotNull Map<Path, Node> nodes = new HashMap<>();
    private @NotNull Node current;
    private @Nullable Node old = null;
    private long lastStep = new Date().getTime();

    /**
     * Load SceneTree from local files
     *
     * @param path Path of SceneTree files, its filename indicates the name of the SceneTree.
     */
    public SceneTree(@NotNull Path path) {
        if (!path.toFile().exists()) {
            throw new RuntimeException("Root path %s not found".formatted(path));
        }
        this.parent = path.toAbsolutePath().getParent();
        this.name = path.getFileName();
        this.root = traverseLoad(this.name);
        this.current = this.root;
        if (!this.root.hasProducer()) {
            ScoreProducer producer = ScoreProducer.create("default");
            this.root.setProducer(producer);
        }
    }

    public @NotNull Path getName() {
        return this.name;
    }

    /**
     * Step into another scene, create if the scene node does not exist.<br/>
     * Involves node switching record.
     *
     * @param path Path of a scene node.
     */
    public void stepInto(Path path) {
        if (this.current.hasProducer()) {
            this.old = this.current;
            this.lastStep = new Date().getTime();
        }
        this.current = this.nodes.get(path);
        if (this.current == null) {
            this.current = this.add(path);
        }
    }

    /**
     * Step into another scene, create if the scene node does not exist.<br/>
     * Involves node switching record.
     *
     * @param program The program that the scene to input.
     * @param box     Path to the input box.
     */
    public void stepInto(Path program, Path box) {
        this.stepInto(this.calcRelative(program, box));
    }

    /**
     * Calculate all possible words and their scores. <br/>
     * Higher scores represents stronger possibilities.
     *
     * @param pinyin Pinyin input.
     */
    public Map<String, Float> getScores(String pinyin) {
        if (!this.current.hasProducer()) {
            this.current.createProducer(this.root.getProducer());
        }
        ScoreProducer current = this.current.getProducer();
        if (current == null) {
            this.current.createProducer(this.root.getProducer());
            current = this.current.getProducer();
        }
        if (this.old == null) {
            return current.getScores(pinyin);
        }
        ScoreProducer old = this.old.getProducer();
        float weight = this.calcWeight();
        ScoreProducer producer = current.mergeWith(old, weight);
        return producer.getScores(pinyin);
    }

    /**
     * Update (or train) the producer model with the user's selection.
     *
     * @param pinyin    Pinyin input by the user.
     * @param selection Candidate word that is selected by the user.
     */
    public void updateProducer(String pinyin, String selection) {
        assert this.current.getProducer() != null;
        assert this.root.getProducer() != null;
        this.current.getProducer().update(pinyin, selection);
        this.root.getProducer().update(pinyin, selection);
    }

    /**
     * Load SceneTree from file system and overwrite.
     */
    public void load() {
        this.nodes.clear();
        this.root = traverseLoad(this.parent.resolve(this.name));
    }

    /**
     * Dump dirty part of SceneTree into file system.
     */
    public void dump() {
        for (Node node : nodes.values()) {
            if (node.isDirty()) {
                node.dump(this.parent);
            }
        }
    }

    /**
     * Dump every scene nodes of SceneTree into file system.
     */
    public void dumpAll() {
        for (Node node : nodes.values()) {
            node.dump(this.parent);
        }
    }

    private void traverseDelete(Path path) {
        Node node = this.nodes.get(path);
        if (node.isRoot()) {
            node.getParent().getChildren().remove(node);
            this.nodes.remove(path);
        }
        for (Node child : node.getChildren()) {
            this.traverseDelete(child.getPath());
        }
    }

    private Node add(Path path) {
        if (!path.getName(0).equals(this.name)) {
            throw new IllegalArgumentException("Relative path does not belong to this SceneTree");
        }
        Node former = this.root, node = null;
        for (int i = 1; i < path.getNameCount(); ++i) {
            Path route = path.subpath(0, i + 1);
            node = this.nodes.get(route);
            if (node == null) {
                node = Node.create(path);
                node.setParent(former);
                this.nodes.put(route, node);
            }
            former = node;
        }
        return node;
    }

    private float calcWeight() {
        long duration = (new Date().getTime() - this.lastStep) / 1000;
        return (float) 1 / (duration + 2);
    }

    private Path calcRelative(Path program, Path box) {
        program = Path.of(String.valueOf(program.hashCode()));
        return this.name.resolve(program).resolve(box);
    }

    private Node traverseLoad(@NotNull Path path) {
        Path absolute = this.parent.resolve(path);
        ScoreProducer producer = ScoreProducer.genericLoad(absolute);
        Node node = new Node(path);
        node.setProducer(producer);
        for (File subFile : Objects.requireNonNull(absolute.toFile().listFiles())) {
            if (subFile.isDirectory()) {
                Path subFileName = subFile.toPath().getFileName();
                Node subNode = traverseLoad(path.resolve(subFileName));
                subNode.setParent(node);
            }
        }
        this.nodes.put(path, node);
        return node;
    }

    private static class Node {
        private final Path path;
        private @Nullable ScoreProducer producer = null;
        private final Set<Node> children = new HashSet<>();
        private @Nullable Node parent = null;
        private boolean dirty = false;

        /**
         * Instantiate a Node
         *
         * @param path Relative path of a scene node.
         */
        public Node(Path path) {
            this.path = path;
        }

        /**
         * Create a node that is new, which involves dirty-marking.
         *
         * @param path Relative path of a scene node.
         */
        public static Node create(Path path) {
            Node node = new Node(path);
            node.markDirty();
            return node;
        }

        /**
         * Get relative path of a node.
         */
        public Path getPath() {
            return this.path;
        }

        /**
         * Whether the scene node contains a producer.
         */
        public boolean hasProducer() {
            return producer != null;
        }

        protected @Nullable ScoreProducer getProducer() {
            return this.producer;
        }

        protected void setProducer(@Nullable ScoreProducer producer) {
            this.producer = producer;
        }

        /**
         * Generically create a producer for the node. <br/>
         * If the node has no parent, create a defaulted score producer model. <br/>
         * If the node has the parent, create the score producer with its siblings and
         * root node of the SceneTree.
         *
         * @param root Root node of the SceneTree.
         */
        public void createProducer(ScoreProducer root) {
            if (this.isRoot()) {
                this.setProducer(ScoreProducer.create("default"));
                return;
            }
            Collection<Node> siblings = this.parent.getChildren();
            Collection<ScoreProducer> producers = new ArrayList<>();
            for (Node sibling : siblings) {
                if (sibling.hasProducer()) {
                    producers.add(sibling.getProducer());
                }
            }
            this.producer = root.mergeWith(producers);
        }

        protected Set<Node> getChildren() {
            return this.children;
        }

        protected @Nullable Node getParent() {
            return this.parent;
        }

        protected void setParent(@Nullable Node parent) {
            this.parent = parent;
            if (parent != null) {
                parent.getChildren().add(this);
            }
        }

        public boolean isRoot() {
            return this.parent == null;
        }

        /**
         * Mark the node as dirty, which means the node should be dumped in proper situations.<br/>
         */
        public void markDirty() {
            this.dirty = true;
        }

        /**
         * Whether the node is dirty and should be dumped in proper situations.<br/>
         */
        public boolean isDirty() {
            return this.dirty || (this.producer != null && this.producer.isDirty());
        }

        /**
         * Dump the node into file system.
         *
         * @param root Absolute path to the root node of the SceneTree.
         */
        public void dump(@NotNull Path root) {
            Path absolute = root.resolve(this.getPath());
            File dir = absolute.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (this.isProducerDirty()) {
                this.getProducer().dump(absolute);
            }
        }

        /**
         * Load the node from file system.
         *
         * @param root Absolute path to the root node of the SceneTree.
         */
        public void load(@NotNull Path root) {
            this.setProducer(ScoreProducer.genericLoad(root.resolve(this.getPath())));
        }

        /**
         * Whether the producer is dirty and should be dumped in proper situations.
         */
        private boolean isProducerDirty() {
            return this.producer != null && this.producer.isDirty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(path, node.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }
}