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

import static org.assertj.core.api.Assertions.*;

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

        assertThat(content).isNotNull();
        assertThat(content.getPath()).isEqualTo("/");
        assertThat(content.getEntries()).isNotNull();

        // Should have: README.md, src/, docs/
        List<Entry> entries = content.getEntries();
        assertThat(entries).hasSizeGreaterThanOrEqualTo(3);

        // Verify we have the expected entries
        assertThat(entries).anyMatch(e -> e.getName().equals("README.md") && e.getType() == FileEntryType.FILE);
        assertThat(entries).anyMatch(e -> e.getName().equals("src") && e.getType() == FileEntryType.DIRECTORY);
        assertThat(entries).anyMatch(e -> e.getName().equals("docs") && e.getType() == FileEntryType.DIRECTORY);
    }

    @Test
    public void testListRootDirectoryWithSlash() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        assertThat(content).isNotNull();
        assertThat(content.getPath()).isEqualTo("/");
        assertThat(content.getEntries()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    public void testListRootDirectoryWithEmptyString() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "");

        assertThat(content).isNotNull();
        assertThat(content.getPath()).isEqualTo("/");
        assertThat(content.getEntries()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    public void testListSubdirectory() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "src");

        assertThat(content).isNotNull();
        assertThat(content.getPath()).isEqualTo("src");
        assertThat(content.getEntries()).isNotNull();

        // Should have: main/, test/
        List<Entry> entries = content.getEntries();
        assertThat(entries).hasSizeGreaterThanOrEqualTo(2);

        assertThat(entries).anyMatch(e -> e.getName().equals("main") && e.getType() == FileEntryType.DIRECTORY);
        assertThat(entries).anyMatch(e -> e.getName().equals("test") && e.getType() == FileEntryType.DIRECTORY);
    }

    @Test
    public void testListNestedDirectory() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "src/main");

        assertThat(content).isNotNull();
        assertThat(content.getPath()).isEqualTo("src/main");

        List<Entry> entries = content.getEntries();
        assertThat(entries).hasSizeGreaterThanOrEqualTo(2);

        assertThat(entries).anyMatch(e -> e.getName().equals("java") && e.getType() == FileEntryType.DIRECTORY);
        assertThat(entries).anyMatch(e -> e.getName().equals("resources") && e.getType() == FileEntryType.DIRECTORY);
    }

    @Test
    public void testListDirectoryWithFiles() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "src/main/java");

        assertThat(content).isNotNull();
        assertThat(content.getPath()).isEqualTo("src/main/java");

        List<Entry> entries = content.getEntries();

        // Should have App.java and possibly .gitkeep
        Entry app = entries.stream()
            .filter(e -> e.getName().equals("App.java"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Should have App.java"));

        assertThat(app).isInstanceOf(FileEntry.class);
        FileEntry appJava = (FileEntry) app;
        assertThat(appJava.getType()).isEqualTo(FileEntryType.FILE);
        assertThat(appJava.getSize()).isNotNull();
        assertThat(appJava.getSize()).isGreaterThan(0);
        assertThat(appJava.getPath()).isEqualTo("src/main/java/App.java");
    }

    @Test
    public void testListDirectoryWithMixedContent() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "docs");

        assertThat(content).isNotNull();

        List<Entry> entries = content.getEntries();

        // Should have guide.md and possibly .gitkeep
        Entry guideMdEntry = entries.stream()
            .filter(e -> e.getName().equals("guide.md"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Should have guide.md"));

        assertThat(guideMdEntry).isInstanceOf(FileEntry.class);
        FileEntry guideMd = (FileEntry) guideMdEntry;

        assertThat(guideMd.getType()).isEqualTo(FileEntryType.FILE);
        assertThat(guideMd.getSize()).isNotNull();
        assertThat(guideMd.getSize()).isGreaterThan(0);
    }

    @Test
    public void testListEmptyDirectory() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "src/test");

        assertThat(content).isNotNull();
        assertThat(content.getPath()).isEqualTo("src/test");

        // May contain .gitkeep
        List<Entry> entries = content.getEntries();
        assertThat(entries).isNotNull();
    }

    @Test
    public void testListNonExistentDirectory() {
        Throwable exception = catchThrowable(() -> repositoryFileService.listDirectory(repositoryId, "non/existent/path"));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).contains("Directory not found");
    }

    @Test
    public void testListDirectoryOnFilePath() {
        Throwable exception = catchThrowable(() -> repositoryFileService.listDirectory(repositoryId, "README.md"));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).contains("not a directory");
    }

    @Test
    public void testReadFile() {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "README.md");

        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getPath()).isEqualTo("README.md");
        assertThat(fileContent.getName()).isEqualTo("README.md");
        assertThat(fileContent.getSize()).isNotNull();
        assertThat(fileContent.getSize()).isGreaterThan(0);
        assertThat(fileContent.getContent()).isNotNull();

        String content = fileContent.getContent();
        assertThat(content).contains("Test Repository");
        assertThat(content).contains("This is a test");
    }

    @Test
    public void testReadNestedFile() {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "src/main/java/App.java");

        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getPath()).isEqualTo("src/main/java/App.java");
        assertThat(fileContent.getName()).isEqualTo("App.java");
        assertThat(fileContent.getSize()).isNotNull();
        assertThat(fileContent.getContent()).isNotNull();

        String content = fileContent.getContent();
        assertThat(content).contains("public class App");
        assertThat(content).contains("main");
        assertThat(content).contains("System.out.println");
    }

    @Test
    public void testReadPropertiesFile() {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "src/main/resources/config.properties");

        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getName()).isEqualTo("config.properties");

        String content = fileContent.getContent();
        assertThat(content).contains("app.name=TestApp");
        assertThat(content).contains("app.version=1.0");
    }

    @Test
    public void testReadMarkdownFile() {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "docs/guide.md");

        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getName()).isEqualTo("guide.md");

        String content = fileContent.getContent();
        assertThat(content).contains("User Guide");
        assertThat(content).contains("Getting Started");
    }

    @Test
    public void testReadNonExistentFile() {
        Throwable exception = catchThrowable(() -> repositoryFileService.readFile(repositoryId, "non/existent/file.txt"));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).contains("File not found");
    }

    @Test
    public void testReadDirectoryAsFile() {
        Throwable exception = catchThrowable(() -> repositoryFileService.readFile(repositoryId, "src"));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).contains("not a file");
    }

    @Test
    public void testDirectoryEntriesAreSorted() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        List<Entry> entries = content.getEntries();

        // Directories should come before files
        boolean seenFile = false;
        boolean directoryAfterFile = false;
        for (Entry entry : entries) {
            if (entry.getType() == FileEntryType.FILE) {
                seenFile = true;
            } else if (entry.getType() == FileEntryType.DIRECTORY && seenFile) {
                directoryAfterFile = true;
                break;
            }
        }

        assertThat(directoryAfterFile).isFalse();

        // Within each type, entries should be alphabetically sorted
        List<String> dirNames = entries.stream()
            .filter(e -> e.getType() == FileEntryType.DIRECTORY)
            .map(Entry::getName)
            .toList();

        for (int i = 1; i < dirNames.size(); i++) {
            assertThat(dirNames.get(i - 1).compareTo(dirNames.get(i))).isLessThanOrEqualTo(0);
        }
    }

    @Test
    public void testFileEntriesHaveCorrectMetadata() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        Entry readmeEntryBase = content.getEntries().stream()
            .filter(e -> e.getName().equals("README.md"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Should have README.md"));

        assertThat(readmeEntryBase.getName()).isEqualTo("README.md");
        assertThat(readmeEntryBase.getPath()).isEqualTo("README.md");
        assertThat(readmeEntryBase.getType()).isEqualTo(FileEntryType.FILE);
        assertThat(readmeEntryBase).isInstanceOf(FileEntry.class);
        FileEntry readmeEntry = (FileEntry) readmeEntryBase;
        assertThat(readmeEntry.getSize()).isNotNull();
        assertThat(readmeEntry.getSize()).isGreaterThan(0);
    }

    @Test
    public void testDirectoryEntriesHaveNullSize() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        List<Entry> directories = content.getEntries().stream()
            .filter(e -> e.getType() == FileEntryType.DIRECTORY)
            .toList();

        assertThat(directories).isNotEmpty();

        for (Entry dir : directories) {
            assertThat(dir).isInstanceOf(DirectoryEntry.class);
            // DirectoryEntry doesn't have a size field, so we can't access it
            // The fact that it's a DirectoryEntry is sufficient validation
        }
    }

    @Test
    public void testPathTraversalProtection() {
        // Try to access outside repository
        Throwable exception = catchThrowable(() -> repositoryFileService.listDirectory(repositoryId, "../../../etc"));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).containsAnyOf("Invalid path", "outside repository");
    }

    @Test
    public void testReadFileAfterUpdate() {
        // Update a file
        String newContent = "# Updated README\n\nNew content here.";
        repositoryFileService.updateFile(repositoryId, "README.md",
            newContent.getBytes(StandardCharsets.UTF_8), "Update README");

        // Read it back
        FileContent fileContent = repositoryFileService.readFile(repositoryId, "README.md");

        String content = fileContent.getContent();
        assertThat(content).isEqualTo(newContent);
    }

    @Test
    public void testListDirectoryAfterAddingFile() {
        // Add a new file
        repositoryFileService.addFile(repositoryId, "NEW_FILE.txt",
            "New file content".getBytes(StandardCharsets.UTF_8), "Add new file");

        // List directory
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        assertThat(content.getEntries()).anyMatch(e -> e.getName().equals("NEW_FILE.txt"));
    }

    @Test
    public void testListDirectoryAfterDeletingFile() {
        // Delete a file
        repositoryFileService.deleteFile(repositoryId, "README.md", "Delete README");

        // List directory
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        assertThat(content.getEntries()).noneMatch(e -> e.getName().equals("README.md"));
    }

    @Test
    public void testGitFilesAreExcluded() {
        DirectoryContent content = repositoryFileService.listDirectory(repositoryId, "/");

        // .git directory should not be listed
        assertThat(content.getEntries()).noneMatch(e -> e.getName().startsWith(".git"));
    }
}

