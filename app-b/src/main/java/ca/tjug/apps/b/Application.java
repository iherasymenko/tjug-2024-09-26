package ca.tjug.apps.b;

import ca.tjug.libs.one.One;
import ca.tjug.libs.two.Two;

import static java.io.IO.*;

public class Application {

    void main() {
        var one = new One();
        var two = new Two();
        println(one.doOne());
        two.doTwo();
        println("Powered by JDK %s by %s".formatted(System.getProperty("java.version"), System.getProperty("java.vendor")));
        println("App B did its job and finished successfully");
    }

}
