package tmmsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tmmsystem.entity.Product;
import tmmsystem.entity.ProductCategory;
import tmmsystem.entity.User;
import tmmsystem.entity.Customer;
import tmmsystem.repository.MachineRepository;
import tmmsystem.repository.ProductRepository;
import tmmsystem.repository.ProductCategoryRepository;
import tmmsystem.repository.MaterialRepository;
import tmmsystem.repository.MaterialStockRepository;
import tmmsystem.repository.UserRepository;
import tmmsystem.repository.RoleRepository;
import tmmsystem.entity.Role;
import tmmsystem.repository.CustomerRepository;
import tmmsystem.entity.Material;
import tmmsystem.entity.MaterialStock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.math.BigDecimal;


@Component
public class DataInitializer implements CommandLineRunner {
	@Autowired
	private MachineRepository machineRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

	@Autowired
	private MaterialRepository materialRepository;

	@Autowired
	private MaterialStockRepository materialStockRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RoleRepository roleRepository;
    
	@Override
	public void run(String... args) throws Exception {
		// Chỉ tạo dữ liệu cơ bản, không xóa dữ liệu cũ
		// createSampleMachines();
        // createSampleCategoriesAndProducts();
		// createSampleMaterials();
		
		// Đổi mật khẩu tất cả user và customer thành "Abcd1234"
		resetAllUserPasswords();
		resetAllCustomerPasswords();
		createDepartmentalRolesAndUsers();
	}
	

	private static final Random RNG = new Random();

	private void createSampleMachines() {
		// 10 weaving machines (máy dệt)
		for (int i = 1; i <= 10; i++) {
			createMachine(
				String.format("WEAV-%03d", i),
				"Máy dệt " + i,
				"WEAVING",
				"Khu A-" + (i % 5 + 1),
				"{\"brand\":\"TMMS\",\"power\":\"5kW\",\"modelYear\":\"2022\",\"capacityPerDay\":50,\"capacityUnit\":\"KG\"}"
			);
		}
		// 2 warping machines (máy mắc)
		for (int i = 1; i <= 2; i++) {
			createMachine(
				String.format("WARP-%03d", i),
				"Máy mắc " + i,
				"WARPING",
				"Khu B-" + (i % 2 + 1),
				"{\"brand\":\"TMMS\",\"power\":\"5kW\",\"modelYear\":\"2022\",\"capacityPerDay\":200,\"capacityUnit\":\"KG\"}"
			);
		}
		// 5 cutting machines (máy cắt)
		for (int i = 1; i <= 5; i++) {
			createMachine(
				String.format("CUT-%03d", i),
				"Máy cắt " + i,
				"CUTTING",
				"Khu C-" + (i % 3 + 1),
				"{\"brand\":\"TMMS\",\"power\":\"3kW\",\"modelYear\":\"2022\",\"capacityPerHour\":{\"faceTowels\":150,\"bathTowels\":70,\"sportsTowels\":100},\"capacityUnit\":\"PIECES\"}"
			);
		}
		// 5 sewing machines (máy may)
		for (int i = 1; i <= 5; i++) {
			createMachine(
				String.format("SEW-%03d", i),
				"Máy may " + i,
				"SEWING",
				"Khu C-" + (i % 3 + 1),
				"{\"brand\":\"TMMS\",\"power\":\"5kW\",\"modelYear\":\"2022\",\"capacityPerHour\":{\"faceTowels\":150,\"bathTowels\":70,\"sportsTowels\":100},\"capacityUnit\":\"PIECES\"}"
			);
		}
		System.out.println("Sample machines created successfully!");
	}

	private void createMachine(String code, String name, String type, String location, String specifications) {
		tmmsystem.entity.Machine m = new tmmsystem.entity.Machine();
		m.setCode(code);
		m.setName(name);
		m.setType(type);
		m.setStatus("AVAILABLE");
		m.setLocation(location);
		m.setSpecifications(specifications);
		Instant now = Instant.now();
		m.setLastMaintenanceAt(now.minus(30 + RNG.nextInt(30), ChronoUnit.DAYS));
		m.setNextMaintenanceAt(now.plus(60 + RNG.nextInt(60), ChronoUnit.DAYS));
		m.setMaintenanceIntervalDays(90);
		machineRepository.save(m);
	}

	private void createSampleMaterials() {
		createMaterial("Ne 32/1CD", "sợi cotton", bd(68000));
		createMaterial("Ne 30/1", "sợi bamboo", bd(78155));
		System.out.println("Sample materials created successfully!");
	}

