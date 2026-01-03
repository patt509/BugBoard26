package com.bugboard.service;

import com.bugboard.dto.DashboardStatsDTO;
import com.bugboard.dto.IssueDTO;
import com.bugboard.enums.IssueStatus;
import com.bugboard.enums.PriorityLevel;
import com.bugboard.enums.UserRole;
import com.bugboard.model.Issue;
import com.bugboard.model.User;
import com.bugboard.repository.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the IssueService class.
 * Uses Mockito to mock repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IssueService Tests")
class IssueServiceTest {

    @Mock
    private IssueRepository repository;

    @InjectMocks
    private IssueService issueService;

    private User reporter;
    private User admin;
    private Issue sampleIssue;

    @BeforeEach
    void setUp() {
        reporter = new User("reporter@company.com", "password", UserRole.USER);
        reporter.finalizeProfile("Reporter");

        admin = new User("admin@company.com", "password", UserRole.ADMIN);
        admin.finalizeProfile("AdminUser");

        sampleIssue = new Issue("Sample issue title here", "Sample description", reporter);
    }

    // ==================== CREATE ISSUE TESTS ====================

    @Nested
    @DisplayName("Create Issue Tests")
    class CreateIssueTests {

        @Test
        @DisplayName("Should create issue with minimal parameters")
        void shouldCreateIssueWithMinimalParams() {
            // Act
            issueService.createIssue("Valid title for the issue", "Description", reporter);

            // Assert
            ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
            verify(repository).save(issueCaptor.capture());

            Issue savedIssue = issueCaptor.getValue();
            assertEquals("Valid title for the issue", savedIssue.getTitle());
            assertEquals("Description", savedIssue.getDescription());
            assertEquals(reporter, savedIssue.getReporter());
            assertEquals(IssueStatus.TODO, savedIssue.getStatus());
            assertEquals(PriorityLevel.MEDIUM, savedIssue.getPriority());
        }

        @Test
        @DisplayName("Should create issue from DTO")
        void shouldCreateIssueFromDTO() {
            // Arrange
            IssueDTO dto = IssueDTO.builder()
                    .title("Issue title from DTO")
                    .description("Description from DTO")
                    .priority("HIGH")
                    .build();

            // Act
            issueService.createIssue(dto, reporter);

            // Assert
            ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
            verify(repository).save(issueCaptor.capture());

            Issue savedIssue = issueCaptor.getValue();
            assertEquals("Issue title from DTO", savedIssue.getTitle());
            assertEquals(PriorityLevel.HIGH, savedIssue.getPriority());
        }

        @Test
        @DisplayName("Should use default priority when not specified in DTO")
        void shouldUseDefaultPriorityWhenNotSpecified() {
            // Arrange
            IssueDTO dto = IssueDTO.builder()
                    .title("Issue without priority")
                    .description("Description")
                    .build();

            // Act
            issueService.createIssue(dto, reporter);

            // Assert
            ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
            verify(repository).save(issueCaptor.capture());

            assertEquals(PriorityLevel.MEDIUM, issueCaptor.getValue().getPriority());
        }
    }

    // ==================== UPDATE STATUS TESTS ====================

    @Nested
    @DisplayName("Update Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update issue status")
        void shouldUpdateIssueStatus() {
            // Arrange
            when(repository.findById(1L)).thenReturn(sampleIssue);

            // Act
            issueService.updateStatus(1L, IssueStatus.IN_PROGRESS);

            // Assert
            assertEquals(IssueStatus.IN_PROGRESS, sampleIssue.getStatus());
            verify(repository).save(sampleIssue);
        }

