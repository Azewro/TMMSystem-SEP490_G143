package tmmsystem.config;

import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import tmmsystem.entity.User;
import tmmsystem.entity.Customer;
import tmmsystem.repository.UserRepository;
import tmmsystem.repository.CustomerRepository;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            // Chỉ thực thi đổi mật khẩu cho tất cả User/Customer thành "Abcd1234"
            resetAllUserPasswords();
            resetAllCustomerPasswords();
            // Giữ nguyên các chức năng khác ở dưới trong comment, KHÔNG thực thi:
            // createSampleMachines();
            // createSampleCategoriesAndProducts();
            // createSampleMaterials();
            // createDepartmentalRolesAndUsers();
        } catch (Exception ex) {
            log.error("DataInitializer encountered an error: {}", ex.getMessage(), ex);
        }
    }

    /** Đổi mật khẩu tất cả User thành "Abcd1234" */
    private void resetAllUserPasswords() {
        String raw = "Abcd1234";
        String encoded = passwordEncoder.encode(raw);
        List<User> users = userRepository.findAll();
        int count = 0;
        for (User u : users) {
            u.setPassword(encoded);
            userRepository.save(u); count++;
        }
        log.info("Đã đổi mật khẩu {} User thành '{}'", count, raw);
    }

    /** Đổi mật khẩu tất cả Customer thành "Abcd1234" */
    private void resetAllCustomerPasswords() {
        String raw = "Abcd1234";
        String encoded = passwordEncoder.encode(raw);
        List<Customer> customers = customerRepository.findAll();
        int count = 0;
        for (Customer c : customers) {
            c.setPassword(encoded);
            customerRepository.save(c); count++;
        }
        log.info("Đã đổi mật khẩu {} Customer thành '{}'", count, raw);
    }

    /*
    ============================
    LƯU CÁC HÀM KHỞI TẠO MẪU (KHÔNG THỰC THI) NGUYÊN VẸN TỪ BẢN CŨ
    Để dùng sau này, chỉ cần bỏ comment ở phần gọi trong run().
    ============================

    // @Autowired private MachineRepository machineRepository;
    // @Autowired private ProductRepository productRepository;
    // @Autowired private ProductCategoryRepository productCategoryRepository;
    // @Autowired private MaterialRepository materialRepository;
    // @Autowired private MaterialStockRepository materialStockRepository;
    // @Autowired private RoleRepository roleRepository;

    // private static final java.util.Random RNG = new java.util.Random();

    // private void createSampleMachines() { /* ... giữ nguyên nội dung như bản comment cũ ... */ /* }
    // private void createMachine(String code, String name, String type, String location, String specifications) { /* ... */ /* }
    // private void createSampleMaterials() { /* ... */ /* }
    // private void createMaterial(String code, String name, java.math.BigDecimal priceVndPerKg) { /* ... */ /* }
    // private void createSampleCategoriesAndProducts() { /* ... */ /* }
    // private tmmsystem.entity.ProductCategory createCategory(String name, String description) { /* ... */ /* }
    // private void createProduct(String code, String name, tmmsystem.entity.ProductCategory category, String dimensions, int weightGrams, java.math.BigDecimal price) { /* ... */ /* }
    // private java.math.BigDecimal bd(long vnd) { /* ... */ /* }

    // private void createDepartmentalRolesAndUsers() { /* ... */ /* }
    // private void createRoleIfMissing(String name, String description) { /* ... */ /* }
    // private void createUserIfMissing(String email, String name, String phone, String roleName) { /* ... */ /* }

    */
}
