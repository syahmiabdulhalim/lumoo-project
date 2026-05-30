package com.example.lumoo.service;
import com.example.lumoo.domain.blog.BlogPost;
import com.example.lumoo.domain.blog.BlogPostRepository;
import com.example.lumoo.domain.blog.BlogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class BlogServiceTest {
    @Mock private BlogPostRepository repo;
    @InjectMocks private BlogService blogService;
    @Test
    void slugify_convertsToLowerKebabCase() {
        assertEquals("hello-world", blogService.slugify("Hello World"));
    }
    @Test
    void slugify_removesSpecialChars() {
        assertEquals("hello-world", blogService.slugify("Hello, World!"));
    }
    @Test
    void slugify_collapsesMultipleSpaces() {
        assertEquals("a-b", blogService.slugify("a   b"));
    }
    @Test
    void slugify_handlesNull() {
        assertEquals("", blogService.slugify(null));
    }
    @Test
    void slugify_handlesEmptyString() {
        assertEquals("", blogService.slugify(""));
    }
    @Test
    void save_setsPublishedAt_whenPublishedAndNotSet() {
        BlogPost post = post("slug-1", true, null);
        when(repo.findBySlugAndPublishedTrue("slug-1")).thenReturn(Optional.of(post));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BlogPost result = blogService.save(post);
        assertNotNull(result.getPublishedAt());
    }
    @Test
    void save_doesNotOverwritePublishedAt_whenAlreadySet() {
        LocalDateTime original = LocalDateTime.of(2025, 1, 1, 0, 0);
        BlogPost post = post("slug-2", true, original);
        when(repo.findBySlugAndPublishedTrue("slug-2")).thenReturn(Optional.of(post));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BlogPost result = blogService.save(post);
        assertEquals(original, result.getPublishedAt());
    }
    @Test
    void save_doesNotSetPublishedAt_whenDraft() {
        BlogPost post = post("slug-3", false, null);
        when(repo.findBySlugAndPublishedTrue("slug-3")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BlogPost result = blogService.save(post);
        assertNull(result.getPublishedAt());
    }
    @Test
    void save_setsDefaultAuthor_whenBlank() {
        BlogPost post = post("slug-4", false, null);
        post.setAuthor("");
        when(repo.findBySlugAndPublishedTrue(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BlogPost result = blogService.save(post);
        assertEquals("LUMOO Team", result.getAuthor());
    }
    @Test
    void save_convertsMarkdownToHtml() {
        BlogPost post = post("slug-5", false, null);
        post.setContent("**bold**");
        when(repo.findBySlugAndPublishedTrue(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BlogPost result = blogService.save(post);
        assertTrue(result.getContent().contains("<strong>bold</strong>"),
                "Expected markdown to be converted to HTML");
    }
    @Test
    void save_doesNotConvertHtmlContent() {
        BlogPost post = post("slug-6", false, null);
        post.setContent("<p>Already HTML</p>");
        when(repo.findBySlugAndPublishedTrue(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BlogPost result = blogService.save(post);
        assertEquals("<p>Already HTML</p>", result.getContent());
    }
    @Test
    void save_setsReadingTimeAtLeast1() {
        BlogPost post = post("slug-7", false, null);
        post.setContent("Short");
        when(repo.findBySlugAndPublishedTrue(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BlogPost result = blogService.save(post);
        assertTrue(result.getReadingTimeMinutes() >= 1);
    }
    @Test
    void save_estimatesLongerReadingTime_forManyWords() {
        BlogPost post = post("slug-8", false, null);
        post.setContent("<p>" + "word ".repeat(400) + "</p>");
        when(repo.findBySlugAndPublishedTrue(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BlogPost result = blogService.save(post);
        assertEquals(2, result.getReadingTimeMinutes());
    }
    @Test
    void delete_callsDeleteById() {
        blogService.delete(5L);
        verify(repo).deleteById(5L);
    }
    private BlogPost post(String slug, boolean published, LocalDateTime publishedAt) {
        BlogPost p = new BlogPost();
        p.setId(1L);
        p.setSlug(slug);
        p.setPublished(published);
        p.setPublishedAt(publishedAt);
        p.setAuthor("Test Author");
        p.setContent("Some content");
        return p;
    }
}
