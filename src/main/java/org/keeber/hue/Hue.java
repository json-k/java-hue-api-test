package org.keeber.hue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import org.keeber.http.Rest;
import org.keeber.http.Rest.RestException;

import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

public class Hue {
  private Rest.Client rest;

  // private static Gson serializer = new GsonBuilder().setPrettyPrinting().create();
  //
  // private static void print(Object o) {
  // System.out.println(serializer.toJson(o));
  // }

  private List<Light> lights = new ArrayList<>();

  public Hue(String host, String user) {
    this.rest = Rest.newClient("http://" + host + "/api/" + user + "/");
  }

  private Light updateLight(Light nlight) {
    Optional<Light> olight = lights.stream().filter(l -> l.id.equals(nlight.id)).findFirst();
    if (olight.isPresent()) {
      for (Field field : nlight.getClass().getDeclaredFields()) {
        if (!Modifier.isTransient(field.getModifiers())) {
          field.setAccessible(true);
          try {
            field.set(olight.get(), field.get(nlight));
          } catch (IllegalArgumentException | IllegalAccessException e) {

          }
        }
      }
    } else {
      lights.add(nlight);
    }
    return nlight;
  }

  private long lastRequest = 0;

  public List<Light> lights() throws RestException {
    if (lights.isEmpty() || (System.currentTimeMillis() - lastRequest) > 10000) {
      Map<String, Light> mlights = rest.newRequest("lights").get().as(Light.MAP_TYPE);
      for (Entry<String, Light> entry : mlights.entrySet()) {
        updateLight(entry.getValue().setup(entry.getKey(), this));
      }
      for (Light light : lights.toArray(new Light[0])) {
        if (!mlights.containsKey(light.getId())) {
          lights.remove(light);
        }
      }
      lastRequest = System.currentTimeMillis();
    }
    return Collections.unmodifiableList(lights);
  }


  public void lights(Action foreach) throws RestException {
    lights().stream().forEach(foreach);
  }

  @FunctionalInterface
  public interface Action extends Consumer<Light> {
    @Override
    default void accept(final Light elem) {
      try {
        acceptThrows(elem);
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    void acceptThrows(Light elem) throws RestException;
  }

  public static class Light {

    public enum Alert {
      none, select, lselect;
    }

    public enum Effect {
      none, colorloop;
    }

    private static Type MAP_TYPE = new TypeToken<Map<String, Light>>() {}.getType();
    private State state;
    private String id, type, name, modelid, uniqueid, manu, facturername, luminaireuniqueid, swversion;
    private transient Hue parent;

    private Light setup(String id, Hue parent) {
      this.id = id;
      this.parent = parent;
      return this;
    }

    public State getState() {
      return state;
    }

    public String getId() {
      return id;
    }

    public String getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public String getModelid() {
      return modelid;
    }

    public String getUniqueid() {
      return uniqueid;
    }

    public String getManu() {
      return manu;
    }

    public String getFacturername() {
      return facturername;
    }

    public String getLuminaireuniqueid() {
      return luminaireuniqueid;
    }

    public String getSwversion() {
      return swversion;
    }

    public Light push(State state) throws RestException {
      parent.rest.newRequest("lights/" + this.id + "/state").put(state).<List<StateResponse>>as(StateResponse.LIST_TYPE).stream().forEach(sr -> {
        sr.ifSuccess(rmap -> {
          String fname;
          for (Entry<String, JsonPrimitive> entry : rmap.entrySet()) {
            fname = entry.getKey().replaceAll(".*/", "");
            try {
              Field field = State.class.getDeclaredField(fname);
              if (!Modifier.isTransient(field.getModifiers())) {
                field.setAccessible(true);
                try {
                  if (field.getType().equals(String.class)) {
                    field.set(this.state, entry.getValue().getAsString());
                  }
                  if (field.getType().equals(Integer.class)) {
                    field.set(this.state, entry.getValue().getAsInt());
                  }
                  if (field.getType().equals(Boolean.class)) {
                    field.set(this.state, entry.getValue().getAsBoolean());
                  }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                  e.printStackTrace();
                }
              }
            } catch (NoSuchFieldException | SecurityException e) {

            }
          }
        });
      });
      return parent.updateLight(this);
    }

    public static State newState(int transitiontime) {
      return new State().setTransitionTime(transitiontime);
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof Light)) {
        return false;
      }
      Light other = (Light) obj;
      if (id == null) {
        if (other.id != null) {
          return false;
        }
      } else if (!id.equals(other.id)) {
        return false;
      }
      return true;
    }



    public static class State {
      private Boolean on, reachable;
      private float[] xy;
      private Integer bri, hue, sat, ct;
      private String alert, effect, colormode;
      @SuppressWarnings("unused")
      private Integer transitiontime = 10;

      public boolean isOn() {
        return on;
      }

      public State withOn(boolean on) {
        this.on = on;
        return this;
      }

      private State setTransitionTime(int time) {
        this.transitiontime = (int) Math.round(time / 100f);
        return this;
      }

      public boolean isReachable() {
        return reachable;
      }

      public State withXY(float x, float y) {
        this.xy = new float[] {x, y};
        this.colormode = "xy";
        return this;
      }

      public float[] getXY() {
        return xy;
      }

      public State withBrightness(float brightness) {
        this.bri = Math.max(1, (int) Math.floor(brightness * 254));
        return this;
      }

      public int getBri() {
        return bri;
      }

      public State withHue(int hue) {
        this.hue = hue;
        return this;
      }

      public int getHue() {
        return hue;
      }

      public State withSaturation(int sat) {
        this.sat = sat;
        return this;
      }

      public int getSat() {
        return sat;
      }

      public State withColorTemperature(int ct) {
        this.ct = ct;
        return this;
      }

      public int getCt() {
        return ct;
      }

      public State withAlert(Alert alert) {
        this.alert = alert.toString();
        return this;
      }

      public String getAlert() {
        return alert;
      }

      public State withEffect(Effect effect) {
        this.effect = effect.toString();
        return this;
      }

      public String getEffect() {
        return effect;
      }

      public String getColormode() {
        return colormode;
      }

    }

  }

  @SuppressWarnings("unused")
  private static class StateResponse {
    private static Type LIST_TYPE = new TypeToken<List<StateResponse>>() {}.getType();

    private Error error;
    private Map<String, JsonPrimitive> success;

    public void ifSuccess(Consumer<Map<String, JsonPrimitive>> consumer) {
      if (success != null) {
        consumer.accept(success);
      }
    }

    private static class Error {
      private String type, address, description;


    }
  }



}
