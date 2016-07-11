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

activitydef = {
    "alias" : "threadspeeds",
    "type" : "diag",
    "cycles" : "1..4000000000000",
    "threads" : "1",
    "interval" : "10000"
};
scenario.start(activitydef);


var speeds = [];

function aTestCycle(threads, index) {
    print("speeds:" + speeds.toString());
    activities.threadspeeds.threads = threads; // dynamically adjust the active threads for this activity
    scenario.waitMillis(60000);                // wait for 1 minute to get a clean 1 minute average speed
    speeds[threads]= metrics.get("activity.threadspeeds.timer").getOneMinuteRate(); // record 1 minute avg speed
}

var min=1;
var max=256;
var mid=Math.floor((min+max)/2)|0;

[min,mid,max].forEach(aTestCycle);

// speeds[min]=3669236.056434803;
// speeds[mid]=5376173.001237295;
// speeds[max]=5976483.90095274;

while (min<mid && mid<max && scenario.isRunningActivity(activitydef)) {
    print("speeds:" + speeds.toString());
    if (speeds[min]<speeds[max]) {
        min=mid;
        mid=parseInt(Math.floor((mid+max) / 2),10)|0;
    } else if (speeds[min]>speeds[max]) {
        max=mid;
        mid=parseInt(Math.floor((min+mid) / 2),10)|0;
    }
    aTestCycle(mid,mid);
}
scenario.stop(threadspeeds);


