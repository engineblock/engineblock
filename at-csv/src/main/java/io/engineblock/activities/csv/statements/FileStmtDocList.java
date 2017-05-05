package io.engineblock.activities.csv.statements;

import io.engineblock.util.TagFilter;

import java.util.*;
import java.util.stream.Collectors;

public class FileStmtDocList {

    private List<FileStmtDoc> fileStmtDocList = new ArrayList<>();

    public FileStmtDocList(List<FileStmtDoc> fileStmtDocList) {
        this.fileStmtDocList = fileStmtDocList;
    }

    public Map<String,String> getFilteringDetails(String spec) {
        Map<String,String> details = new LinkedHashMap<>();
        TagFilter ts = new TagFilter(spec);
        for (FileStmtDoc gsb : this.fileStmtDocList) {
            TagFilter.Result result = ts.matchesTaggedResult(gsb);
            details.put(gsb.getName(), result.getLog());
        }
        return details;
    }

    public List<FileStmtDoc> getMatching(String tagSpec) {
        List<FileStmtDoc> matchingBlocks = new ArrayList<>();
        TagFilter ts = new TagFilter(tagSpec);
        return this.fileStmtDocList.stream().filter(ts::matchesTagged).collect(Collectors.toList());
    }

    public List<FileStmtDoc> getAll() {
        return Collections.unmodifiableList(this.fileStmtDocList);
    }
}
