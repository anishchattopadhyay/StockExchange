package com.iongroup.stockexchange;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

enum OrderType {
    BUY,
    SELL
}

class Stock {
    String name;
    int pricePerShare, qty;

    Stock(int pricePerShare, int noShare, String name) {
        this.pricePerShare = pricePerShare;
        this.qty = noShare;
        this.name = name;
    }

    synchronized boolean buy(int buyQty) {
        if(qty >= buyQty) {
            this.qty -= buyQty;
            return true;
        }
        return false;
    }

    synchronized void sell(int sellQty) {
        this.qty += sellQty;
    }
}

class Order implements Runnable {
    Stock stock;
    int qty, id;
    OrderType orderType;
    boolean executed = false;

    static Queue<Order> buyOrders = new LinkedList<>();
    static Queue<Order> sellOrders = new LinkedList<>();

    Random rand = new Random();

    public Order(Stock stock, int qty, OrderType orderType) {
        this.stock = stock;
        this.qty = qty;
        this.id = rand.nextInt(1000, 9999);
        this.orderType = orderType;
    }

    void placeOrder() {
        if(this.orderType == OrderType.BUY) {
            if(!stock.buy(this.qty)) {
                System.out.println("Cannot buy stock: Insufficient quantity");
                return;
            }
            buyOrders.offer(this);
        }
        if(this.orderType == OrderType.SELL) {
            stock.sell(this.qty);
            sellOrders.offer(this);
        }
    }

    void matchOrder() {
        // if the current order is BUY -> we will try to match it to a sell order
        if(this.orderType == OrderType.BUY) {
            for(Order sellOrder: sellOrders) {
                if(this.qty == sellOrder.qty && this.stock.pricePerShare == sellOrder.stock.pricePerShare) {
                    System.out.println("Matching order: " + this.id + " with " + sellOrder.id);
                    // match found -> both the orders are executed
                    this.executed = true;
                    sellOrder.executed = true;
                    break;
                }
            }
        }

        if(this.orderType == OrderType.SELL) {
            for(Order buyOrder: buyOrders) {
                if(this.qty == buyOrder.qty && this.stock.pricePerShare == buyOrder.stock.pricePerShare) {
                    System.out.println("Matching order: " + this.id + " with " + buyOrder.id);
                    // match found -> both the orders are executed
                    this.executed = true;
                    buyOrder.executed = true;
                    break;
                }
            }
        }
    }

    void checkIfExecuted() throws InterruptedException {
        System.out.println(this.id + " checking if order is executed");
        while(!this.executed) {
            Thread.sleep(500);
        }
        System.out.println(this.id + " is executed");
    }

    @Override
    public void run() {
        placeOrder();
        matchOrder();
        try {
            checkIfExecuted();
            System.out.println("Order "+this.id+" finished");
        } catch (Exception e) {}
    }
}

public class Main {
    public static void main(String[] args) {
        BlockingQueue<Runnable> orderQueue = new ArrayBlockingQueue<>(5);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 3000, TimeUnit.MICROSECONDS, orderQueue);

        executor.setRejectedExecutionHandler((Runnable r, ThreadPoolExecutor _) -> {
            System.out.println("Order Rejected : " + ((Order) r).id);
        });

        List<Order> ordersToExecute = getOrders();
        int i=0;
        while (i < ordersToExecute.size()) {
            if (executor.getQueue().remainingCapacity() == 0) {
                break;
            }
            // Adding threads one by one
            Order currentOrder = ordersToExecute.get(i);
            System.out.println("Creating an order: " + currentOrder.id);
            executor.execute(ordersToExecute.get(i));
            i++;
        }
    }

    private static List<Order> getOrders() {
        List<Stock> stocks = new ArrayList<>(Arrays.asList(
                new Stock(50, 3, "compA"),
                new Stock(30, 2, "compB"),
                new Stock(10, 1, "compC")
        ));

        return new ArrayList<>(Arrays.asList(
                new Order(stocks.get(1), 1, OrderType.BUY),
                new Order(stocks.get(0), 1, OrderType.BUY),

                new Order(stocks.get(1), 1, OrderType.SELL),
                new Order(stocks.get(0), 1, OrderType.SELL)
        ));
    }
}