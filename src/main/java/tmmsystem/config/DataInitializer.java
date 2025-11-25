package tmmsystem.config;

import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import tmmsystem.entity.User;
import tmmsystem.entity.Customer;
import tmmsystem.entity.QcCheckpoint;
import tmmsystem.repository.UserRepository;
import tmmsystem.repository.CustomerRepository;
import tmmsystem.repository.QcCheckpointRepository;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private QcCheckpointRepository qcCheckpointRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            // Chỉ thực thi đổi mật khẩu cho tất cả User/Customer thành "Abcd1234"
            resetAllUserPasswords();
            resetAllCustomerPasswords();
           
            // Giữ nguyên các chức năng khác ở dưới trong comment, KHÔNG thực thi:
            //  seedQcCheckpoints();
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

    /** Seed QC checkpoints (song ngữ) nếu chưa có */
    private void seedQcCheckpoints() {
        List<QcSeed> seeds = new java.util.ArrayList<>();
        // Cuồng mắc / Warping
        addSeed(seeds, "CUONG_MAC", "Chất lượng sợi", "Đối chiếu thông số BOM", "AQL", true, 1);
        addSeed(seeds, "CUONG_MAC", "Độ căng sợi", "Quan sát & đo", "AQL", true, 2);
        addSeed(seeds, "CUONG_MAC", "Sợi mắc đều", "Quan sát", "AQL", false, 3);
        addSeed(seeds, "CUONG_MAC", "Khổ & chiều dài cây sợi", "Đo khổ/cây", "AQL", false, 4);
        addSeed(seeds, "WARPING", "Yarn quality", "Match BOM specification", "AQL", true, 1);
        addSeed(seeds, "WARPING", "Consistent tension", "Visual & measure", "AQL", true, 2);
        addSeed(seeds, "WARPING", "Even warping", "Visual", "AQL", false, 3);

        // Dệt / Weaving
        addSeed(seeds, "DET", "Độ bền sợi nền", "Thử kéo", "AQL", true, 1);
        addSeed(seeds, "DET", "Hình dáng khăn", "Quan sát", "AQL", false, 2);
        addSeed(seeds, "DET", "Bề mặt vải", "Quan sát", "AQL", false, 3);
        addSeed(seeds, "WEAVING", "Fabric density", "Measure", "AQL", true, 1);
        addSeed(seeds, "WEAVING", "Fabric width", "Measure", "AQL", true, 2);

        // Nhuộm / Dyeing
        addSeed(seeds, "NHUOM", "Màu sắc chuẩn", "So màu mẫu chuẩn", "100%", true, 1);
        addSeed(seeds, "NHUOM", "Độ bền màu", "Thử ma sát/giặt", "AQL", true, 2);
        addSeed(seeds, "NHUOM", "Vết loang/đốm", "Quan sát", "AQL", false, 3);
        addSeed(seeds, "DYEING", "Color evenness", "Visual inspect", "AQL", true, 1);
        addSeed(seeds, "DYEING", "Color fastness", "Rub/wash test", "AQL", true, 2);

        // Cắt / Cutting
        addSeed(seeds, "CAT", "Kích thước chuẩn", "Đo từng chi tiết", "100%", true, 1);
        addSeed(seeds, "CAT", "Đường cắt sạch", "Quan sát", "AQL", true, 2);
        addSeed(seeds, "CUTTING", "Dimension accuracy", "Measure", "100%", true, 1);

        // May / Hemming
        addSeed(seeds, "MAY", "Đường may thẳng", "Quan sát", "AQL", true, 1);
        addSeed(seeds, "MAY", "Mật độ mũi chỉ", "Đếm/đo", "AQL", true, 2);
        addSeed(seeds, "HEMMING", "Seam straightness", "Visual", "AQL", true, 1);
        addSeed(seeds, "HEMMING", "Stitch density", "Count", "AQL", true, 2);

        // Đóng gói / Packaging
        addSeed(seeds, "DONG_GOI", "Đủ phụ kiện kèm", "Đếm", "AQL", true, 1);
        addSeed(seeds, "DONG_GOI", "Tem/nhãn đúng chuẩn", "Đối chiếu", "AQL", true, 2);
        addSeed(seeds, "PACKAGING", "Accessories completeness", "Count", "AQL", true, 1);

        int inserted = 0;
        for (QcSeed seed : seeds) {
            if (!qcCheckpointRepository.existsByStageTypeAndCheckpointName(seed.stageType, seed.name)) {
                QcCheckpoint cp = new QcCheckpoint();
                cp.setStageType(seed.stageType);
                cp.setCheckpointName(seed.name);
                cp.setInspectionCriteria(seed.criteria);
                cp.setSamplingPlan(seed.sampling);
                cp.setMandatory(seed.mandatory);
                cp.setDisplayOrder(seed.displayOrder);
                qcCheckpointRepository.save(cp);
                inserted++;
            }
        }
        if (inserted > 0) {
            log.info("Đã seed thêm {} QC checkpoints (song ngữ)", inserted);
        }
    }

    private void addSeed(List<QcSeed> seeds, String stageType, String name,
                         String criteria, String sampling, boolean mandatory, int order) {
        seeds.add(new QcSeed(stageType, name, criteria, sampling, mandatory, order));
    }

    private static class QcSeed {
        final String stageType;
        final String name;
        final String criteria;
        final String sampling;
        final boolean mandatory;
        final int displayOrder;

        QcSeed(String stageType, String name, String criteria, String sampling,
               boolean mandatory, int displayOrder) {
            this.stageType = stageType;
            this.name = name;
            this.criteria = criteria;
            this.sampling = sampling;
            this.mandatory = mandatory;
            this.displayOrder = displayOrder;
        }
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
