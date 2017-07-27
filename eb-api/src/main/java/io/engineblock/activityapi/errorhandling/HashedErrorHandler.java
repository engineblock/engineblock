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

import java.util.HashMap;
import java.util.Map;

/**
 * Allow error handlers to be registered for any exception type, with an explicit handler
 * that will be called if no specific other handler is matched.
 * Because of type erasure, the constructor is required to be called with a class instance
 * matching generic parameter T for initialization.
 *
 * This error handler will automatically cascade up the error type hierarchy of the reported
 * error to find any matching handlers. If none are found between the reported error type
 * and the upper type bound, inclusive, then the default error handler is called, which
 * simply rethrows an error indicating that no more suitable error handler was provided.
 *
 * @param <T> The subtype bound of exception to allow exception handlers for.
 * @param <R> The result type that will be produced by these error handlers.
 */
public class HashedErrorHandler<T extends Exception,R> implements CycleErrorHandler<T,R> {
    private Map<Class<? extends T>,CycleErrorHandler<T,R>> handlers = new HashMap<>();
    private final CycleErrorHandler<T,R> DEFAULT_defaultHandler = (cycle, error, errMsg) -> {
        throw new RuntimeException("no handler defined for type " + error.getClass() + " in cycle " + cycle + ", " + errMsg);
    };
    private CycleErrorHandler<T,R> defaultHandler = DEFAULT_defaultHandler;
    private final Class<?> upperBound;


    HashedErrorHandler(Class<T> upperBound, CycleErrorHandler<T,R> defaultErrorHandler) {
        setDefaultHandler(defaultErrorHandler);
        this.upperBound = upperBound;
    }

    HashedErrorHandler(Class<T> upperBound) {
        this.upperBound = upperBound;
    }
    HashedErrorHandler() {
        this.upperBound = Exception.class;
    }


    public void addHandler(Class<? extends T> errorClass, CycleErrorHandler<T,R> errorHandler) {
        handlers.put(errorClass, errorHandler);
    }

    public void setDefaultHandler(CycleErrorHandler<T,R> errorHandler) {
        defaultHandler = errorHandler;
    }

    @Override
    public R handleError(long cycle, T error, String errMsg) {
        Class<?> errorClass = error.getClass();
        CycleErrorHandler<T,R> errorHandler = null;
        while (errorHandler==null) {
            errorHandler = handlers.get(errorClass);
            errorClass = errorClass.getSuperclass();
            if (!upperBound.isAssignableFrom(errorClass)) {
                break;
            }
        }
        errorHandler = (errorHandler==null) ? defaultHandler : errorHandler;
        return errorHandler.handleError(cycle, error, errMsg);
    }
}
