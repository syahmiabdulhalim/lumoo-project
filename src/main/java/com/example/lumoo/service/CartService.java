package com.example.lumoo.service;

import com.example.lumoo.model.CartItem;
import com.example.lumoo.model.Product;
import com.example.lumoo.model.User;
import com.example.lumoo.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    @Autowired private CartRepository cartRepository;

    public List<CartItem> getItems(User user) {
        return cartRepository.findByUser(user);
    }

    public double getTotal(List<CartItem> items) {
        return items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
    }

    public void addItem(User user, Product product) {
        CartItem item = cartRepository.findByUserAndProduct(user, product).orElse(null);
        if (item != null) {
            item.setQuantity(item.getQuantity() + 1);
        } else {
            item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setQuantity(1);
            item.setName(product.getName());
            item.setPrice(product.getPrice());
        }
        cartRepository.save(item);
    }

    public boolean removeItem(Long cartItemId, User user) {
        CartItem item = cartRepository.findById(cartItemId).orElse(null);
        if (item == null) return false;
        if (!item.getUser().getId().equals(user.getId())) return false;
        cartRepository.delete(item);
        return true;
    }
}