        @Test
        @DisplayName("Should throw exception for non-existent issue")
        void shouldThrowExceptionForNonExistentIssue() {
            // Arrange
            when(repository.findById(999L)).thenReturn(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> issueService.updateStatus(999L, IssueStatus.IN_PROGRESS));
            assertEquals("Issue not found", exception.getMessage());
        }
    }

    // ==================== GET ISSUES TESTS ====================

    @Nested
    @DisplayName("Get Issues Tests")
    class GetIssuesTests {

        @Test
        @DisplayName("Should return all issues as DTOs")
        void shouldReturnAllIssuesAsDTOs() {
            // Arrange
            Issue issue1 = new Issue("First issue title", "Description 1", reporter);
            Issue issue2 = new Issue("Second issue title", "Description 2", reporter);
            when(repository.findAll()).thenReturn(Arrays.asList(issue1, issue2));

            // Act
            List<IssueDTO> result = issueService.getAllIssues();

            // Assert
            assertEquals(2, result.size());
            assertEquals("First issue title", result.get(0).getTitle());
            assertEquals("Second issue title", result.get(1).getTitle());
        }

        @Test
        @DisplayName("Should return empty list when no issues exist")
        void shouldReturnEmptyListWhenNoIssues() {
            // Arrange
            when(repository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<IssueDTO> result = issueService.getAllIssues();

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should get issue by ID")
        void shouldGetIssueById() {
            // Arrange
            when(repository.findById(1L)).thenReturn(sampleIssue);

            // Act
            IssueDTO result = issueService.getIssueById(1L);

            // Assert
            assertNotNull(result);
            assertEquals("Sample issue title here", result.getTitle());
            assertEquals("Reporter", result.getReporterName());
        }

        @Test
        @DisplayName("Should throw exception for non-existent issue ID")
        void shouldThrowExceptionForNonExistentIssueId() {
            // Arrange
            when(repository.findById(999L)).thenReturn(null);

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> issueService.getIssueById(999L));
        }
    }

    // ==================== SEARCH TESTS ====================

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should search with all filters")
        void shouldSearchWithAllFilters() {
            // Arrange
            Issue matchingIssue = new Issue("Matching issue title", "Description", reporter);
            when(repository.search("search", PriorityLevel.HIGH, IssueStatus.TODO))
                    .thenReturn(Collections.singletonList(matchingIssue));

            // Act
            List<IssueDTO> results = issueService.searchIssues("search", PriorityLevel.HIGH, IssueStatus.TODO);

            // Assert
            assertEquals(1, results.size());
            verify(repository).search("search", PriorityLevel.HIGH, IssueStatus.TODO);
        }

        @Test
        @DisplayName("Should search with partial filters")
        void shouldSearchWithPartialFilters() {
            // Arrange
            when(repository.search("keyword", PriorityLevel.LOW, null))
                    .thenReturn(Collections.emptyList());

            // Act
            List<IssueDTO> results = issueService.searchIssues("keyword", PriorityLevel.LOW, null);

            // Assert
            assertTrue(results.isEmpty());
        }
    }

    // ==================== DUPLICATE MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Duplicate Management Tests")
    class DuplicateManagementTests {

        @Test
        @DisplayName("Admin should mark issue as duplicate")
        void adminShouldMarkIssueAsDuplicate() {
            // Arrange
            Issue original = new Issue("Original issue title", "Description", reporter);
            Issue duplicate = new Issue("Duplicate issue title", "Description", reporter);

            when(repository.findById(1L)).thenReturn(duplicate);
            when(repository.findById(2L)).thenReturn(original);

            // Act
            issueService.processDuplicate(1L, 2L, admin);

            // Assert
            assertEquals(IssueStatus.CLOSED, duplicate.getStatus());
            verify(repository).save(duplicate);
        }

        @Test
        @DisplayName("Non-admin should not mark issue as duplicate")
        void nonAdminShouldNotMarkDuplicate() {
            // Act & Assert
            SecurityException exception = assertThrows(
                    SecurityException.class,
                    () -> issueService.processDuplicate(1L, 2L, reporter));
            assertEquals("Only administrators can mark issues as duplicate.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null admin")
        void shouldThrowExceptionForNullAdmin() {
            // Act & Assert
            assertThrows(
                    SecurityException.class,
                    () -> issueService.processDuplicate(1L, 2L, null));
        }

        @Test
        @DisplayName("Should throw exception for null IDs")
        void shouldThrowExceptionForNullIds() {
            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> issueService.processDuplicate(null, 2L, admin));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> issueService.processDuplicate(1L, null, admin));
        }

        @Test
        @DisplayName("Should throw exception for non-existent issues")
        void shouldThrowExceptionForNonExistentIssues() {
            // Arrange
            when(repository.findById(1L)).thenReturn(null);
            when(repository.findById(2L)).thenReturn(sampleIssue);

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> issueService.processDuplicate(1L, 2L, admin));
        }
    }

    // ==================== ATTACHMENT TESTS ====================

    @Nested
    @DisplayName("Attachment Tests")
    class AttachmentTests {

        @Test
        @DisplayName("Should set attachment path")
        void shouldSetAttachmentPath() {
            // Arrange
            when(repository.findById(1L)).thenReturn(sampleIssue);

            // Act
            issueService.setAttachmentPath(1L, "/uploads/1/file.png");

            // Assert
            assertEquals("/uploads/1/file.png", sampleIssue.getAttachmentPath());
            verify(repository).save(sampleIssue);
        }

        @Test
        @DisplayName("Should remove attachment and return old path")
        void shouldRemoveAttachment() {
            // Arrange
            sampleIssue.setAttachmentPath("/uploads/1/old-file.png");
            when(repository.findById(1L)).thenReturn(sampleIssue);

            // Act
            String oldPath = issueService.removeAttachment(1L);

            // Assert
            assertEquals("/uploads/1/old-file.png", oldPath);
            assertNull(sampleIssue.getAttachmentPath());
            verify(repository).save(sampleIssue);
        }

        @Test
        @DisplayName("Should throw exception when setting attachment for non-existent issue")
        void shouldThrowExceptionForNonExistentIssueAttachment() {
            // Arrange
            when(repository.findById(999L)).thenReturn(null);

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> issueService.setAttachmentPath(999L, "/path"));
        }
    }

    // ==================== DASHBOARD STATS TESTS ====================

    @Nested
    @DisplayName("Dashboard Stats Tests")
    class DashboardStatsTests {

        @Test
        @DisplayName("Should return dashboard statistics")
        void shouldReturnDashboardStats() {
            // Arrange
            when(repository.countAll()).thenReturn(100L);
            when(repository.countByStatus(IssueStatus.TODO)).thenReturn(30L);
            when(repository.countByStatus(IssueStatus.IN_PROGRESS)).thenReturn(20L);
            when(repository.countByStatus(IssueStatus.RESOLVED)).thenReturn(25L);
            when(repository.countByStatus(IssueStatus.CLOSED)).thenReturn(25L);
            when(repository.countDuplicates()).thenReturn(5L);
            when(repository.countByPriority(any())).thenReturn(25L);
            when(repository.getIssuesCreatedPerDaySince(any())).thenReturn(Collections.emptyList());
            when(repository.getAverageResolutionTimeHours()).thenReturn(48.0);
            when(repository.countCreatedToday()).thenReturn(3L);
            when(repository.countClosedToday()).thenReturn(2L);

            // Act
            DashboardStatsDTO stats = issueService.getDashboardStats();

            // Assert
            assertNotNull(stats);
            assertEquals(100L, stats.getTotalIssues());
            assertEquals(50L, stats.getOpenIssues()); // TODO + IN_PROGRESS
            assertEquals(25L, stats.getResolvedIssues());
            assertEquals(25L, stats.getClosedIssues());
            assertEquals(5L, stats.getDuplicateIssues());
            assertEquals(48.0, stats.getAvgResolutionTimeHours());
        }
    }
}
