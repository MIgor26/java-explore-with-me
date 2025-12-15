package ru.practicum.ewm.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.service.CommentService;

@RestController
@RequestMapping(path = "/users/{userId}/comments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateCommentController {
    private final CommentService commentService;

    @Operation(summary = "Добавление пользователем комментария")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable @Positive Long userId,
                                 @RequestParam @Positive Long eventId,
                                 @Valid @RequestBody NewCommentDto newCommentDto) {
        log.info("POST in PrivateCommentController. Добавление пользователем id = {} комментария {} к событию id = {} ",
                userId, newCommentDto, eventId);
        return commentService.addComment(userId, eventId, newCommentDto);
    }


    @Operation(summary = "Удаление пользователем комментария")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @Positive Long userId,
                              @PathVariable Long commentId) {
        log.info("DELETE/id in PrivateCommentController. Удаление пользователем id = {} комментария id = {}",
                userId, commentId);
        commentService.deleteComment(userId, commentId);
    }

    @Operation(summary = "Изменение пользователем комментария")// изменение комментария
    @PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto updateComment(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long commentId,
                                    @Valid @RequestBody NewCommentDto newCommentDto) {
        log.info("PATH in PrivateCommentController. Изменение пользователем id = {} комментария id = {} ",
                userId, commentId);
        return commentService.updateComment(userId, commentId, newCommentDto);
    }
}
