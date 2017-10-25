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

package activityconfig;

import activityconfig.rawyaml.RawStatementsLoader;
import activityconfig.rawyaml.RawStmtsDocList;
import activityconfig.rawyaml.RawYamlStatementLoader;
import activityconfig.yaml.StmtsDocList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class StatementsLoader {

    private final static Logger logger = LoggerFactory.getLogger(StatementsLoader.class);

    public static StmtsDocList load(String path, String... searchPaths) {
        RawYamlStatementLoader loader = new RawYamlStatementLoader();
        RawStmtsDocList rawDocList = loader.load(path, searchPaths);
        StmtsDocList layered = new StmtsDocList(rawDocList);
        return layered;
    }

    public static StmtsDocList load(String path, Function<String, String> transformer, String... searchPaths) {
        RawYamlStatementLoader loader = new RawYamlStatementLoader(transformer);
        RawStmtsDocList rawDocList = loader.load(path, searchPaths);
        StmtsDocList layered = new StmtsDocList(rawDocList);
        return layered;
    }

    public static StmtsDocList loadWithAlternateImpl(RawStatementsLoader alternateLoader, String path,
                                                     Function<String, String> transformer, String... searchPaths) {
        try {
            RawYamlStatementLoader loader = new RawYamlStatementLoader(transformer);
            RawStmtsDocList rawDocList = loader.load(path, searchPaths);
            StmtsDocList layered = new StmtsDocList(rawDocList);
            return layered;
        } catch (Exception e) {
            try {
                RawStmtsDocList loaded = alternateLoader.load(path, searchPaths);
                logger.warn("Loaded yaml " + path + " with compatibility mode. This will be deprecated in a future release." +
                        " Please refer to http://docs.engineblock.io/user-guide/standard_yaml/ for more details.");
                return new StmtsDocList(loaded);
            } catch (Exception e2) {
                logger.error("Unable to load " + path + " with uniform or previous loader:" + e2, e2);
                throw (e2);
            }
        }
    }

}
