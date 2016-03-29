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

import com.metawiring.load.generators.*;

import java.util.HashMap;
import java.util.Map;

public class GeneratorSourceImpl implements GeneratorInstanceSource {

    /**
     * For local generators, each time they are resolved, a new instance is created.
     * For shared generators, a singleton pattern is used transparently. Use local generators
     * when possible. By default, generators are managed as local. To set them as shared, use .setShared()
     */
    private Map<String,GeneratorFactory> genFacts = new HashMap<String,GeneratorFactory>() {{

        put("threadnum",new GeneratorInstanceFactory("ThreadNumGenerator"));
        put("date-epoch-hour", new GeneratorInstanceFactory("DateSequenceFieldGenerator:1000:YYYY-MM-dd-HH"));
        put("varnames", new GeneratorInstanceFactory("LineExtractGenerator:data/variable_words.txt"));
        put("datesecond",new GeneratorInstanceFactory("DateSequenceGenerator:1000"));
        put("loremipsum:100:200", new GeneratorInstanceFactory("LoremExtractGenerator:100:200"));
        put("cycle", new GeneratorInstanceFactory("CycleNumberGenerator"));

        put("namednum:1M", new GeneratorInstanceFactory("NamedNumberGenerator:1000000"));
        put("loremipsum:10:20", new GeneratorInstanceFactory("LoremExtractGenerator:10:20"));
        put("loremipsum:10:12", new GeneratorInstanceFactory("LoremExtractGenerator:10:12"));

        put("types5mod", new GeneratorInstanceFactory(LineExtractModGenerator.class, "data/types5.txt"));
        put("types5", new GeneratorInstanceFactory("LineExtractGenerator:types5.txt"));


        put("longdiv4", new GeneratorInstanceFactory("LongDivSequenceGenerator:4"));
        put("longdiv5", new GeneratorInstanceFactory("LongDivSequenceGenerator:5"));
        put("date-hour", new GeneratorInstanceFactory("DateSequenceFieldGenerator:1000:%tH"));
        put("datestamp", new GeneratorInstanceFactory("DateStampGenerator"));
        // those below are not in active use, but still here for reference
        put("datemilli",new GeneratorInstanceFactory("DateSequenceGenerator:1"));
        put("dateminute",new GeneratorInstanceFactory("DateSequenceGenerator:60000"));
        put("datehour",new GeneratorInstanceFactory("DateSequenceGenerator:3600000"));
        put("dateday",new GeneratorInstanceFactory("DateSequenceGenerator:86400000"));
        put("int", new GeneratorInstanceFactory("IntegerSequenceGenerator"));
        put("intmod10", new GeneratorInstanceFactory("IntegerModSequenceGenerator:10"));
        put("long", new GeneratorInstanceFactory("LongSequenceGenerator:"));
        put("date", new GeneratorInstanceFactory("DateSequenceGenerator:"));
        put("long100", new GeneratorInstanceFactory("LongModSequenceGenerator:100l"));
        put("int100", new GeneratorInstanceFactory("IntegerModSequenceGenerator:100"));
        put("int1000", new GeneratorInstanceFactory("IntegerModSequenceGenerator:1000"));
        put("int100000", new GeneratorInstanceFactory("IntegerModSequenceGenerator:100000"));
        put("map5", new GeneratorInstanceFactory("MapGenerator:data/variable_words.txt:5"));
        put("mapstring5", new GeneratorInstanceFactory("MapStringGenerator:data/variable_words.txt:5"));
        put("bool:true", new GeneratorInstanceFactory(BooleanGenerator.class,true));
        put("bool:false", new GeneratorInstanceFactory(BooleanGenerator.class,false));
        put("randomstring", new GeneratorInstanceFactory(RandomStringGenerator.class));
        put("longstring", new GeneratorInstanceFactory(LongStringSequenceGenerator.class));
        put("inetaddrs", new GeneratorInstanceFactory(InetAddressGenerator.class));
        put("inetaddrstring", new GeneratorInstanceFactory(InetAddressStringGenerator.class));

    }};

    public Generator getGenerator(String name) {
        GeneratorFactory generatorFactory = genFacts.get(name);
        if (generatorFactory==null) {
            generatorFactory = new GeneratorInstanceFactory(name);
        }
        Generator generator = generatorFactory.getGenerator();
        return generator;
    }
}
