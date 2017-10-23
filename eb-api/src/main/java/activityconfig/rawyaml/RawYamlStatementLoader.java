/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package activityconfig.rawyaml;

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

public class RawYamlStatementLoader {

    private final static Logger logger = LoggerFactory.getLogger(RawYamlStatementLoader.class);
    List<Function<String, String>> stringTransformers = new ArrayList<>();

    public RawYamlStatementLoader() {
    }

    public RawYamlStatementLoader(Function<String, String> stringTransformer) {
        this.transformWith(stringTransformer);
    }

    private RawYamlStatementLoader transformWith(Function<String, String>... transformers) {
        stringTransformers.addAll(Arrays.asList(transformers));
        return this;
    }


    public RawStmtsDocList load(String fromPath, String... searchPaths) {
        InputStream stream = EngineBlockFiles.findRequiredStreamOrFile(fromPath, "yaml", searchPaths);
        String data;

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
            List<RawStmtsDoc> stmtListList = new ArrayList<>();
            for (Object object : objects) {
                RawStmtsDoc tgsd = (RawStmtsDoc) object;
                stmtListList.add(tgsd);
            }
            return new RawStmtsDocList(stmtListList);
        } catch (Exception e) {
            logger.error("Error loading yaml from " + fromPath, e);
            throw e;
        }
    }

    private Yaml getCustomYaml() {
        Constructor constructor = new Constructor(RawStmtsDoc.class);
        TypeDescription tds = new TypeDescription(RawStmtsDoc.class);
        tds.putListPropertyType("blocks", RawStmtsBlock.class);
        constructor.addTypeDescription(tds);
        return new Yaml(constructor);
    }

}
