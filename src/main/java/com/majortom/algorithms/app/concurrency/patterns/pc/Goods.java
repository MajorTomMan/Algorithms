package com.majortom.algorithms.app.concurrency.patterns.pc;

public class Goods {
    private String name;
    private Integer price;
    private String from;
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getPrice() {
        return price;
    }
    public void setPrice(Integer price) {
        this.price = price;
    }
    @Override
    public String toString() {
        return "Goods [name=" + name + ", price=" + price + ", from=" + from + "]";
    }
}
