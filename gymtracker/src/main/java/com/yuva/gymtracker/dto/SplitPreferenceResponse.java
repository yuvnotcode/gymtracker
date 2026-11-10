package com.yuva.gymtracker.dto;

import com.yuva.gymtracker.entity.SplitType;

import java.util.List;

public record SplitPreferenceResponse(SplitType splitType, List<String> customSplitDays) {
}
