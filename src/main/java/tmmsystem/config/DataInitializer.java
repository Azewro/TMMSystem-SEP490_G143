package tmmsystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private QcCheckpointRepository qcCheckpointRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private tmmsystem.service.RfqService rfqService;
    @Autowired
    private tmmsystem.service.ProductionService productionService;

    @Override
    public void run(String... args) {
        try {
            // Chỉ thực thi đổi mật khẩu cho tất cả User/Customer thành "Abcd1234"
            // resetAllUserPasswords();
            // resetAllCustomerPasswords();

            // Fix RFQ status on startup
            int fixedRfqs = rfqService.scanAndFixDraftRfqs();
            if (fixedRfqs > 0) {
                log.info("Startup: Auto-corrected {} RFQs from DRAFT to SENT (assigned sales found)", fixedRfqs);
            }

            // Fix Production Data Consistency (Missing Stages)
            productionService.fixDataConsistency();
            productionService.fixDataConsistency();
            log.info("Startup: Verified and fixed Production Data consistency (Stages/QR Tokens)");

            // Fix Stage Tracking Data (Rework Flag)
            productionService.migrateStageTrackingData();
            log.info("Startup: Migrated Stage Tracking data for Rework detection");

            // Fix Missing Rework Details (Supplementary Orders)
            productionService.fixMissingReworkDetails();
            log.info("Startup: Fixed missing details for Supplementary Orders");
            // Giữ nguyên các chức năng khác ở dưới trong comment, KHÔNG thực thi:
            // Seed QC Checkpoints (Updated)
            // seedQcCheckpoints();
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
            userRepository.save(u);
            count++;
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
            customerRepository.save(c);
            count++;
        }
        log.info("Đã đổi mật khẩu {} Customer thành '{}'", count, raw);
    }

    /** Seed QC checkpoints (song ngữ) nếu chưa có */
    private void seedQcCheckpoints() {
        List<QcSeed> seeds = new java.util.ArrayList<>();

        // WARPING (Cuồng mắc)
        addSeed(seeds, "WARPING", "Chủng loại sợi đúng BOM", "Đối chiếu BOM", "AQL", true, 1);
        addSeed(seeds, "WARPING", "Không lẫn cỡ sợi", "Quan sát & so mẫu", "AQL", true, 2);
        addSeed(seeds, "WARPING", "Không sợi đứt/chùng", "Quan sát", "AQL", true, 3);
        addSeed(seeds, "WARPING", "Độ hồ bám", "Kiểm tra độ bám hồ trên sợi", "AQL", true, 4);
        addSeed(seeds, "WARPING", "Không dầu, bẩn", "Quan sát", "AQL", true, 5);

        // WEAVING (Dệt)
        addSeed(seeds, "WEAVING", "Mật độ dệt", "Đo mặt độ warp/weft", "AQL", true, 1);
        addSeed(seeds, "WEAVING", "Độ rộng vải", "Đo khổ vải", "AQL", true, 2);
        addSeed(seeds, "WEAVING", "Mặt vòng", "Quan sát lỗi vòng sợi", "AQL", true, 3);
        addSeed(seeds, "WEAVING", "Sọc", "Quan sát sọc dọc/ngang", "AQL", true, 4);
        addSeed(seeds, "WEAVING", "Vải vân", "Quan sát vải xoắn/vênh", "AQL", true, 5);
        addSeed(seeds, "WEAVING", "Độ bền kéo Warp/Weft", "Thử kéo hai chiều", "AQL", true, 6);

        // DYEING (Nhuộm)
        addSeed(seeds, "DYEING", "Không lem/loang", "Quan sát trên bề mặt", "AQL", true, 1);
        addSeed(seeds, "DYEING", "Ánh sáng", "So màu dưới ánh sáng chuẩn", "AQL", true, 2);
        addSeed(seeds, "DYEING", "Độ co", "Đo độ co theo tiêu chuẩn", "AQL", true, 3);
        addSeed(seeds, "DYEING", "Không dư hóa chất", "Kiểm tra hóa chất dư", "AQL", true, 4);
        addSeed(seeds, "DYEING", "Độ đều màu", "Quan sát và so màu", "AQL", true, 5);
        addSeed(seeds, "DYEING", "Độ bền màu", "Kiểm tra ma sát/giặt", "AQL", true, 6);
        addSeed(seeds, "DYEING", "Độ co rút sau nhuộm", "Đo co rút sau xử lý", "AQL", true, 7);
        addSeed(seeds, "DYEING", "Độ mềm", "Kiểm tra cảm quan độ mềm vải", "AQL", false, 8); // is_mandatory = 0

        // CUTTING (Cắt)
        addSeed(seeds, "CUTTING", "Kích thước chuẩn", "Đo kích thước chi tiết", "100%", true, 1);
        addSeed(seeds, "CUTTING", "Đường cắt thẳng, không tưa sợi", "Quan sát", "AQL", true, 2);
        addSeed(seeds, "CUTTING", "Góc vuông vắn, mép sạch", "Quan sát", "AQL", true, 3);
        addSeed(seeds, "CUTTING", "Cắt đúng layout", "Đối chiếu layout", "AQL", true, 4);
        addSeed(seeds, "CUTTING", "Không bị cuộn biên / lệch biên", "Quan sát", "AQL", true, 5);

        // HEMMING (May)
        addSeed(seeds, "HEMMING", "Đường may thẳng", "Quan sát", "AQL", true, 1);
        addSeed(seeds, "HEMMING", "Mũi chỉ đều", "Đếm & kiểm", "AQL", true, 2);
        addSeed(seeds, "HEMMING", "Không đứt mũi, bỏ mũi", "Quan sát", "AQL", true, 3);
        addSeed(seeds, "HEMMING", "Không nhăn, vặn mép khăn", "Quan sát", "AQL", true, 4);
        addSeed(seeds, "HEMMING", "Gắn nhãn đúng vị trí, không lệch", "Đối chiếu vị trí nhãn", "AQL", true, 5);

        // PACKAGING (Đóng gói)
        addSeed(seeds, "PACKAGING", "Sạch, không sợi thừa", "Quan sát", "AQL", true, 1);
        addSeed(seeds, "PACKAGING", "Gấp đúng quy cách", "Đối chiếu hướng dẫn gấp", "AQL", true, 2);
        addSeed(seeds, "PACKAGING", "Đủ số lượng hàng", "Đếm số lượng", "100%", true, 3);

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
     * ============================
     * LƯU CÁC HÀM KHỞI TẠO MẪU (KHÔNG THỰC THI) NGUYÊN VẸN TỪ BẢN CŨ
     * Để dùng sau này, chỉ cần bỏ comment ở phần gọi trong run().
     * ============================
     * 
     * // @Autowired private MachineRepository machineRepository;
     * // @Autowired private ProductRepository productRepository;
     * // @Autowired private ProductCategoryRepository productCategoryRepository;
     * // @Autowired private MaterialRepository materialRepository;
     * // @Autowired private MaterialStockRepository materialStockRepository;
     * // @Autowired private RoleRepository roleRepository;
     * 
     * // private static final java.util.Random RNG = new java.util.Random();
     * 
     * // private void createSampleMachines() { /* ... giữ nguyên nội dung như bản
     * comment cũ ...
     */ /*
         * }
         * // private void createMachine(String code, String name, String type, String
         * location, String specifications) { /* ...
         */ /*
             * }
             * // private void createSampleMaterials() { /* ...
             */ /*
                 * }
                 * // private void createMaterial(String code, String name, java.math.BigDecimal
                 * priceVndPerKg) { /* ...
                 */ /*
                     * }
                     * // private void createSampleCategoriesAndProducts() { /* ...
                     */ /*
                         * }
                         * // private tmmsystem.entity.ProductCategory createCategory(String name,
                         * String description) { /* ...
                         */ /*
                             * }
                             * // private void createProduct(String code, String name,
                             * tmmsystem.entity.ProductCategory category, String dimensions, int
                             * weightGrams, java.math.BigDecimal price) { /* ...
                             */ /*
                                 * }
                                 * // private java.math.BigDecimal bd(long vnd) { /* ...
                                 */ /*
                                     * }
                                     * 
                                     * // private void createDepartmentalRolesAndUsers() { /* ...
                                     */ /*
                                         * }
                                         * // private void createRoleIfMissing(String name, String description) { /* ...
                                         */ /*
                                             * }
                                             * // private void createUserIfMissing(String email, String name, String
                                             * phone, String roleName) { /* ...
                                             */ /*
                                                 * }
                                                 * 
                                                 */
}
