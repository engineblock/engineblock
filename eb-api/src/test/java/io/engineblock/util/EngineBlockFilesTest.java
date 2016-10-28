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

import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class EngineBlockFilesTest {

    @Test
    public void testNestedClasspathLoading() {
        Optional<InputStream> optionalStreamOrFile = EngineBlockFiles.findOptionalStreamOrFile("nested/testfile", "txt", "activities");
        assertThat(optionalStreamOrFile).isPresent();
    }

    @Test
    public void testUrlResourceSearchSanity() {
        String url="https://google.com/robots";
        Optional<InputStream> inputStream = EngineBlockFiles.findOptionalStreamOrFile(url,"txt","activity");
        assertThat(inputStream).isPresent();
    }

    @Test
    public void testUrlResourceLoading() {
        String url="https://google.com/";
        Optional<InputStream> inputStream = EngineBlockFiles.getInputStream(url);
        assertThat(inputStream).isPresent();
    }
}