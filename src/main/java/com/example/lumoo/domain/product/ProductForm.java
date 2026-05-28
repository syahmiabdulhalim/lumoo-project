package com.example.lumoo.domain.product;

public record ProductForm(
    String name,
    String category,
    Double price,
    String imageUrl
) {}