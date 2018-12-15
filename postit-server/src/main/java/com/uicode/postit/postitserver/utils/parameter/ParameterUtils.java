package com.uicode.postit.postitserver.utils.parameter;

import java.util.Optional;

public class ParameterUtils {

    private ParameterUtils() {
    }

    public static Long getLong(Optional<String> parameterValue, Long defaultValue) {
        if (parameterValue.isPresent()) {
            return Long.valueOf(parameterValue.get());
        } else {
            return defaultValue;
        }
    }

}
