package io.engineblock.activities.json.statements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FileStmtDoc extends FileStmtBlock {

    private final static Logger logger = LoggerFactory.getLogger(FileStmtDoc.class);
    private List<FileStmtBlock> blocks = new ArrayList<>();

    public List<FileStmtBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<FileStmtBlock> blocks) {
        this.blocks.clear();
        this.blocks.addAll(blocks);
    }

    /**
     * Returns a full view of all blocks, with the global 'this' block inserted at the front of the list,
     * but only if it includes statements.
     * @return a new List of FileStmtSections
     */
    public List<FileStmtBlock> getAllBlocks() {
        List<FileStmtBlock> allSections = new ArrayList<FileStmtBlock>();
        if (getStatements().size()>0) {
            allSections.add(this);
        }
        allSections.addAll(blocks);
        return allSections;
    }
}

