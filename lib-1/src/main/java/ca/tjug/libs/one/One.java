package ca.tjug.libs.one;

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

public final class One {

    public JSONObject doOne() {
        /*
         org.yaml.snakeyaml.Yaml is an internal dependency that is
         not exposed to the consumer of this library.
        */
        var yaml = new Yaml();
        /*
         org.json.JSONObject is a public API that
         is exposed to the consumer of this library.
        */
        return new JSONObject();
    }

}
