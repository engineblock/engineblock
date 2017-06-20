package io.engineblock.activities.csv.statements;

import io.engineblock.util.TagFilter;

import java.util.*;
import java.util.stream.Collectors;

public class CSVStmtDocList {

    private List<CSVStmtDoc> csvStmtDocList = new ArrayList<>();

    public CSVStmtDocList(List<CSVStmtDoc> csvStmtDocList) {
        this.csvStmtDocList = csvStmtDocList;
    }

    public Map<String,String> getFilteringDetails(String spec) {
        Map<String,String> details = new LinkedHashMap<>();
        TagFilter ts = new TagFilter(spec);
        for (CSVStmtDoc gsb : this.csvStmtDocList) {
            TagFilter.Result result = ts.matchesTaggedResult(gsb);
            details.put(gsb.getName(), result.getLog());
        }
        return details;
    }

    public List<CSVStmtDoc> getMatching(String tagSpec) {
        List<CSVStmtDoc> matchingBlocks = new ArrayList<>();
        TagFilter ts = new TagFilter(tagSpec);
        return this.csvStmtDocList.stream().filter(ts::matchesTagged).collect(Collectors.toList());
    }

    public List<CSVStmtDoc> getAll() {
        return Collections.unmodifiableList(this.csvStmtDocList);
    }
}
