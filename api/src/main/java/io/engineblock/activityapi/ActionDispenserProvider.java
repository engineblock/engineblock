package io.engineblock.activityapi;

public interface ActionDispenserProvider {
    /**
     * This method will be called <em>once</em> per action instance.
     * @param activityDef
     * @return an instance of ActionDispenser
     */
    ActionDispenser getActionDispenser(ActivityDef activityDef);
}
