package io.engineblock.activityapi;

public interface ActionDispenserProvider {
    /**
     * This method will be called <em>once</em> per action instance.
     * @param activityDef The activity definition instance that will parameterize the returned ActionDispenser instance.
     * @return an instance of ActionDispenser
     */
    ActionDispenser getActionDispenser(ActivityDef activityDef);
}
