import org.junit.jupiter.api.*;

import java.awt.Polygon;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Milestone1Tests {
    @Test
    @Order(1)
    public void testSetPageSize() throws Exception {
        String configFile = "config";

        if (Files.exists(Paths.get(configFile, "DBApp.config"))) {
            configFile = Paths.get(configFile, "DBApp.config").toString();
        } else {
            throw new Exception("Cannot open config file");
        }

        List<String> config = Files.readAllLines(Paths.get(configFile));
        boolean lineFound = false;
        for (int i = 0; i < config.size(); i++) {
            if (config.get(i).toLowerCase().contains("page")) {
                config.set(i, config.get(i).replaceAll("\\d+", "250"));
                lineFound = true;
                break;
            }
        }

        if (!lineFound) {
            throw new Exception("Cannot set page size");
        }

        System.out.println("CONFIG" + configFile);
        Files.write(Paths.get(configFile), config);

    }

}

