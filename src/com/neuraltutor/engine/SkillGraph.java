package com.neuraltutor.engine;

import com.neuraltutor.model.LearnerProfile;
import java.util.*;

public final class SkillGraph {
    public record Skill(String name, String description, List<String> prerequisites) {}

    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public SkillGraph() {
        add("Control Flow", "Build reliable reasoning with conditions, loops and switch.",
                List.of());
        add("OOP Basics", "Classes, objects, encapsulation, constructors and polymorphic thinking.",
                List.of("Control Flow"));
        add("Inheritance", "Model parent-child relationships and reuse behavior safely.",
                List.of("OOP Basics"));
        add("Abstract & Interfaces", "Define contracts and abstraction boundaries.",
                List.of("OOP Basics", "Inheritance"));
        add("Collections", "Choose and operate on List, Set, Map and Queue abstractions.",
                List.of("OOP Basics"));
        add("String Handling", "Reason precisely about immutable text and String APIs.",
                List.of("OOP Basics"));
        add("Exceptions", "Design predictable failure handling and recovery.",
                List.of("OOP Basics", "Control Flow"));
        add("File I/O & Streams", "Move data between files and Java stream abstractions.",
                List.of("Exceptions", "Collections"));
        add("Multithreading", "Understand threads, synchronization and concurrent execution.",
                List.of("OOP Basics", "Exceptions"));
        add("JDBC & Databases", "Persist state using connections, prepared statements and transactions.",
                List.of("Collections", "Exceptions"));
    }

    private void add(String name, String description, List<String> prereq) {
        skills.put(name, new Skill(name, description, prereq));
    }

    public Collection<Skill> all() { return Collections.unmodifiableCollection(skills.values()); }

    public Skill skill(String name) { return skills.get(name); }

    public String recommend(LearnerProfile learner) {
        return skills.values().stream()
                .filter(s -> prerequisitesMet(s, learner))
                .min(Comparator.comparingDouble(s -> learner.mastery(s.name())))
                .map(Skill::name)
                .orElse("Control Flow");
    }

    public List<Skill> nextPath(LearnerProfile learner, int limit) {
        return skills.values().stream()
                .filter(s -> prerequisitesMet(s, learner))
                .sorted(Comparator.comparingDouble(s -> learner.mastery(s.name())))
                .limit(limit)
                .toList();
    }

    private boolean prerequisitesMet(Skill skill, LearnerProfile learner) {
        return skill.prerequisites().stream().allMatch(p -> learner.mastery(p) >= 0.48);
    }
}