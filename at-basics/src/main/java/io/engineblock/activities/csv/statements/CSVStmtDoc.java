package io.engineblock.activities.csv.statements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CSVStmtDoc extends CSVStmtBlock {

    private final static Logger logger = LoggerFactory.getLogger(CSVStmtDoc.class);
    private List<CSVStmtBlock> blocks = new ArrayList<>();

    public List<CSVStmtBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<CSVStmtBlock> blocks) {
        this.blocks.clear();
        this.blocks.addAll(blocks);
    }

    /**
     * Returns a full view of all blocks, with the global 'this' block inserted at the front of the list,
     * but only if it includes statements.
     * @return a new List of CSVStmtSections
     */
    public List<CSVStmtBlock> getAllBlocks() {
        List<CSVStmtBlock> allSections = new ArrayList<CSVStmtBlock>();
        if (getStatements().size()>0) {
            allSections.add(this);
        }
        allSections.addAll(blocks);
        return allSections;
    }
}

