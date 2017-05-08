package io.engineblock.activities.csv.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReadyCSVStatementsTemplate {

    private List<ReadyCSVStatementTemplate> templateList = new ArrayList<>();

    public void addTemplate(String name, String stmtTemplate, Map<String,String> bindingSpecs) {
        ReadyCSVStatementTemplate rgst = new ReadyCSVStatementTemplate(name, stmtTemplate, bindingSpecs);
    }

    public void addTemplate(ReadyCSVStatementTemplate template) {
        this.templateList.add(template);
    }

    public List<ReadyCSVStatement> resolve() {
        return templateList.stream().map(ReadyCSVStatementTemplate::resolve)
                .collect(Collectors.toList());
    }

    public int size() {
        return templateList.size();
    }
}
