package com.tuckshop.pos.config;

import com.tuckshop.pos.model.AppUser;
import com.tuckshop.pos.model.Customer;
import com.tuckshop.pos.model.Product;
import com.tuckshop.pos.repository.CustomerRepository;
import com.tuckshop.pos.repository.ProductRepository;
import com.tuckshop.pos.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ProductRepository productRepository, UserRepository userRepository,
                            CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedProducts();
        seedCustomers();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        AppUser owner = new AppUser();
        owner.setUsername("owner");
        owner.setFullName("Shop Owner");
        owner.setPasswordHash(passwordEncoder.encode("owner123"));
        owner.setRole("OWNER");
        owner.setPinHash(passwordEncoder.encode("1234"));
        owner.setActive(true);
        userRepository.save(owner);

        AppUser cashier = new AppUser();
        cashier.setUsername("cashier");
        cashier.setFullName("Counter Staff");
        cashier.setPasswordHash(passwordEncoder.encode("cashier123"));
        cashier.setRole("CASHIER");
        cashier.setActive(true);
        userRepository.save(cashier);

        System.out.println("=================================================");
        System.out.println(" Default logins created - CHANGE THESE PASSWORDS:");
        System.out.println("   Owner   -> username: owner    password: owner123   PIN: 1234");
        System.out.println("   Cashier -> username: cashier  password: cashier123");
        System.out.println("=================================================");
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        addProduct("8964000000011", "Mineral water 1.5L", "Drinks", "bottle", 40, 60, 80, 15);
        addProduct("8964000000028", "Cold drink 250ml", "Drinks", "bottle", 35, 55, 60, 12);
        addProduct("8964000000035", "Lays chips 30g", "Snacks", "pack", 25, 40, 4, 10);
        addProduct("8964000000042", "Biscuits assorted", "Snacks", "pack", 30, 50, 45, 10);
        addProduct("8964000000059", "Tea cup", "Beverages", "cup", 15, 30, 200, 30);
        addProduct("8964000000066", "Coke 500ml", "Drinks", "bottle", 55, 80, 6, 12);
        addProduct("8964000000073", "Cigarettes pack", "Tobacco", "pack", 180, 220, 11, 15);
        addProduct("8964000000080", "Engine oil 1L sachet", "Automotive", "sachet", 550, 650, 9, 10);
        addProduct("8964000000097", "Matchbox", "Essentials", "box", 5, 10, 120, 20);
        addProduct("8964000000103", "Chewing gum", "Snacks", "pack", 10, 20, 90, 15);
        addProduct("8964000000110", "Energy drink 250ml", "Drinks", "can", 90, 130, 30, 10);
        addProduct("8964000000127", "Instant noodles", "Snacks", "pack", 35, 55, 50, 10);
        addProduct("8964000000134", "Sunglasses (roadside)", "Accessories", "pcs", 150, 300, 8, 5);
        addProduct("8964000000141", "Phone charger cable", "Accessories", "pcs", 120, 200, 14, 5);
        addProduct("8964000000158", "Car air freshener", "Automotive", "pcs", 60, 100, 20, 8);

        System.out.println("Sample products loaded: 15 items.");
    }

    private void seedCustomers() {
        if (customerRepository.count() > 0) {
            return;
        }

        addCustomer("Malik Sahab (truck driver)", "0300-1234567", 5000, 1200);
        addCustomer("Rasheed Transport", "0321-9876543", 10000, 0);
        addCustomer("Naveed (pump staff)", "0333-4567890", 3000, 850);

        System.out.println("Sample khata customers loaded: 3.");
    }

    private void addProduct(String barcode, String name, String category, String unit,
                             double cost, double price, int qty, int threshold) {
        Product p = new Product();
        p.setBarcode(barcode);
        p.setName(name);
        p.setCategory(category);
        p.setUnit(unit);
        p.setCostPrice(BigDecimal.valueOf(cost));
        p.setSellingPrice(BigDecimal.valueOf(price));
        p.setQuantity(qty);
        p.setLowStockThreshold(threshold);
        productRepository.save(p);
    }

    private void addCustomer(String name, String phone, double creditLimit, double currentBalance) {
        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setCreditLimit(BigDecimal.valueOf(creditLimit));
        c.setCurrentBalance(BigDecimal.valueOf(currentBalance));
        customerRepository.save(c);
    }
}
