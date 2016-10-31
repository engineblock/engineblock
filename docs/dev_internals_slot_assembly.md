# This is superceded with recent changes and needs to be rewritten
## It is currently inaccurate and will be rewritten shortly

### Understanding Slot Assembly

If you are building new ActionType implementations for EngineBlock, it can help to understand the creational patterns used for slot assembly.

### What is Slot Assembly?

_Slot assembly_ in this context simply refers the logic that determines how to assemble a motor, input, and action instance together for a numbered slot within an activity.

The slot assembly logic is contained within the ActivitySlotAssembler class. That is its sole responsibility. It was isolated in this way because it is core to understanding how slots are populated within an activity.

### How does it work?

The three primary components to a slot are the Motor, the Input, and the Action. The Motor is the Runnable for the slot's thread. It requires an input and an action instance. Each of the Motor, Input, and Action instances for a slot is provided by a MotorDispenser, InputDispenser, and ActionDispenser, respectively.

An ActivityType implementation determines which components of an activity instance it will manage per-slot. This means that it could, if desired, provide no dispensers at all. (No MotorDispenser, no ActionDispenser, no InputDispenser) Such an implementation would do close to nothing, since the default action only logs the input value. When an ActivityType doesn't implement one of the three dispensers, the default implementation just uses a very basic core dispenser.

All of the dispenser methods take a slot number as a parameter. The rest of the details for how a particular instance of a motor, input or action is created are controlled by the instance of the dispenser itself.

### An example

Suppose you have a type FooActivityType implementing ActivityType. It implements none of the decorator interfaces. It looks like this:

    public class FooActivityType implements ActivityType {

      public string getName() {
        return "foo";
      }

    ...

In this case, all of the components for each slot are provided by the core runtime and assembled into a motor to be run within the ActivityExecutor's thread pool.

if you wanted to add an action to it, so that it does something besides just logging every 1000th cycle (as the CoreAction does), you would declare your implementation like this:

~~~
public class FooActivityType implements ActivityType {

  public string getName() {
    return "foo";
  }

  public getActionDispenser(ActivityDef activity) {
   // return an ActionDispenser that is tailored
   // to the ActivityDef
   return new FrisbeeActionDispenser(activity);
  }
}
~~~

The core runtime will find your activity type by way of its name "foo", and then call the above method when it needs an action dispenser for a new activity which uses type "foo", customized by the ActivityDef.

With that ActionDispenser, the ActivityExecutor will create a new action to assign to a slot as it is started, customized by the slot number.

This same pattern applies to Motors and Inputs as well.

![SlotAssembler](diagrams/slot_assembly.svg)




