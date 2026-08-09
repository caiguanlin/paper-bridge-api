package com.paper.teacher.modules.curriculum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.modules.curriculum.dto.CurriculumResponse;
import com.paper.teacher.modules.curriculum.dto.CurriculumSearchRequest;
import com.paper.teacher.modules.curriculum.dto.CurriculumUpsertRequest;
import com.paper.teacher.modules.curriculum.entity.CurriculumNode;
import com.paper.teacher.modules.curriculum.repository.CurriculumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {
    @Mock
    private CurriculumRepository curriculumRepository;
    @InjectMocks
    private CurriculumService curriculumService;

    @Test
    void searchMapsNodesToResponses() {
        when(curriculumRepository.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(node(1L, "人教版", "CHINESE", "三年级", "上册", "第一单元", "第一课")));

        List<CurriculumResponse> responses =
                curriculumService.search(new CurriculumSearchRequest("人教版", "CHINESE", null, null));

        assertEquals(1, responses.size());
        assertEquals("第一课", responses.getFirst().chapter());
        assertEquals("第一单元", responses.getFirst().unit());
    }

    @Test
    void createDefaultsSortOrderAndMarksManualSource() {
        CurriculumResponse response = curriculumService.create(new CurriculumUpsertRequest(
                "人教版", "CHINESE", "三年级", "上册", "第一单元", "第一课", null, 2024, "https://example.com"));

        ArgumentCaptor<CurriculumNode> captor = ArgumentCaptor.forClass(CurriculumNode.class);
        verify(curriculumRepository).insert(captor.capture());
        assertEquals(0, captor.getValue().getSortOrder());
        assertEquals("MANUAL", captor.getValue().getSourceCode());
        assertEquals(2024, captor.getValue().getEditionYear());
        assertEquals("第一课", response.chapter());
    }

    @Test
    void updateAppliesRequestToExistingNode() {
        CurriculumNode existing = node(5L, "人教版", "CHINESE", "三年级", "上册", "第一单元", "第一课");
        when(curriculumRepository.selectById(5L)).thenReturn(existing);

        CurriculumResponse response = curriculumService.update(5L, new CurriculumUpsertRequest(
                "北师大版", "MATH", "四年级", "下册", "第二单元", "第三课", 9, null, null));

        verify(curriculumRepository).updateById(existing);
        assertEquals("北师大版", response.publisher());
        assertEquals("第三课", response.chapter());
        assertEquals(9, response.sortOrder());
    }

    @Test
    void updateRejectsUnknownNode() {
        when(curriculumRepository.selectById(5L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> curriculumService.update(5L,
                new CurriculumUpsertRequest("人教版", "CHINESE", "三年级", "上册", "单元", "章节", 1, null, null)));

        assertEquals("教材目录不存在", exception.getMessage());
        verify(curriculumRepository, never()).updateById(any(CurriculumNode.class));
    }

    @Test
    void deleteRejectsUnknownNode() {
        when(curriculumRepository.deleteById(eq(5L))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> curriculumService.delete(5L));

        assertEquals("教材目录不存在", exception.getMessage());
    }

    @Test
    void treeGroupsNodesByPublisherSubjectGradeVolumeUnit() {
        when(curriculumRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                node(1L, "人教版", "CHINESE", "三年级", "上册", "第一单元", "第一课"),
                node(2L, "人教版", "CHINESE", "三年级", "上册", "第一单元", "第二课"),
                node(3L, "人教版", "MATH", "三年级", "上册", "第一单元", "认识时间"),
                node(4L, "北师大版", "SCIENCE", "四年级", "下册", "第二单元", "植物")));

        List<CurriculumService.CurriculumTreeNode> roots = curriculumService.tree();

        assertEquals(2, roots.size());
        CurriculumService.CurriculumTreeNode renjiao = roots.getFirst();
        assertEquals("publisher", renjiao.type());
        assertEquals("人教版", renjiao.label());
        assertEquals(2, renjiao.children().size());
        CurriculumService.CurriculumTreeNode chinese = renjiao.children().getFirst();
        assertEquals("语文", chinese.label());
        assertEquals("CHINESE", chinese.value());
        CurriculumService.CurriculumTreeNode unit = chinese.children().getFirst()
                .children().getFirst()
                .children().getFirst();
        assertEquals("unit", unit.type());
        assertEquals(2, unit.children().size());
        assertEquals("chapter", unit.children().getFirst().type());
        assertEquals(1L, unit.children().getFirst().id());
        assertEquals("数学", renjiao.children().get(1).label());
        assertEquals("科学", roots.get(1).children().getFirst().label());
    }

    @Test
    void treeFallsBackToRawSubjectWhenUnmapped() {
        when(curriculumRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                node(1L, "人教版", "MUSIC", "三年级", "上册", "第一单元", "第一课")));

        CurriculumService.CurriculumTreeNode subject = curriculumService.tree().getFirst().children().getFirst();

        assertEquals("MUSIC", subject.label());
    }

    private static CurriculumNode node(
            Long id, String publisher, String subject, String grade, String volume, String unit, String chapter) {
        CurriculumNode node = new CurriculumNode();
        node.setId(id);
        node.setPublisher(publisher);
        node.setSubject(subject);
        node.setGrade(grade);
        node.setVolume(volume);
        node.setUnit(unit);
        node.setChapter(chapter);
        node.setSortOrder(0);
        return node;
    }
}
