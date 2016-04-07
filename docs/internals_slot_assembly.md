# Understanding Slot Assembly

If you are building new ActionType implementations for EM, it can help to understand the creational pattern used for slot assembly.

### What is Slot Assembly?

_Slot assembly_ in this context simply refers the logic that determines how to assemble a motor, input, and action instance together for a numbered slot within an activity.

The slot assembly logic is contained within the ActivitySlotAssembler class. That is its sole responsibility. It was isolated in this way because it is core to understanding how slots are populated within an activity.

### How does it work?

An ActivityType implementation determines which components of an activity instance it will manage per-slot. This means that it could, if desired, provide no dispensers at all. Such an implementation would do close to nothing, since the default action only logs the input value.

The three primary components to a slot are the Motor, the Input, and the Action. The Motor is the Runnable for the slot's thread. It requires an input and an action instance. Each of the Motor, Input, and Action instances for a slot is provided by a MotorDispenser, InputDispenser, and ActionDispenser, respectively. The MotorDispenser provides the top level service, having both an InputDispenser and an ActionDispenser. The responsibility of the MotorDispenser then is to provide instances of Runnable to fill newly created slots in a running Activity instance.

Here, a dispenser provides all the motor, input, and action instances for an activity. All of the dispenser methods take a slot number as a parameter. The rest of the details for how a particular instance of a motor, input or action is created are controlled by the instance of the dispenser itself.

In order to allow finer control of these when desired, an ActivityType implementation can provide its own Dispensers. These are each optionally implemented by an ActivityType with decorator interfaces: MotorDispenserProvider, InputDispenserProvider, and ActivityDispenserProvider. Each of these interfaces provides a method to create a dispenser, given an ActivityDef. The MotorDispenserProvider method takes additionally the instance of the InputDispenser and that of the ActionDispenser. In this way, the MotorDispenser acts as the top-level dispenser of the assembly.

### An example

Suppose you have a type FooActivityType implementing ActivityType. It implements none of the decorator interfaces. It looks like this:

    public class FooActivityType implements ActivityType {

      public string getName() {
        return "foo";
      }

    ...

In this case, all of the components for each slot are provided by the core runtime and assembled into a motor to be run within the ActivityExecutor's thread pool.

if you wanted to add an action to it, so that it does something besides just logging every 1000th cycle (as the CoreAction does), you would declare your implementation like this:

    public class FooActivityType implements ActivityType, ActionDispenserProvider {

      public string getName() {
        return "foo";
      }
    ...

To implement the ActivityDispenserProvider interface, you must provide a method:

    public getActionDispenser(ActivityDef activityDef) {
     // return an ActionDispenser that is tailored to the ActivityDef
     ...
    }

The core runtime will find your activity type by way of its name "foo", and then call the above method when it needs an action dispenser for a new activity which uses type "foo", customized by the ActivityDef.

With that ActionDispenser, the ActivityExecutor will create a new action to assign to a slot as it is started, customized by the slot number.

This same pattern applies to Motors and Inputs as well.

![SlotAssembler](diagrams/slot_assembly.svg)




