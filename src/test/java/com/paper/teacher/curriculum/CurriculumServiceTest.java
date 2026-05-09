package com.paper.teacher.curriculum;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.curriculum.dto.CurriculumSearchRequest;
import com.paper.teacher.curriculum.dto.CurriculumUpsertRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurriculumServiceTest {
    private final CurriculumRepository repository = mock(CurriculumRepository.class);
    private final CurriculumService service = new CurriculumService(repository);

    @Test
    void treeBuildsPublisherSubjectGradeVolumeUnitChapterHierarchy() {
        CurriculumNode node = node(1L, "人民教育出版社", "MATH", "三年级", "上册", "教材目录", "测量");
        when(repository.selectList(any(Wrapper.class))).thenReturn(List.of(node));

        List<CurriculumService.CurriculumTreeNode> tree = service.tree();

        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().label()).isEqualTo("人民教育出版社");
        CurriculumService.CurriculumTreeNode subject = tree.getFirst().children().getFirst();
        assertThat(subject.label()).isEqualTo("数学");
        assertThat(subject.value()).isEqualTo("MATH");
        CurriculumService.CurriculumTreeNode chapter = subject.children().getFirst()
                .children().getFirst()
                .children().getFirst()
                .children().getFirst();
        assertThat(chapter.type()).isEqualTo("chapter");
        assertThat(chapter.id()).isEqualTo(1L);
        assertThat(chapter.label()).isEqualTo("测量");
    }

    @Test
    void searchMapsNodesToResponses() {
        when(repository.selectList(any(Wrapper.class))).thenReturn(List.of(
                node(1L, "人民教育出版社", "MATH", "三年级", "上册", "教材目录", "测量")
        ));

        var responses = service.search(new CurriculumSearchRequest("人民教育出版社", "MATH", null, null));

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.chapter()).isEqualTo("测量");
        });
    }

    @Test
    void updateRejectsMissingNode() {
        when(repository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(99L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("教材目录不存在");
    }

    @Test
    void deleteRejectsMissingNode() {
        when(repository.deleteById(99L)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("教材目录不存在");
    }

    @Test
    void createMarksManualSource() {
        service.create(request());

        verify(repository).insert(any(CurriculumNode.class));
    }

    private CurriculumUpsertRequest request() {
        return new CurriculumUpsertRequest(
                "人民教育出版社",
                "MATH",
                "三年级",
                "上册",
                "教材目录",
                "测量",
                1,
                2026,
                "https://jc.pep.com.cn/"
        );
    }

    private CurriculumNode node(
            Long id,
            String publisher,
            String subject,
            String grade,
            String volume,
            String unit,
            String chapter
    ) {
        CurriculumNode node = new CurriculumNode();
        node.setId(id);
        node.setPublisher(publisher);
        node.setSubject(subject);
        node.setGrade(grade);
        node.setVolume(volume);
        node.setUnit(unit);
        node.setChapter(chapter);
        node.setSortOrder(1);
        node.setEditionYear(2026);
        node.setSourceUrl("https://jc.pep.com.cn/");
        return node;
    }
}
