package com.example.lumoo.dto;

public record ProductForm(
    String name,
    String category,
    Double price,
    String imageUrl
) {}