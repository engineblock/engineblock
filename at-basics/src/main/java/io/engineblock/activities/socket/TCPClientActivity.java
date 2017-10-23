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

package io.engineblock.activities.socket;

import io.engineblock.activities.stdout.StdoutActivity;
import io.engineblock.activityimpl.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPClientActivity extends StdoutActivity {
    private final static Logger logger = LoggerFactory.getLogger(TCPClientActivity.class);
    private Boolean sslEnabled;
    private SocketFactory socketFactory = SocketFactory.getDefault();


    public TCPClientActivity(ActivityDef activityDef) {
        super(activityDef);
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        super.onActivityDefUpdate(activityDef);
        this.sslEnabled = activityDef.getParams().getOptionalBoolean("ssl").orElse(false);
    }

    @Override
    protected PrintWriter createPrintWriter() {
        String host = getActivityDef().getParams().getOptionalString("host").orElse("localhost");
        int port = getActivityDef().getParams().getOptionalInteger("port").orElse(12345);
        try {
            if (sslEnabled) {
                socketFactory = SSLSocketFactory.getDefault();
            }
            Socket socket = socketFactory.createSocket(host,port);
            logger.info("connected to " + socket.toString());
            return new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("Error opening socket:" + e, e);
        }
    }


}
