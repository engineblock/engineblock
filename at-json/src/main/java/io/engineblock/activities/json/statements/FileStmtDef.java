package io.engineblock.activities.json.statements;

import io.engineblock.util.Tagged;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a base name, a set of statements, some bindings, and tags for a File statement.
 * This version is meant to provide YAML-friendly field naming.
 */
public class FileStmtDef implements Tagged {

    private Map<String,String> tags = new LinkedHashMap<>();
    private String name;
    private List<String> statements = new ArrayList<>();
    private Map<String,String> bindings = new LinkedHashMap<>();

    public FileStmtDef() {};

    public List<String> getStatements() {
        return statements;
    }

    public void setStatements(List<String> statements) {
        this.statements.clear();
        this.statements.addAll(statements);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, String> bindings) {
        this.bindings = bindings;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags.clear();
        this.tags.putAll(tags);
    }
}
