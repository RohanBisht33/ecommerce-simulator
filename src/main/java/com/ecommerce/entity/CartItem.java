package com.ecommerce.entity;

public class CartItem {

    private Long id;
    private Product product;
    private Integer quantity;

    CartItem(){
    }

    public CartItem(Long id, Product product, Integer quantity){
        this.id = id;
        this.product = product;
        this.quantity = quantity;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

}
