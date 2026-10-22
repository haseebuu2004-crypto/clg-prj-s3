package skillswap.model;

import java.io.Serializable;
import java.util.Locale;

/** Shared identity and contact information for people using Skill Swap. */
public abstract class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private String email;

    protected Person(String id, String name, String email) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("An ID is required.");
        this.id = id;
        setName(name);
        setEmail(email);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Enter a name.");
        this.name = name.trim();
    }

    public void setEmail(String email) {
        if (email == null || !email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        this.email = email.trim().toLowerCase(Locale.ROOT);
    }
}
