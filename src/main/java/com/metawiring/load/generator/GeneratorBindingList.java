/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.generator;

//

import com.datastax.driver.core.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Maps a named generator binding point to a named generator.
 * In this case, both the named binding point and the generator which
 * is bound to it are managed by the client object. This simply provides
 * a clean and reusable way to manage such generatorBindings.
 */
public class GeneratorBindingList {
    private final static Logger logger = LoggerFactory.getLogger(GeneratorBindingList.class);

    private boolean trace=false;
    private final GeneratorInstanceSource instanceSource;
    private LinkedHashMap<String,Generator> bindings = new LinkedHashMap<String,Generator>();
    private List<Generator> generatorList = new ArrayList<Generator>();
    private Object all;

    public GeneratorBindingList(GeneratorInstanceSource instanceSource) {
        this.instanceSource = instanceSource;
    }

    /**
     * Set a binding
     * @param name the name of the binding point, according to the client object
     * @param generator the generator which is used by the binding point
     */
    public void set(String name, Generator generator) {
        bindings.put(name, generator);
        generatorList.add(generator);
    }

    /**
     * Get a value from each generator in the bindings list
     * @return An array of objects, the values generated from each generator in the list
     */
    public Object[] getAll() {
        Object[] values = new Object[generatorList.size()];
        int offset=0;
        for (Generator generator: generatorList) {
            values[offset++]=generator.get();
        }
        if (trace ) {
            logger.info(Arrays.toString(values));
        }
        return values;
    }

    /**
     * For the prepared statement, add generatorBindings for the parameters, using each pair of names.
     * The first name in each pair is the local name of the object using the generatorBindings.
     * The second name in each pair is the name of the generator to lookup and assign.
     * @param preparedStmt The statement which the binding is for
     * @param names pairs of names in var,generator, ... form
     */
    public void bindGenerator(PreparedStatement preparedStmt, String... names) {

        for (int idx=0;idx<names.length;idx++)
        {
            String varname=names[idx++];
            String genname=names[idx];
            bindGenerator(preparedStmt, varname, genname, 0);
        }

    }

    /**
     * Like {@link #bindGenerator(PreparedStatement, String...)} except that it can only bind one
     * parameter, and that it can accept an offset for addressing generated data from a later
     * continuation point instead of 0
     * @param preparedStmt The statement which the binding is for
     * @param varname - the name of the field which is 'referencing' this binding
     * @param genname - the generator name
     * @param startOffset - the offset which the generator should be initialized with
     */
    public void bindGenerator(PreparedStatement preparedStmt, String varname, String genname, long startOffset) {

        Generator generator = instanceSource.getGenerator(genname);

        if (generator == null ) {
            throw new RuntimeException("No generator found for varname:" + varname +", generatorName:" + genname);
        }

        set(varname, generator);

        if (generator instanceof FastForwardableGenerator) {
            ((FastForwardableGenerator) generator).fastForward(startOffset);
            logger.debug("generator " + genname + " fast-forwarded to " + startOffset);
        }
        else {
            logger.debug("generator " + genname + " NOT fast-forwarded");
        }
    }

}
