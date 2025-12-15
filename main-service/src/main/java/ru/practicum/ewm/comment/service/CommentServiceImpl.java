package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // Приватные: добавление пользователем комментария
    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        Comment commentToSave = commentMapper.toComment(newCommentDto);
        commentToSave.setCreated(LocalDateTime.now());
        commentToSave.setCommentator(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден по id = " + userId)));
        commentToSave.setEvent(eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено по id = " + eventId)));

        commentRepository.save(commentToSave);
        log.info("Комментарий успешно добавлен: " + commentToSave);
        return commentMapper.toCommentDto(commentToSave);
    }

    // Приватные: удаление пользователем комментария
    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден по id = " + commentId));

        if (!userId.equals(comment.getCommentator().getId())) {
            throw new ValidationException("Пользователь не является комментатором, id =  " + userId);
        }
        commentRepository.deleteById(commentId);
        log.info("Комментарий с id = {} успешно удалён", commentId);
    }

    // Приватные: Изменение пользователем комментария
    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto newCommentDto) {
        Comment commentToUpdate = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден по id = " + commentId));

        if (!userId.equals(commentToUpdate.getCommentator().getId())) {
            throw new ValidationException("Пользователь не является комментатором, id =  " + userId);
        }

        // Если есть лишние пробелы, то удаляем их
        commentToUpdate.setCommentText(newCommentDto.getCommentText().trim());
        commentRepository.save(commentToUpdate);
        log.info("Комментарий успешно изменён: " + commentToUpdate);
        return commentMapper.toCommentDto(commentToUpdate);
    }

    // Публичные: получение комментариев к событию
    @Override
    public List<CommentDto> getAllCommentsToEvent(Long eventId, Pageable pageable) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие не найдено по id = " + eventId);
        }

        List<Comment> commentList = commentRepository.findAllByEvent_IdOrderByCreatedDesc(eventId, pageable);
        List<CommentDto> commentDtoList = new ArrayList<>();
        for (Comment comment : commentList) {
            commentDtoList.add(commentMapper.toCommentDto(comment));
        }
        log.info("К событию с id = {} получено {} комментариев", eventId, commentDtoList.size());
        return commentDtoList;
    }

    // Администрирование: удаление администратором комментария
    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий не найден по id = " + commentId);
        }
        commentRepository.deleteById(commentId);
        log.info("Комментарий с id = {} успешно удалён", commentId);
    }
}