	private void createMaterial(String code, String name, BigDecimal priceVndPerKg) {
		Material m = new Material();
		m.setCode(code);
		m.setName(name);
		m.setType("YARN");
		m.setUnit("KG");
		m.setStandardCost(priceVndPerKg);
		m.setActive(true);
		Material savedMaterial = materialRepository.save(m);
		
		// Tạo nhiều batch với giá khác nhau để tính giá trung bình
		// Batch 1: Giá cao hơn
		MaterialStock stock1 = new MaterialStock();
		stock1.setMaterial(savedMaterial);
		stock1.setQuantity(new BigDecimal("500000"));
		stock1.setUnit("KG");
		stock1.setLocation("MAIN-WH");
		stock1.setBatchNumber("BATCH-" + code.replaceAll("\\s+", "-") + "-001");
		stock1.setUnitPrice(priceVndPerKg.multiply(new BigDecimal("1.05"))); // +5% so với giá chuẩn
		stock1.setReceivedDate(java.time.LocalDate.now().minusDays(10));
		materialStockRepository.save(stock1);

		// Batch 2: Giá thấp hơn
		MaterialStock stock2 = new MaterialStock();
		stock2.setMaterial(savedMaterial);
		stock2.setQuantity(new BigDecimal("499999"));
		stock2.setUnit("KG");
		stock2.setLocation("MAIN-WH");
		stock2.setBatchNumber("BATCH-" + code.replaceAll("\\s+", "-") + "-002");
		stock2.setUnitPrice(priceVndPerKg.multiply(new BigDecimal("0.95"))); // -5% so với giá chuẩn
		stock2.setReceivedDate(java.time.LocalDate.now().minusDays(5));
		materialStockRepository.save(stock2);
	}

    private void createSampleCategoriesAndProducts() {
        ProductCategory catMat = createCategory("khăn mặt", "Khăn dùng cho mặt");
        ProductCategory catTam = createCategory("khăn tắm", "Khăn tắm các kích thước");
        ProductCategory catTheThao = createCategory("khăn thể thao", "Khăn dùng cho thể thao");

        // Khăn mặt (category: khăn mặt, code prefix MAT)
        createProduct("MAT-001", "Khăn mặt màu cotton cuộn tròn", catMat, "30 x 50 cm", 60, bd(9500));
        createProduct("MAT-002", "Khăn mặt hoa bông mềm", catMat, "30 x 50 cm", 60, bd(13000));
        createProduct("MAT-003", "Khăn mặt hoa cotton", catMat, "30 x 50 cm", 60, bd(12000));
        createProduct("MAT-004", "Khăn mặt hoa cotton + bambo", catMat, "30 x 50 cm", 60, bd(12000));
        createProduct("MAT-005", "Khăn mặt hoa bambo", catMat, "30 x 50 cm", 60, bd(12000));
        createProduct("MAT-006", "Khăn mặt màu bambo", catMat, "30 x 50 cm", 60, bd(12000));
        createProduct("MAT-007", "Khăn mặt màu bông mềm", catMat, "30 x 50 cm", 60, bd(13000));

        // Khăn thể thao (category: khăn thể thao, code prefix SPT)
        createProduct("SPT-001", "Khăn thể thao màu cotton", catTheThao, "36 x 80 cm", 100, bd(17000));
        createProduct("SPT-002", "Khăn thể thao hoa bông mềm", catTheThao, "35 x 78 cm", 100, bd(20000));
        createProduct("SPT-003", "Khăn thể thao hoa bambo", catTheThao, "36 x 80 cm", 100, bd(18000));
        createProduct("SPT-004", "Khăn thể thao hoa cotton + bambo", catTheThao, "35 x 78 cm", 100, bd(18000));

        // Khăn tắm (category: khăn tắm, code prefix TAM)
        createProduct("TAM-001", "Khăn tắm màu trơn cotton", catTam, "50 x 100 cm", 220, bd(35000));
        createProduct("TAM-002", "Khăn tắm màu trơn bambo", catTam, "50 x 100 cm", 220, bd(35000));
        createProduct("TAM-003", "Khăn tắm hoa bông mềm", catTam, "50 x 100 cm", 220, bd(42000));
        createProduct("TAM-004", "Khăn tắm màu trơn cottton", catTam, "60 x 120 cm", 320, bd(50000));
        createProduct("TAM-005", "Khăn tắm màu trơn bambo", catTam, "60 x 120 cm", 320, bd(50000));
        createProduct("TAM-006", "Khăn tắm màu trơn cotton", catTam, "70 x 140 cm", 420, bd(65000));
        createProduct("TAM-007", "Khăn tắm màu trơn bambo", catTam, "70 x 140 cm", 420, bd(68000));
        createProduct("TAM-008", "Khăn tắm hoa bông mềm", catTam, "70 x 140 cm", 420, bd(80000));
        createProduct("TAM-009", "Khăn tắm màu bông mềm", catTam, "70 x 140 cm", 420, bd(75000));
        createProduct("TAM-010", "Khăn tắm hoa cotton", catTam, "70 x 140 cm", 420, bd(72000));
        createProduct("TAM-011", "Khăn tắm hoa cotton + bambo", catTam, "70 x 140 cm", 420, bd(72000));
        
        System.out.println("Sample categories and products created successfully!");
    }

