package ru.practicum.ewm.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.model.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "commentator.id", target = "commentator")
    CommentDto toCommentDto(Comment comment);

    Comment toComment(NewCommentDto newCommentDto);
}
