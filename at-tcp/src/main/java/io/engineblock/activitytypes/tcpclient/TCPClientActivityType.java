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

package io.engineblock.activitytypes.tcpclient;

import io.engineblock.activitytypes.stdout.StdoutAction;
import io.engineblock.activitytypes.stdout.StdoutActivity;
import io.engineblock.activityapi.core.Action;
import io.engineblock.activityapi.core.ActionDispenser;
import io.engineblock.activityapi.core.ActivityType;
import io.engineblock.activityimpl.ActivityDef;
import io.virtdata.annotations.Service;

@Service(ActivityType.class)
public class TCPClientActivityType implements ActivityType<TCPClientActivity> {

    @Override
    public String getName() {
        return "tcpclient";
    }

    @Override
    public TCPClientActivity getActivity(ActivityDef activityDef) {
        return new TCPClientActivity(activityDef);
    }

    @Override
    public ActionDispenser getActionDispenser(TCPClientActivity activity) {
        return new Dispenser(activity);
    }

    private static class Dispenser implements ActionDispenser {
        private StdoutActivity activity;

        private Dispenser(StdoutActivity activity) {
            this.activity = activity;
        }

        @Override
        public Action getAction(int slot) {
            return new StdoutAction(slot,this.activity);
        }
    }
}
