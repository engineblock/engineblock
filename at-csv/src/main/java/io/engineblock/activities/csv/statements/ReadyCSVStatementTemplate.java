package io.engineblock.activities.csv.statements;


import io.virtdata.core.AllDataMapperLibraries;
import io.virtdata.core.BindingsTemplate;
import java.util.Map;


public class ReadyCSVStatementTemplate {

    private final String stmtTemplate;
    private final BindingsTemplate bindingsTemplate;
    private String name;

    public ReadyCSVStatementTemplate(String name, String stmtTemplate, Map<String, String> bindingSpecs) {
        this.name = name;
        this.stmtTemplate = stmtTemplate;
        this.bindingsTemplate = new BindingsTemplate(AllDataMapperLibraries.get(), bindingSpecs);
    }

    public ReadyCSVStatement resolve() {
        return new ReadyCSVMapStatement(stmtTemplate,bindingsTemplate.resolveBindings());
    }
}
