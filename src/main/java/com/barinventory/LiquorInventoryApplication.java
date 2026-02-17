package com.barinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class LiquorInventoryApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LiquorInventoryApplication.class, args);
    }
}
