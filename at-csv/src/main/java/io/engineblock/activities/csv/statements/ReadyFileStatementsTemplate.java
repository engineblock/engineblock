package io.engineblock.activities.csv.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReadyFileStatementsTemplate {

    private List<ReadyFileStatementTemplate> templateList = new ArrayList<>();

    public void addTemplate(String name, String stmtTemplate, Map<String,String> bindingSpecs) {
        ReadyFileStatementTemplate rgst = new ReadyFileStatementTemplate(name, stmtTemplate, bindingSpecs);
    }

    public void addTemplate(ReadyFileStatementTemplate template) {
        this.templateList.add(template);
    }

    public List<ReadyFileStatement> resolve() {
        return templateList.stream().map(ReadyFileStatementTemplate::resolve)
                .collect(Collectors.toList());
    }

    public int size() {
        return templateList.size();
    }
}
