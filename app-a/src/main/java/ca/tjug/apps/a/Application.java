package ca.tjug.apps.a;

import ca.tjug.libs.one.One;

// https://github.com/gradle/gradle/issues/1251
import static java.lang.System.out;

public class Application {

    void main() {
        var one = new One();
        out.println(one.doOne());
        out.println("Powered by JDK %s by %s".formatted(System.getProperty("java.version"), System.getProperty("java.vendor")));
        out.println("App A did its job and finished successfully");
    }

}
