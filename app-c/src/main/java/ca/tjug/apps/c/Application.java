package ca.tjug.apps.c;

import ca.tjug.libs.one.One;
import ca.tjug.libs.three.Three;
import ca.tjug.libs.two.Two;

public class Application {

    public static void main(String[] args) {
        var one = new One();
        var two = new Two();
        var three = new Three();
        System.out.print(one.doOne());
        two.doTwo();
        three.doThree();
        System.out.println("Powered by JDK %s by %s".formatted(System.getProperty("java.version"), System.getProperty("java.vendor")));
        System.out.println("App C did its job and finished successfully");
    }

}
