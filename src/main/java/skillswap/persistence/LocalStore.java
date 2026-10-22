package skillswap.persistence;

import skillswap.model.Session;
import skillswap.model.Student;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small local binary store with write-to-temp plus atomic replace. */
public final class LocalStore {
    private final Path file;

    public LocalStore(Path file) { this.file = file.toAbsolutePath(); }
    public Path getFile() { return file; }

    public Snapshot load() throws IOException {
        if (!Files.exists(file)) return new Snapshot();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file.toFile()))) {
            Object value = in.readObject();
            if (!(value instanceof Snapshot snapshot)) throw new IOException("The local Skill Swap data file has an unsupported format.");
            snapshot.validate();
            return snapshot;
        } catch (ClassNotFoundException | RuntimeException ex) {
            throw new IOException("The local Skill Swap data file could not be read. Your saved file was left untouched.", ex);
        }
    }

    public void save(Snapshot snapshot) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (FileOutputStream stream = new FileOutputStream(temp.toFile());
             ObjectOutputStream out = new ObjectOutputStream(stream)) {
            out.writeObject(snapshot);
            out.flush();
            stream.getFD().sync();
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static final class Snapshot implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, Student> students;
        private final List<Session> sessions;

        public Snapshot() { this(new LinkedHashMap<>(), new ArrayList<>()); }
        public Snapshot(Map<String, Student> students, List<Session> sessions) {
            this.students = new LinkedHashMap<>(students);
            this.sessions = new ArrayList<>(sessions);
        }
        public Map<String, Student> getStudents() { return new LinkedHashMap<>(students); }
        public List<Session> getSessions() { return new ArrayList<>(sessions); }
        private void validate() throws IOException {
            if (students == null || sessions == null) throw new IOException("The local Skill Swap data file is incomplete.");
            for (Map.Entry<String, Student> row : students.entrySet()) {
                if (row.getKey() == null || row.getValue() == null || !row.getKey().equals(row.getValue().getId())) {
                    throw new IOException("The local Skill Swap data file contains an invalid student record.");
                }
            }
            for (Session session : sessions) if (session == null) throw new IOException("The local Skill Swap data file contains an invalid session record.");
        }
    }
}
