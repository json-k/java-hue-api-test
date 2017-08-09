![Hue](https://upload.wikimedia.org/wikipedia/en/a/a1/Philips_hue_logo.png)

I have the good fortune to have some Philips Hue lighting (and other automation) in my house - it's a long story about an old cat not being able to see in the basement to pee, but it ends with me joining the new world of home automation.

The excellent HUE bridge has an API so of course I was eager to learn how it works. Philips suggest (probably quite rightly) to use their provided SDK and have some very good information about the specifics of working with their product. Sign up free and get the information from their [excellent developer's site](https://www.developers.meethue.com/).

But me? I don't learn that way. I learn by solving problems. So this is my test project, the one where I (probably) figure out in the end that I should have used their SDK.

# HUE API

So initially I was only interested in the actual lights so that is the only part of the API I have created here. I wanted to be able to control arbitrary groups of different lights in different contexts (eg: Master Bedroom.*, Downstairs, Livingroom) where they may overlap rooms (score one for open plan living).

I imagine this evolving into a simple expression language to build shortcuts to complex setups.

## Quickstart

I haven't worried about bridge discovery or username white listing (I created that via the debug/clip.html endpoint).

```java
	//Create a hue instance using the IP and a username.
	Hue hue = new Hue("192.x.x.x", "username/from/debug/clip.html");
	//Get the list of lights from the bridge
	List<Hue.Light> lights = hue.lights();
```

This list of lights updates if it isn't cached or is older than 10 seconds. To push a new state to the light:

```java
	Hue.Light light=lights.get(0);
	light.push(Light.newState(4000).withBrightness(1.0f));
```

This example will set the first light to 100% brightness over a duration of 4 seconds. Each of the responses from the state requests set the state in the light - so when a request is made the state of the light should be synchronized. 

If the light list is combined with the Java stream API it makes it very easy to batch process a group of lights:

```java
	hue.lights().stream().filter(light -> light.getName().matches("(Living|Hallway 1|Dining).*")).forEach(light -> {
	  try {
	    light.push(Light.newState(4000).withBrightness(1.0f).withColorTemperature(500));
	  } catch (RestException e) {
	
	  }
	});
```

So easy in fact I added an functional interface that hides the RestException and delegated the streaming forEach method - which gives this pattern:

```java
	hue.lights(light -> {
	  if (light.getName().matches("(Living|Hallway 1|Dining).*")) {
	    light.push(Light.newState(4000).withBrightness(1.0f).withColorTemperature(500));
	  }
	});
```

This example sets all of the matching light to 100% brightness with a specific color temperature.

## TODO

See how this evolves! Or doesn't...