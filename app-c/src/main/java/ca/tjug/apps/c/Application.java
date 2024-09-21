package ca.tjug.apps.c;

import ca.tjug.libs.one.One;
import ca.tjug.libs.three.Three;
import ca.tjug.libs.two.Two;

import static java.io.IO.*;

public class Application {

    void main() {
        var one = new One();
        var two = new Two();
        var three = new Three();
        println(one.doOne());
        two.doTwo();
        three.doThree();
        println("Powered by JDK %s by %s".formatted(System.getProperty("java.version"), System.getProperty("java.vendor")));
        println("App C did its job and finished successfully");
    }

}
