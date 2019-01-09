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

var leader = {
    type: 'diag',
    alias: 'leader',
    targetrate: '10000',
    async: '1000'
};

var follower = {
    type: 'diag',
    alias: 'follower',
    linkinput: 'leader',
    async: '1000'
};

scenario.start(leader);
print("started leader");
scenario.start(follower);
print("started follower");

scenario.waitMillis(500);

scenario.stop(leader);
print("stopped leader");
scenario.stop(follower);
print("stopped follower");