    private ProductCategory createCategory(String name, String description) {
        ProductCategory c = new ProductCategory();
        c.setName(name);
        c.setDescription(description);
        c.setActive(true);
        return productCategoryRepository.save(c);
    }

    private void createProduct(String code, String name, ProductCategory category, String dimensions, int weightGrams, BigDecimal price) {
        Product p = new Product();
        p.setCode(code);
        p.setName(name);
        p.setCategory(category);
        p.setStandardDimensions(dimensions);
        p.setStandardWeight(new BigDecimal(weightGrams));
        p.setBasePrice(price);
        p.setUnit("CÁI");
        p.setActive(true);
        productRepository.save(p);
    }

    private BigDecimal bd(long vnd) {
        return new BigDecimal(Long.toString(vnd));
    }

    /**
     * Đổi mật khẩu tất cả User thành "Abcd1234"
     */
    private void resetAllUserPasswords() {
        String newPassword = "Abcd1234";
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        java.util.List<User> allUsers = userRepository.findAll();
        int updatedCount = 0;
        
        for (User user : allUsers) {
            user.setPassword(encodedPassword);
            userRepository.save(user);
            updatedCount++;
        }
        
        System.out.println("Đã đổi mật khẩu " + updatedCount + " User thành '" + newPassword + "'");
    }

    /**
     * Đổi mật khẩu tất cả Customer thành "Abcd1234"
     */
    private void resetAllCustomerPasswords() {
        String newPassword = "Abcd1234";
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        java.util.List<Customer> allCustomers = customerRepository.findAll();
        int updatedCount = 0;
        
        for (Customer customer : allCustomers) {
            customer.setPassword(encodedPassword);
            customerRepository.save(customer);
            updatedCount++;
        }
        System.out.println("Đã đổi mật khẩu " + updatedCount + " Customer thành '" + newPassword + "'");
    }

    /**
     * Tạo 4 role/phòng ban và mỗi role tạo 1 user mẫu nếu chưa tồn tại
     */
    private void createDepartmentalRolesAndUsers() {
        createRoleIfMissing("Production manager", "Production manager role");
        createRoleIfMissing("Quality Assurance department", "Quality Assurance department role");
        createRoleIfMissing("Product Process Leader", "Product Process Leader role");
        createRoleIfMissing("Technical Department", "Technical Department role");

        createUserIfMissing(
            "prod.manager@tmmsystem.local",
            "Production Manager",
            "+84000000001",
            "Production manager");

        createUserIfMissing(
            "qa.dept@tmmsystem.local",
            "QA Department",
            "+84000000002",
            "Quality Assurance department");

        createUserIfMissing(
            "process.leader@tmmsystem.local",
            "Product Process Leader",
            "+84000000003",
            "Product Process Leader");

        createUserIfMissing(
            "technical.dept@tmmsystem.local",
            "Technical Department",
            "+84000000004",
            "Technical Department");
    }

    private void createRoleIfMissing(String name, String description) {
        roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            r.setDescription(description);
            r.setActive(true);
            return roleRepository.save(r);
        });
    }

    private void createUserIfMissing(String email, String name, String phone, String roleName) {
        if (userRepository.findAll().stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()))) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPhoneNumber(phone);
        user.setActive(true);
        user.setVerified(true);
        user.setPassword(passwordEncoder.encode("Abcd1234"));
        Role role = roleRepository.findByName(roleName).orElseThrow();
        user.setRole(role);
        userRepository.save(user);
    }
}
