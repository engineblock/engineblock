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

package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class InetAddressStringGenerator implements FastForwardableGenerator<String> {
    private AtomicInteger atomicInt = new AtomicInteger();

    @Override
    public String get() {
        int image = atomicInt.incrementAndGet();
        ByteBuffer bytes = ByteBuffer.allocate(4);
        bytes.clear();
        bytes.putInt(image);
        bytes.flip();
        try {
            return Inet4Address.getByAddress(bytes.array()).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fastForward(long fastForwardTo) {
        atomicInt.set((int)fastForwardTo % Integer.MAX_VALUE);
    }
}
