package com.dgkt.quiz;

import java.util.List;

public record Question(String id, String topic, String text, List<String> options, int correctIndex, String difficulty) {}
