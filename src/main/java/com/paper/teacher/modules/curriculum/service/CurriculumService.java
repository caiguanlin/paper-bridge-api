package com.paper.teacher.modules.curriculum.service;

import com.paper.teacher.modules.curriculum.repository.CurriculumRepository;

import com.paper.teacher.modules.curriculum.entity.CurriculumNode;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.teacher.common.Entities;
import com.paper.teacher.modules.curriculum.dto.CurriculumResponse;
import com.paper.teacher.modules.curriculum.dto.CurriculumSearchRequest;
import com.paper.teacher.modules.curriculum.dto.CurriculumUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurriculumService {
    private final CurriculumRepository curriculumRepository;

    public List<CurriculumResponse> search(CurriculumSearchRequest request) {
        return curriculumRepository.selectList(baseQuery(request)).stream()
                .map(CurriculumResponse::from)
                .toList();
    }

    @Transactional
    public CurriculumResponse create(CurriculumUpsertRequest request) {
        CurriculumNode node = new CurriculumNode();
        apply(node, request);
        curriculumRepository.insert(node);
        return CurriculumResponse.from(node);
    }

    @Transactional
    public CurriculumResponse update(Long id, CurriculumUpsertRequest request) {
        CurriculumNode node = Entities.require(curriculumRepository.selectById(id), "教材目录不存在");
        apply(node, request);
        curriculumRepository.updateById(node);
        return CurriculumResponse.from(node);
    }

    @Transactional
    public void delete(Long id) {
        Entities.requireAffected(curriculumRepository.deleteById(id), "教材目录不存在");
    }

    public List<CurriculumTreeNode> tree() {
        List<CurriculumNode> nodes = curriculumRepository.selectList(baseQuery(new CurriculumSearchRequest(null, null, null, null)));
        CurriculumTreeBuilder builder = new CurriculumTreeBuilder();
        for (CurriculumNode node : nodes) {
            builder.add(node);
        }
        return builder.roots();
    }

    private LambdaQueryWrapper<CurriculumNode> baseQuery(CurriculumSearchRequest request) {
        return new LambdaQueryWrapper<CurriculumNode>()
                .eq(StrUtil.isNotBlank(request.publisher()), CurriculumNode::getPublisher, request.publisher())
                .eq(StrUtil.isNotBlank(request.subject()), CurriculumNode::getSubject, request.subject())
                .eq(StrUtil.isNotBlank(request.grade()), CurriculumNode::getGrade, request.grade())
                .eq(StrUtil.isNotBlank(request.volume()), CurriculumNode::getVolume, request.volume())
                .orderByAsc(CurriculumNode::getSortOrder)
                .orderByAsc(CurriculumNode::getId);
    }

    private void apply(CurriculumNode node, CurriculumUpsertRequest request) {
        node.setPublisher(request.publisher());
        node.setSubject(request.subject());
        node.setGrade(request.grade());
        node.setVolume(request.volume());
        node.setUnit(request.unit());
        node.setChapter(request.chapter());
        node.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        node.setEditionYear(request.editionYear());
        node.setSourceUrl(request.sourceUrl());
        node.setSourceCode("MANUAL");
    }

    private static final class CurriculumTreeBuilder {
        private final Map<String, CurriculumTreeNode> roots = new LinkedHashMap<>();

        private void add(CurriculumNode node) {
            CurriculumTreeNode publisher = node(roots, "publisher", node.getPublisher(), null);
            CurriculumTreeNode subject = publisher.child("subject", subjectLabel(node.getSubject()), node.getSubject(), null);
            CurriculumTreeNode grade = subject.child("grade", node.getGrade(), null);
            CurriculumTreeNode volume = grade.child("volume", node.getVolume(), null);
            CurriculumTreeNode unit = volume.child("unit", node.getUnit(), null);
            unit.child("chapter", node.getChapter(), node.getChapter(), node.getId());
        }

        private List<CurriculumTreeNode> roots() {
            return new ArrayList<>(roots.values());
        }

        private CurriculumTreeNode node(
                Map<String, CurriculumTreeNode> nodes,
                String type,
                String label,
                Long id
        ) {
            String key = type + ":" + label;
            return nodes.computeIfAbsent(key, ignored -> new CurriculumTreeNode(label, label, type, id, new ArrayList<>()));
        }
    }

    public record CurriculumTreeNode(
            String label,
            String value,
            String type,
            Long id,
            List<CurriculumTreeNode> children
    ) {
        private CurriculumTreeNode child(String type, String label, Long id) {
            return child(type, label, label, id);
        }

        private CurriculumTreeNode child(String type, String label, String value, Long id) {
            return children.stream()
                    .filter(child -> child.type.equals(type) && child.value.equals(value))
                    .findFirst()
                    .orElseGet(() -> addChild(type, label, value, id));
        }

        private CurriculumTreeNode addChild(String type, String label, String value, Long id) {
            CurriculumTreeNode child = new CurriculumTreeNode(label, value, type, id, new ArrayList<>());
            children.add(child);
            return child;
        }
    }

    private static String subjectLabel(String subject) {
        return switch (subject) {
            case "CHINESE" -> "语文";
            case "MATH" -> "数学";
            case "ENGLISH" -> "英语";
            case "SCIENCE" -> "科学";
            case "MORALITY" -> "道德与法治";
            default -> subject;
        };
    }
}
