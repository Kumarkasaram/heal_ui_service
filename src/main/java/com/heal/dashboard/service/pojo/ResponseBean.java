package com.heal.dashboard.service.pojo;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@JsonPropertyOrder({"data","message","status"})
@ApiModel
@AllArgsConstructor
@Getter
@Setter
public class ResponseBean<T> {

    @ApiModelProperty(dataType = "string" )
    private String message;
    @ApiModelProperty(dataType = "T")
    private T data;
    @ApiModelProperty(dataType = "HttpStatus")
    private HttpStatus status;
}
