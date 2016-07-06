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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class TagFilterTest {

    @Test
    public void testTagFilterNameOnly() {
        TagFilter tf = new TagFilter("name");
        assertThat(tf.getMap().size()).isEqualTo(1);
        assertThat(tf.getMap().containsKey("name")).isTrue();
        assertThat(tf.getMap().get("name")).isNull();
    }

    @Test
    public void testEmptyTagFilterDoesMatch() {
        Map<String,String> itemtags = new HashMap<String,String>() {{
            put("a","tag");
        }};
        TagFilter tf = new TagFilter("");
        assertThat(tf.matches(itemtags)).isTrue();
    }

    @Test
    public void testSomeFilterTagsNoItemTagsDoesNotMatch() {
        Map<String,String> itemtags = new HashMap<String,String>() {{
        }};
        TagFilter tf = new TagFilter("tag=foo");
        assertThat(tf.matches(itemtags)).isFalse();

    }

    @Test
    public void testEmptyTagFilterValueDoesMatch() {
        Map<String,String> itemtags = new HashMap<String,String>() {{
            put("one","two");
        }};
        TagFilter tf = new TagFilter("");
        assertThat(tf.matches(itemtags)).isTrue();

    }

    @Test
    public void testMatchingTagKeyValueDoesMatch() {
        Map<String,String> itemtags = new HashMap<String,String>() {{
            put("one","two");
        }};
        TagFilter tf = new TagFilter("one");
        assertThat(tf.matches(itemtags)).isTrue();

        Map<String,String> itemtags2 = new HashMap<String,String>() {{
            put("one",null);
        }};
        assertThat(tf.matches(itemtags2)).isTrue();
    }


    @Test
    public void testMatchingKeyMismatchingValueDoesNotMatch() {
        Map<String,String> itemtags = new HashMap<String,String>() {{
            put("one","four");
        }};
        TagFilter tf = new TagFilter("one:two");
        assertThat(tf.matches(itemtags)).isFalse();
    }

    @Test
    public void testMatchingKeyAndValueDoesMatch() {
        Map<String,String> itemtags = new HashMap<String,String>() {{
            put("one","four");
        }};
        TagFilter tf = new TagFilter("one:four");
        assertThat(tf.matches(itemtags)).isTrue();
    }

    @Test
    public void testMatchingKeyAndValueRegexDoesMatch() {
        Map<String,String> itemtags = new HashMap<String,String>() {{
            put("one","four-five-six");
        }};
        TagFilter tfLeft = new TagFilter("one:'four-.*'");
        assertThat(tfLeft.matches(itemtags)).isTrue();
        TagFilter tfInner = new TagFilter("one:'.*-five-.*'");
        assertThat(tfInner.matches(itemtags)).isTrue();
        TagFilter tfRight = new TagFilter("one:'.*-six'");
        assertThat(tfRight.matches(itemtags)).isTrue();
    }

    @Test
    public void testRawSubstringDoesNotMatchRegex() {
        Map<String,String> itemtags = new HashMap<String,String>() {{
            put("one","four-five-six");
        }};
        TagFilter tf = new TagFilter("one:'five'");
        assertThat(tf.matches(itemtags)).isFalse();
    }

}