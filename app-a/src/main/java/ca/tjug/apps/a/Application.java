package ca.tjug.apps.a;

import ca.tjug.libs.one.One;

import static java.io.IO.*;

public class Application {

    void main() {
        var one = new One();
        println(one.doOne());
        println("Powered by JDK %s by %s".formatted(System.getProperty("java.version"), System.getProperty("java.vendor")));
        println("App A did its job and finished successfully");
    }

}
