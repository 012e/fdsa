package huyphmnat.fdsa.repository;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.repository.dtos.*;
import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import huyphmnat.fdsa.repository.interfaces.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RepositoryBrowseServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RepositoryFileService repositoryFileService;

    @Autowired
    private RepositoryRepository repositoryRepository;

    private UUID repositoryId;

    @BeforeEach
    public void setUp() {
        // Create a test repository with a known structure
        String identifier = "test-user/repo-" + UUID.randomUUID();
        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier(identifier)
                .description("Repository for browsing tests")
                .build());

        RepositoryEntity entity = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalStateException("Repository entity not found"));
        repositoryId = entity.getId();

        // Create a file structure:
        // /README.md
        // /src/
        // /src/main/
        // /src/main/java/
        // /src/main/java/App.java
        // /src/main/resources/
        // /src/main/resources/config.properties
        // /src/test/
        // /docs/
        // /docs/guide.md

        repositoryFileService.addFile(repositoryId, "README.md",
            "# Test Repository\n\nThis is a test.".getBytes(StandardCharsets.UTF_8),
            "Add README");

        repositoryFileService.createFolder(repositoryId, "src/main/java", "Create java folder");
        repositoryFileService.addFile(repositoryId, "src/main/java/App.java",
            "public class App {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}".getBytes(StandardCharsets.UTF_8),
            "Add App.java");

        repositoryFileService.createFolder(repositoryId, "src/main/resources", "Create resources folder");
        repositoryFileService.addFile(repositoryId, "src/main/resources/config.properties",
            "app.name=TestApp\napp.version=1.0".getBytes(StandardCharsets.UTF_8),
            "Add config");

        repositoryFileService.createFolder(repositoryId, "src/test", "Create test folder");

        repositoryFileService.createFolder(repositoryId, "docs", "Create docs folder");
        repositoryFileService.addFile(repositoryId, "docs/guide.md",
            "# User Guide\n\n## Getting Started\n\nFollow these steps...".getBytes(StandardCharsets.UTF_8),
            "Add guide");
    }

    @Test
    public void testListRootDirectory() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, null);

        assertNotNull(content);
        assertEquals("/", content.getPath());
        assertNotNull(content.getEntries());

        // Should have: README.md, src/, docs/
        List<Entry> entries = content.getEntries();
        assertTrue(entries.size() >= 3, "Should have at least 3 entries in root");


        // Verify we have the expected entries
        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("README.md") && e.getType() == FileEntryType.FILE));
        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("src") && e.getType() == FileEntryType.DIRECTORY));
        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("docs") && e.getType() == FileEntryType.DIRECTORY));
    }

    @Test
    public void testListRootDirectoryWithSlash() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        assertNotNull(content);
        assertEquals("/", content.getPath());
        assertTrue(content.getEntries().size() >= 3);
    }

    @Test
    public void testListRootDirectoryWithEmptyString() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "");

        assertNotNull(content);
        assertEquals("/", content.getPath());
        assertTrue(content.getEntries().size() >= 3);
    }

    @Test
    public void testListSubdirectory() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "src");

        assertNotNull(content);
        assertEquals("src", content.getPath());
        assertNotNull(content.getEntries());

        // Should have: main/, test/
        List<Entry> entries = content.getEntries();
        assertTrue(entries.size() >= 2, "src/ should have main and test subdirectories");

        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("main") && e.getType() == FileEntryType.DIRECTORY));
        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("test") && e.getType() == FileEntryType.DIRECTORY));
    }

    @Test
    public void testListNestedDirectory() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "src/main");

        assertNotNull(content);
        assertEquals("src/main", content.getPath());

        List<Entry> entries = content.getEntries();
        assertTrue(entries.size() >= 2, "src/main should have java and resources");

        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("java") && e.getType() == FileEntryType.DIRECTORY));
        assertTrue(entries.stream().anyMatch(e -> e.getName().equals("resources") && e.getType() == FileEntryType.DIRECTORY));
    }

    @Test
    public void testListDirectoryWithFiles() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "src/main/java");

        assertNotNull(content);
        assertEquals("src/main/java", content.getPath());

        List<Entry> entries = content.getEntries();

        // Should have App.java and possibly .gitkeep
        Entry app = entries.stream()
            .filter(e -> e.getName().equals("App.java"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Should have App.java"));


        assertInstanceOf(FileEntry.class, app);
        FileEntry appJava = (FileEntry) app;
        assertEquals(FileEntryType.FILE, appJava.getType());
        assertNotNull(appJava.getSize());
        assertTrue(appJava.getSize() > 0, "App.java should have content");
        assertEquals("src/main/java/App.java", appJava.getPath());
    }

    @Test
    public void testListDirectoryWithMixedContent() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "docs");

        assertNotNull(content);

        List<Entry> entries = content.getEntries();

        // Should have guide.md and possibly .gitkeep
        Entry guideMdEntry = entries.stream()
            .filter(e -> e.getName().equals("guide.md"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Should have guide.md"));

        assertInstanceOf(FileEntry.class, guideMdEntry);
        FileEntry guideMd = (FileEntry) guideMdEntry;

        assertEquals(FileEntryType.FILE, guideMd.getType());
        assertNotNull(guideMd.getSize());
        assertTrue(guideMd.getSize() > 0);
    }

    @Test
    public void testListEmptyDirectory() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "src/test");

        assertNotNull(content);
        assertEquals("src/test", content.getPath());

        // May contain .gitkeep
        List<Entry> entries = content.getEntries();
        assertNotNull(entries);
    }

    @Test
    public void testListNonExistentDirectory() {
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> repositoryFileService.listDirectory(repositoryId, "non/existent/path"));

        assertTrue(exception.getMessage().contains("Directory not found"));
    }

    @Test
    public void testListDirectoryOnFilePath() {
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> repositoryFileService.listDirectory(repositoryId, "README.md"));

        assertTrue(exception.getMessage().contains("not a directory"));
    }

    @Test
    public void testReadFile() {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "README.md");

        assertNotNull(fileContent);
        assertEquals("README.md", fileContent.getPath());
        assertEquals("README.md", fileContent.getName());
        assertNotNull(fileContent.getSize());
        assertTrue(fileContent.getSize() > 0);
        assertNotNull(fileContent.getContent());

        String content = new String(fileContent.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("Test Repository"));
        assertTrue(content.contains("This is a test"));
    }

    @Test
    public void testReadNestedFile() {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "src/main/java/App.java");

        assertNotNull(fileContent);
        assertEquals("src/main/java/App.java", fileContent.getPath());
        assertEquals("App.java", fileContent.getName());
        assertNotNull(fileContent.getSize());
        assertNotNull(fileContent.getContent());

        String content = new String(fileContent.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("public class App"));
        assertTrue(content.contains("main"));
        assertTrue(content.contains("System.out.println"));
    }

    @Test
    public void testReadPropertiesFile() {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "src/main/resources/config.properties");

        assertNotNull(fileContent);
        assertEquals("config.properties", fileContent.getName());

        String content = new String(fileContent.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("app.name=TestApp"));
        assertTrue(content.contains("app.version=1.0"));
    }

    @Test
    public void testReadMarkdownFile() {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "docs/guide.md");

        assertNotNull(fileContent);
        assertEquals("guide.md", fileContent.getName());

        String content = new String(fileContent.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("User Guide"));
        assertTrue(content.contains("Getting Started"));
    }

    @Test
    public void testReadNonExistentFile() {
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> repositoryFileService.readFile(repositoryId, "non/existent/file.txt"));

        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    public void testReadDirectoryAsFile() {
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> repositoryFileService.readFile(repositoryId, "src"));

        assertTrue(exception.getMessage().contains("not a file"));
    }

    @Test
    public void testDirectoryEntriesAreSorted() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        List<Entry> entries = content.getEntries();

        // Directories should come before files
        boolean seenFile = false;
        for (Entry entry : entries) {
            if (entry.getType() == FileEntryType.FILE) {
                seenFile = true;
            } else if (entry.getType() == FileEntryType.DIRECTORY && seenFile) {
                fail("Directories should come before files in sorted list");
            }
        }

        // Within each type, entries should be alphabetically sorted
        List<String> dirNames = entries.stream()
            .filter(e -> e.getType() == FileEntryType.DIRECTORY)
            .map(Entry::getName)
            .toList();

        for (int i = 1; i < dirNames.size(); i++) {
            assertTrue(dirNames.get(i - 1).compareTo(dirNames.get(i)) <= 0,
                "Directory names should be alphabetically sorted");
        }
    }

    @Test
    public void testFileEntriesHaveCorrectMetadata() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        Entry readmeEntryBase = content.getEntries().stream()
            .filter(e -> e.getName().equals("README.md"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Should have README.md"));

        assertEquals("README.md", readmeEntryBase.getName());
        assertEquals("README.md", readmeEntryBase.getPath());
        assertEquals(FileEntryType.FILE, readmeEntryBase.getType());
        assertInstanceOf(FileEntry.class, readmeEntryBase);
        FileEntry readmeEntry = (FileEntry) readmeEntryBase;
        assertNotNull(readmeEntry.getSize());
        assertTrue(readmeEntry.getSize() > 0);
    }

    @Test
    public void testDirectoryEntriesHaveNullSize() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        List<Entry> directories = content.getEntries().stream()
            .filter(e -> e.getType() == FileEntryType.DIRECTORY)
            .toList();

        assertFalse(directories.isEmpty(), "Should have some directories");

        for (Entry dir : directories) {
            assertInstanceOf(DirectoryEntry.class, dir, "Directory entries should be DirectoryEntry instances");
            // DirectoryEntry doesn't have a size field, so we can't access it
            // The fact that it's a DirectoryEntry is sufficient validation
        }
    }

    @Test
    public void testPathTraversalProtection() {
        // Try to access outside repository
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> repositoryFileService.listDirectory(repositoryId, "../../../etc"));

        assertTrue(exception.getMessage().contains("Invalid path") ||
                   exception.getMessage().contains("outside repository"));
    }

    @Test
    public void testReadFileAfterUpdate() {
        // Update a file
        String newContent = "# Updated README\n\nNew content here.";
        repositoryFileService.updateFile(repositoryId, "README.md",
            newContent.getBytes(StandardCharsets.UTF_8), "Update README");

        // Read it back
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "README.md");

        String content = new String(fileContent.getContent(), StandardCharsets.UTF_8);
        assertEquals(newContent, content);
    }

    @Test
    public void testListDirectoryAfterAddingFile() {
        // Add a new file
        repositoryFileService.addFile(repositoryId, "NEW_FILE.txt",
            "New file content".getBytes(StandardCharsets.UTF_8), "Add new file");

        // List directory
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        assertTrue(content.getEntries().stream()
            .anyMatch(e -> e.getName().equals("NEW_FILE.txt")));
    }

    @Test
    public void testListDirectoryAfterDeletingFile() {
        // Delete a file
        repositoryFileService.deleteFile(repositoryId, "README.md", "Delete README");

        // List directory
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        assertFalse(content.getEntries().stream()
            .anyMatch(e -> e.getName().equals("README.md")));
    }

    @Test
    public void testGitFilesAreExcluded() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        // .git directory should not be listed
        assertFalse(content.getEntries().stream()
            .anyMatch(e -> e.getName().startsWith(".git")),
            ".git files/folders should be excluded from listing");
    }
}

