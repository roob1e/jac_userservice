package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.dto.CardUpdateRequest;
import com.innowise.userservice.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;
import java.time.YearMonth;

@Mapper(componentModel = "spring")
public interface CardMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "active", ignore = true)
  Card toEntity(CardCreateRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "active", ignore = true)
  void updateEntity(CardUpdateRequest request, @MappingTarget Card card);

  @Mapping(target = "userId", source = "user.id")
  CardResponse toResponse(Card card);

  default LocalDate toLocalDate(YearMonth yearMonth) {
    return yearMonth == null ? null : yearMonth.atEndOfMonth();
  }

  default YearMonth toYearMonth(LocalDate date) {
    return date == null ? null : YearMonth.from(date);
  }
}
