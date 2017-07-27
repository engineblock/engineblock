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

package io.engineblock.activityapi.errorhandling;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class HashedErrorHandlerTest {

    HashedErrorHandler<Exception, Boolean> handler;

    @BeforeTest
    public void beforeTest() {
        handler = new HashedErrorHandler<>(Exception.class);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*actually.*")
    public void testDefaultHandler() {
        handler.handleError(1L, new InvalidParameterException("this is an invalid exception, actually"));
    }

    @Test
    public void testSuperclassErrorHandler() {
        handler.addHandler(
                IndexOutOfBoundsException.class,
                CycleErrorHandlers.log(Exception.class, true));
        try {
            String[] a = new String[10];
            System.out.println(a[20]);
        } catch (Exception e) {
            handler.handleError(2L, e);
        }
    }

    @Test
    public void testDefaultOverride() {
        List<CycleErrorHandler.Triple> list = new ArrayList<>();
        RuntimeException myError = new RuntimeException("none here");
        handler.setDefaultHandler(CycleErrorHandlers.store(Exception.class, list, true));
        handler.handleError(3L, myError, "an error");
        assertThat(list.get(0).cycle).isEqualTo(3L);
        assertThat(list.get(0).error).isEqualTo(myError);
        assertThat(list.get(0).msg).isEqualTo("an error");
    }

    public void testExactClassHandler() {
        List<CycleErrorHandler.Triple> list = new ArrayList<>();
        handler.addHandler(
                StringIndexOutOfBoundsException.class,

                CycleErrorHandlers.store(Exception.class, list, true));
        handler.addHandler(
                IndexOutOfBoundsException.class,
                CycleErrorHandlers.store(Exception.class, list, false));

        try {
            String[] a = new String[10];
            System.out.println(a[20]);

        } catch (Exception e) {
            handler.handleError(2L, e);
        }
        assertThat(list.get(0).result).isOfAnyClassIn(Boolean.class);
        boolean result = (boolean) list.get(0).result;
        assertThat(result).isFalse();
    }

}