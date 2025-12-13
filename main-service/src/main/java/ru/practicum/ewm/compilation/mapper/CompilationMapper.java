package ru.practicum.ewm.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

//    default Compilation toCompilation(NewCompilationDto newCompilationDto) {
//        Compilation entity = new Compilation();
//        entity.setPinned(newCompilationDto.getPinned() != null && newCompilationDto.getPinned());
//        entity.setTitle(newCompilationDto.getTitle());
//        return entity;
//    }

    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto newCompilationDto);

    CompilationDto toCompilationDto(Compilation compilation);

    List<CompilationDto> toCompilationDtoList(List<Compilation> compilationList);
}
