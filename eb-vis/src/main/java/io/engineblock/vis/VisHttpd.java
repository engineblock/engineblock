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

package io.engineblock.vis;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class VisHttpd implements Runnable {

    public static void main(String[] args) {
        VisHttpd server = new VisHttpd(8081, "localhost");
        Thread thread = new Thread(server);
        thread.setDaemon(false);
        thread.start();
    }


    Server server;
    private int port;
    private String listenAddr;

    public VisHttpd(int port, String listenAddr) {
        this.port = port;
        this.listenAddr = listenAddr;
    }

    @Override
    public void run() {
        try {
            server = configure();
            server.start();
            server.join();
        } catch (Exception e) {
            handleException(e);
        }
    }

    private Server configure() {
        Server server = null;
        try {
            InetAddress byName = InetAddress.getByName(this.listenAddr);
            InetSocketAddress addr = new InetSocketAddress(byName, this.port);
            server = new Server(port);
            ResourceHandler resourceHandler = getResourceHandler();
            server.setHandler(resourceHandler);
        } catch (Exception e) {
            handleException(e);
        }

        return server;
    }

    private ResourceHandler getResourceHandler() {
        try {
            ResourceHandler resourceHandler = new ResourceHandler();
            Resource viscp = ResourceCollection.newClassPathResource("viscontent", true, true);
            ResourceCollection allresources = new ResourceCollection(
                    viscp
            );
            resourceHandler.setWelcomeFiles(new String[]{"index.html","index.html"});
            resourceHandler.setDirAllowed(true);
            resourceHandler.setResourceBase("/");
            resourceHandler.setBaseResource(allresources);


            return resourceHandler;
        } catch (Exception e) {
            handleException(e);
            throw (new RuntimeException(e));
        }
    }

    private void handleException(Exception e) {
        throw new RuntimeException(e);
    }

}
