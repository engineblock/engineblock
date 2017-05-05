package io.engineblock.activities.csv.statements;

import io.engineblock.activityimpl.ActivityInitializationError;
import io.engineblock.util.EngineBlockFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
public class YamlFileStatementLoader {

    private final static Logger logger = LoggerFactory.getLogger(YamlFileStatementLoader.class);
    List<Function<String, String>> imageTransformers = new ArrayList<>();
    List<Function<String, String>> stringTransformers = new ArrayList<>();

    public YamlFileStatementLoader() {
    }

    public YamlFileStatementLoader(Function<String, String>... stringTransformers) {
        this.stringTransformers.addAll(Arrays.asList(stringTransformers));
    }

    public FileStmtDocList load(String fromPath, String... searchPaths) {
        InputStream stream = EngineBlockFiles.findRequiredStreamOrFile(fromPath, "yaml", searchPaths);
        String data = "";

        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream))) {
            data = buffer.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Error while reading yaml stream data:" + e);
        }

        for (Function<String, String> xform : stringTransformers) {
            try {
                logger.debug("Applying string transformer to yaml data:" + xform);
                data = xform.apply(data);
            } catch (Exception e) {
                RuntimeException t = new ActivityInitializationError("Error applying string transform to input", e);
                logger.error(t.getMessage(), t);
                throw t;
            }
        }

        Yaml yaml = getCustomYaml();

        try {
            Iterable<Object> objects = yaml.loadAll(data);
            List<FileStmtDoc> stmtListList = new ArrayList<>();
            for (Object object : objects) {
                FileStmtDoc tgsd = (FileStmtDoc) object;
                stmtListList.add(tgsd);
            }
            return new FileStmtDocList(stmtListList);
        } catch (Exception e) {
            logger.error("Error loading yaml from " + fromPath, e);
            throw e;
        }

    }

    private Yaml getCustomYaml() {
        Constructor constructor = new Constructor(FileStmtDoc.class);
        TypeDescription tds = new TypeDescription(FileStmtDoc.class);
        tds.putListPropertyType("blocks", FileStmtBlock.class);
        constructor.addTypeDescription(tds);
        return new Yaml(constructor);
    }

}
