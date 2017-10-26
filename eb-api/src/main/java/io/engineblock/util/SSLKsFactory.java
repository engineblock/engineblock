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

import io.engineblock.activityimpl.ActivityDef;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

public class SSLKsFactory {

    private static SSLKsFactory instance = new SSLKsFactory();

    private SSLKsFactory() {
    }

    public static SSLKsFactory get() {
        return instance;
    }

    public ServerSocketFactory createSSLServerSocketFactory(ActivityDef def) {
        return getContext(def).getServerSocketFactory();
    }

    public SocketFactory createSocketFactory(ActivityDef def) {
        return getContext(def).getSocketFactory();
    }

    private SSLContext getContext(ActivityDef def) {
        String keystorePath = def.getParams().getOptionalString("keystore").orElse("JKS");
        String keystorePass = def.getParams().getOptionalString("kspass").orElse("NONE");
        String tlsVersion = def.getParams().getOptionalString("tlsversion").orElse("TLSv1.2");

        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(keystorePath), keystorePass.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keystorePass.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            SSLContext sc = SSLContext.getInstance(tlsVersion);
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}