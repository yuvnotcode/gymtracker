package com.yuva.gymtracker.dto;

import com.yuva.gymtracker.entity.SplitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SplitPreferenceRequest {
    @NotNull
    private SplitType splitType;

    @Size(max = 7)
    private List<@NotBlank @Size(max = 40) String> customSplitDays;
}
