package org.openu.hyperltl.cases.systems;

import org.openu.hyperltl.model.SystemModel;
import org.openu.hyperltl.model.SystemModelLoader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class TestSystems {

    public static final SystemModel system1;
    public static final SystemModel system2;
    public static final SystemModel system3;
    public static final SystemModel system4;
    public static final SystemModel system5;
    public static final SystemModel system6;

    public static final List<SystemModel> systems;

    private static Path getSystemPath(String path) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(TestSystems.class.getResource(path)).toURI());
    }

    static {
        try {
            system1 = SystemModelLoader.load(getSystemPath("/systems/system-1.json"));
            system2 = SystemModelLoader.load(getSystemPath("/systems/system-2.json"));
            system3 = SystemModelLoader.load(getSystemPath("/systems/system-3.json"));
            system4 = SystemModelLoader.load(getSystemPath("/systems/system-4.json"));
            system5 = SystemModelLoader.load(getSystemPath("/systems/system-5.json"));
            system6 = SystemModelLoader.load(getSystemPath("/systems/system-6.json"));

            systems = List.of(system1, system2, system3, system4, system5, system6);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
