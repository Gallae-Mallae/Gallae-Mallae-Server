package com.practice.OAuth2.domain.attraction.mapper;

import com.practice.OAuth2.domain.attraction.dto.AttractionResponse; // [중요] DTO 임포트
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface AttractionMapper {


    List<AttractionResponse> findAllAttractions();
}