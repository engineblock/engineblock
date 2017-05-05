package io.engineblock.activities.json.statements;

import io.virtdata.core.AllDataMapperLibraries;
import io.virtdata.core.BindingsTemplate;
import java.util.Map;

public class ReadyFileStatementTemplate {

    private final String stmtTemplate;
    private final BindingsTemplate bindingsTemplate;
    private String name;

    public ReadyFileStatementTemplate(String name, String stmtTemplate, Map<String, String> bindingSpecs) {
        this.name = name;
        this.stmtTemplate = stmtTemplate;
        this.bindingsTemplate = new BindingsTemplate(AllDataMapperLibraries.get(), bindingSpecs);
    }

    public ReadyFileStatement resolve() {
        return new ReadyFileMapStatement(stmtTemplate,bindingsTemplate.resolveBindings());
    }
}
