package generation;

import dev.dagless.model.splitting.SplitFunction;
import dev.dagless.service.generation.DirectoryMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.code.CtBlock;
import spoon.reflect.factory.CoreFactory;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DirectoryMonitorServiceTest {
    CtBlock<?> mainBody;

    @BeforeEach
    public void setup() {
        // Create a Spoon Launcher
        Launcher launcher = new Launcher();
        CoreFactory coreFactory = launcher.getFactory().Core();
        Factory factory = new FactoryImpl(coreFactory, launcher.getEnvironment());

        // Create an empty CtBlock
        mainBody = factory.createBlock();
    }

    @Test
    public void testStart() {
        DirectoryMonitorService.generateDirectoryMonitorStart(mainBody);

        String expected = "dev.dagless.DirectoryMonitor directoryMonitor = new dev.dagless.DirectoryMonitor(enableDirectoryMonitoring)";
        String actual = mainBody.getStatement(0).toString();

        assertEquals(expected, actual);

        expected = "directoryMonitor.startMonitoring()";
        actual = mainBody.getStatement(1).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testEnd() {
        DirectoryMonitorService.generateDirectoryMonitoringEnd(mainBody);

        String expected = "directoryMonitor.stopMonitoring(null)";
        String actual = mainBody.getStatement(0).toString();

        assertEquals(expected, actual);
    }
}
