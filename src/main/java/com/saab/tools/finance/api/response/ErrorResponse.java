package com.saab.tools.finance.api.response;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ErrorResponse {

    private String errorMessage;

}
