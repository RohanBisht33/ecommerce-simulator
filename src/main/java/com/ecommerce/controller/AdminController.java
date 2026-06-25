package com.ecommerce.controller;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public AdminController(ProductRepository productRepository, OrderRepository orderRepository){
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/orders")
    public String showAdminOrders(Model model){

        List<Order> allOrders = orderRepository.findAll();
        model.addAttribute("orders", allOrders);

        return "admin-orders";
    }

    @GetMapping("/products")
    public String showAdminProducts(Model model){
        List<Product> allProducts = productRepository.findAll();

        model.addAttribute("products", allProducts);

        return "admin-products";
    }

    @PostMapping("/confirm/{id}")
    public String confirmOrder(@PathVariable Long id){
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return "redirect:/admin/orders";
    }

    @PostMapping("/products/add")
    public String saveProduct(Product product){
        productRepository.save(product);

        return "redirect:/admin/products";
    }

    @PostMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id, Product product){
        product.setId(id);
        productRepository.save(product);

        return "redirect:/admin/products";
    }
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id){
        productRepository.deleteById(id);

        return "redirect:/admin/products";
    }
}
