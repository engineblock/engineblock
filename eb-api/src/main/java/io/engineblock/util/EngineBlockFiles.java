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

package io.engineblock.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class EngineBlockFiles {

    public static InputStream findRequiredStreamOrFile(String basename, String extension, String... searchPaths) {
        Optional<InputStream> optionalStreamOrFile = findOptionalStreamOrFile(basename, extension, searchPaths);
        return optionalStreamOrFile.orElseThrow(() -> new RuntimeException(
                "Unable to find " + basename + " with extension " + extension + " in file system or in classpath, with"
                + "searchpaths: " + Arrays.asList(searchPaths).stream().collect(Collectors.joining(","))
        ));
    }

    public static Optional<InputStream> findOptionalStreamOrFile(String basename, String extension, String... searchPaths) {

        String filename = (basename.endsWith("." + extension)) ? basename : basename + "." + extension;

        ArrayList<String> paths = new ArrayList<String>() {{
            add(filename);
            addAll(Arrays.asList(searchPaths)
                    .stream().map(s -> s + File.separator + filename)
                    .collect(Collectors.toCollection(ArrayList::new)));
        }};

        InputStream stream = null;
        for (String path : paths) {
            try {
                stream = new FileInputStream(path);
                break;
            } catch (FileNotFoundException ignored) {
            }
            ClassLoader classLoader = EngineBlockFiles.class.getClassLoader();
            stream = classLoader.getResourceAsStream(path);
            if (stream != null) {
                break;
            }
        }

        return Optional.ofNullable(stream);

    }
}
