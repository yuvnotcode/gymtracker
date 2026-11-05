package com.yuva.gymtracker.dto;

import com.yuva.gymtracker.entity.SplitType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitPreferenceRequest {
    @NotNull
    private SplitType splitType;
}
