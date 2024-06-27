package com.smartbear.mqttsupport.connection.dialog;

import com.eviware.soapui.model.ModelItem;
import com.smartbear.mqttsupport.connection.ConnectionParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import javax.swing.JTextField;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class EditConnectionDialogTest {

    private AutoCloseable closeable;

    private final String TITLE = "title";
    private final String INITITAL_CONNECTION_NAME = "initialConnectionName";
    private List<String> ALREADY_PRESENT_NAMES = List.of("name1", "name2");

    private final String NON_EXISTING_PATH = "non/existing/file/path/file.txt";
    private final String TEMPORARY_FILE_NAME = "temporary_file.txt";

    @Spy
    private TestableEditConnectionDialog dialog;

    @TempDir
    private File tempDir;

    @Mock
    private JTextField edit;

    @Mock
    private ModelItem modelItemOfConnection;

    @Mock
    private ConnectionParams initialConnectionParams;


    @BeforeEach
    void setUp() {
        closeable = openMocks(this);
        dialog = Mockito.spy(new TestableEditConnectionDialog(TITLE, modelItemOfConnection, INITITAL_CONNECTION_NAME,
                initialConnectionParams, false, ALREADY_PRESENT_NAMES));
        doNothing().when(dialog).activateTab(anyString());
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testCheckCertificateFieldWhenInputIsEmpty() {
        when(edit.getText()).thenReturn("");
        boolean result = dialog.checkCertificateField(edit);

        assertTrue(result);
    }

    @Test
    void testCheckCertificateFieldWhenInputIsNull() {
        when(edit.getText()).thenReturn(null);
        boolean result = dialog.checkCertificateField(edit);

        assertTrue(result);
    }

    @Test
    void testCheckCertificateFieldWhenFileExists() throws IOException {
        Path createdFile = Files.createFile(tempDir.toPath().resolve(TEMPORARY_FILE_NAME));
        when(edit.getText()).thenReturn(createdFile.toString());

        boolean result = dialog.checkCertificateField(edit);

        assertTrue(result);
    }

    @Test
    void testCheckCertificateFieldWhenFileDoesNotExist() {
        when(edit.getText()).thenReturn(NON_EXISTING_PATH);

        boolean result = dialog.checkCertificateField(edit);

        assertFalse(result);
    }

    private class TestableEditConnectionDialog extends EditConnectionDialog {
        public TestableEditConnectionDialog() {
            super(null, null, null, null, false, null);
        }

        public TestableEditConnectionDialog(String title, ModelItem modelItemOfConnection, String initialConnectionName,
                                            ConnectionParams initialConnectionParams, boolean legacy, List<String> alreadyPresentNames) {
            super(title, modelItemOfConnection, initialConnectionName, initialConnectionParams, legacy, alreadyPresentNames);
        }

    }

}

