package com.smartbear.mqttsupport.connection.dialog;

import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.propertyexpansion.DefaultPropertyExpansionContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.JTextField;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class EditConnectionDialogTest {

    private AutoCloseable closeable;
    private MockedStatic<UISupport> uiSupportMock;

    private final String NON_EXISTING_PATH = "non/existing/file/path/file.txt";
    private final String TEMPORARY_FILE_NAME = "temporary_file.txt";

    private EditConnectionDialog dialog;

    @Mock
    private DefaultPropertyExpansionContext context;

    @TempDir
    private File tempDir;

    @Mock
    private JTextField edit;

    @Mock
    private ModelItem modelItemOfConnection;

    @BeforeEach
    void setUp() throws Exception {
        closeable = openMocks(this);
        dialog = Mockito.mock(EditConnectionDialog.class, Mockito.CALLS_REAL_METHODS);
        setDialogField("modelItemOfConnection", modelItemOfConnection);
        doNothing().when(dialog).activateTab(anyString());
        uiSupportMock = Mockito.mockStatic(UISupport.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (uiSupportMock != null) {
            uiSupportMock.close();
        }
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    void testExpandReplacesPropertyWithCorrectValue() {
        String dummyProjectDir = "/Users/aragorn.elessar/Documents/ReadyAPI/projects-readyapi";
        String input = "${projectDir}/test.txt";
        String expectedOutput = dummyProjectDir + "/test.txt";

        when(context.getProperty("projectDir")).thenReturn(dummyProjectDir);

        String actualOutput = PropertyExpander.expandProperties(context, input);

        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    void testExpandWhenInputIsNull() {
        String actualOutput = PropertyExpander.expandProperties(context, null);

        assertNull(actualOutput);
    }

    @Test
    void testExpandWhenInputIsEmpty() {
        String actualOutput = PropertyExpander.expandProperties(context, "");

        assertEquals("", actualOutput);
    }

    @Test
    void testExpandWhenInputIsNotAProperty() {
        String input = "/Users/aragorn.elessar/Documents/ReadyAPI/projects-readyapi";
        String actualOutput = PropertyExpander.expandProperties(context, input);

        assertEquals(input, actualOutput);
    }

    @Test
    void testExpandWhenPropertyDoesNotExist() {
        String input = "${nonExistingProperty}/test.txt";
        String expectedOutput = "/test.txt";

        String actualOutput = PropertyExpander.expandProperties(context, input);

        assertEquals(expectedOutput, actualOutput);
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

    @Test
    void testCheckCertificateFieldWhenFileIsDirectory() throws IOException {
        Path createdDirectory = Files.createDirectory(tempDir.toPath().resolve(TEMPORARY_FILE_NAME));
        when(edit.getText()).thenReturn(createdDirectory.toString());

        boolean result = dialog.checkCertificateField(edit);

        assertFalse(result);
    }

    private void setDialogField(String fieldName, Object value) throws Exception {
        Field field = EditConnectionDialog.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(dialog, value);
    }

}

