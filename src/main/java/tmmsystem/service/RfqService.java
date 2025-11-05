package tmmsystem.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tmmsystem.entity.*;
import tmmsystem.repository.*;
import tmmsystem.dto.sales.RfqDetailDto;
import tmmsystem.dto.sales.RfqCreateDto;
import tmmsystem.dto.sales.RfqPublicCreateDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RfqService {
    private final RfqRepository rfqRepository;
    private final RfqDetailRepository detailRepository;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public RfqService(RfqRepository rfqRepository, RfqDetailRepository detailRepository, NotificationService notificationService, CustomerRepository customerRepository, UserRepository userRepository) {
        this.rfqRepository = rfqRepository;
        this.detailRepository = detailRepository;
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    public List<Rfq> findAll() { return rfqRepository.findAll(); }
    public Rfq findById(Long id) { return rfqRepository.findById(id).orElseThrow(); }

    @Transactional
    public Rfq create(Rfq rfq) {
        if (rfq.getRfqNumber() == null || rfq.getRfqNumber().isBlank()) {
            rfq.setRfqNumber(generateRfqNumber());
        }
        return rfqRepository.save(rfq);
    }

    @Transactional
    public Rfq createWithDetails(Rfq rfq, List<RfqDetailDto> details) {
        if (rfq.getRfqNumber() == null || rfq.getRfqNumber().isBlank()) {
            rfq.setRfqNumber(generateRfqNumber());
        }
        // Lưu RFQ trước
        Rfq savedRfq = rfqRepository.save(rfq);
        
        // Nếu có details, thêm vào RFQ
        if (details != null && !details.isEmpty()) {
            for (RfqDetailDto detailDto : details) {
                RfqDetail detail = new RfqDetail();
                detail.setRfq(savedRfq);
                
                if (detailDto.getProductId() != null) {
                    Product product = new Product();
                    product.setId(detailDto.getProductId());
                    detail.setProduct(product);
                }
                
                detail.setQuantity(detailDto.getQuantity());
                detail.setUnit(detailDto.getUnit());
                detail.setNoteColor(detailDto.getNoteColor());
                detail.setNotes(detailDto.getNotes());
                
                detailRepository.save(detail);
            }
        }
        
        return savedRfq;
    }

    // New: create RFQ for logged-in customer (uses RfqCreateDto)
    @Transactional
    public Rfq createFromLoggedIn(RfqCreateDto dto) {
        if (dto.getCustomerId() == null) throw new IllegalArgumentException("customerId is required");
        Rfq rfq = new Rfq();
        rfq.setRfqNumber(dto.getRfqNumber());
        Customer c = new Customer(); c.setId(dto.getCustomerId()); rfq.setCustomer(c);
        rfq.setSourceType(dto.getSourceType());
        rfq.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        rfq.setStatus(dto.getStatus());
        rfq.setSent(dto.getIsSent());
        rfq.setNotes(dto.getNotes());
        if (dto.getCreatedById() != null) { User u = new User(); u.setId(dto.getCreatedById()); rfq.setCreatedBy(u); }
        if (dto.getAssignedPlanningId() != null) { User u = new User(); u.setId(dto.getAssignedPlanningId()); rfq.setAssignedPlanning(u); }
        if (dto.getApprovedById() != null) { User u = new User(); u.setId(dto.getApprovedById()); rfq.setApprovedBy(u); }
        rfq.setApprovalDate(dto.getApprovalDate());

        // If employeeCode provided, try find user and assign if role is Sales
        if (dto.getEmployeeCode() != null && !dto.getEmployeeCode().isBlank()) {
            String code = dto.getEmployeeCode().trim();
            User user = userRepository.findByEmployeeCode(code).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid employeeCode or user not found: " + code)
            );
            String roleName = user.getRole() != null ? user.getRole().getName() : null;
            if (roleName == null || !roleName.toUpperCase().contains("SALE")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not a Sales staff: " + code);
            }
            rfq.setAssignedSales(user);
        } else if (dto.getAssignedSalesId() != null) {
            User u = new User(); u.setId(dto.getAssignedSalesId()); rfq.setAssignedSales(u);
        }

        return createWithDetails(rfq, dto.getDetails());
    }

    // New: create RFQ from public form (find or create customer)
    @Transactional
    public Rfq createFromPublic(RfqPublicCreateDto dto) {
        // validate at service level as well (caller should have validated DTO)
        if ((dto.getContactEmail() == null || dto.getContactEmail().isBlank()) && (dto.getContactPhone() == null || dto.getContactPhone().isBlank())) {
            throw new IllegalArgumentException("Public submission must provide contactEmail or contactPhone");
        }

        String normEmail = normalizeEmail(dto.getContactEmail());
        String normPhone = normalizePhone(dto.getContactPhone());

        Customer customer = null;
        if (normEmail != null) {
            customer = customerRepository.findByEmail(normEmail).orElse(null);
        }
        if (customer == null && normPhone != null) {
            customer = customerRepository.findByPhoneNumber(normPhone).orElse(null);
        }

        if (customer == null) {
            Customer newCustomer = new Customer();
            String prefix = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            int rand = new java.security.SecureRandom().nextInt(900) + 100;
            newCustomer.setCustomerCode("CUS-" + prefix + "-" + rand);
            newCustomer.setContactPerson(dto.getContactPerson());
            if (normEmail != null) newCustomer.setEmail(normEmail);
            if (normPhone != null) newCustomer.setPhoneNumber(normPhone);
            newCustomer.setAddress(dto.getContactAddress());
            newCustomer.setVerified(false);
            newCustomer.setRegistrationType("PUBLIC_FORM");

            try {
                customer = customerRepository.save(newCustomer);
            } catch (DataIntegrityViolationException ex) {
                // Race condition: someone else inserted same email/phone concurrently; re-query
                if (normEmail != null) customer = customerRepository.findByEmail(normEmail).orElse(null);
                if (customer == null && normPhone != null) customer = customerRepository.findByPhoneNumber(normPhone).orElse(null);
                if (customer == null) throw ex; // rethrow if still not found
            }
        }

        Rfq rfq = new Rfq();
        rfq.setRfqNumber(dto.getRfqNumber());
        rfq.setCustomer(customer);
        rfq.setSourceType(dto.getSourceType() == null ? "PUBLIC_FORM" : dto.getSourceType());
        rfq.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        rfq.setStatus(dto.getStatus());
        rfq.setSent(dto.getIsSent());
        rfq.setNotes(dto.getNotes());
        rfq.setApprovalDate(dto.getApprovalDate());

        // If employeeCode was supplied by sales staff in public form, try assign
        if (dto.getEmployeeCode() != null && !dto.getEmployeeCode().isBlank()) {
            String code = dto.getEmployeeCode().trim();
            User user = userRepository.findByEmployeeCode(code).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid employeeCode or user not found: " + code)
            );
            String roleName = user.getRole() != null ? user.getRole().getName() : null;
            if (roleName == null || !roleName.toUpperCase().contains("SALE")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not a Sales staff: " + code);
            }
            rfq.setAssignedSales(user);
        }

        return createWithDetails(rfq, dto.getDetails());
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase();
        return e.isEmpty() ? null : e;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String p = phone.trim();
        if (p.isEmpty()) return null;
        // strip spaces and common separators for normalization
        p = p.replaceAll("[ ()\\-]", "");
        return p;
    }

    @Transactional
    public Rfq update(Long id, Rfq updated) {
        Rfq existing = rfqRepository.findById(id).orElseThrow();
        existing.setRfqNumber(updated.getRfqNumber());
        existing.setCustomer(updated.getCustomer());
        existing.setExpectedDeliveryDate(updated.getExpectedDeliveryDate());
        existing.setStatus(updated.getStatus());
        existing.setSent(updated.getSent());
        existing.setNotes(updated.getNotes());
        existing.setCreatedBy(updated.getCreatedBy());
        existing.setApprovedBy(updated.getApprovedBy());
        return existing;
    }

    public void delete(Long id) { rfqRepository.deleteById(id); }

    // RFQ Detail Management
    public List<RfqDetail> findDetailsByRfqId(Long rfqId) { 
        return detailRepository.findByRfqId(rfqId); 
    }

    public RfqDetail findDetailById(Long id) { 
        return detailRepository.findById(id).orElseThrow(); 
    }

    @Transactional
    public RfqDetail addDetail(Long rfqId, RfqDetailDto dto) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        RfqDetail detail = new RfqDetail();
        detail.setRfq(rfq);
        
        if (dto.getProductId() != null) {
            Product product = new Product();
            product.setId(dto.getProductId());
            detail.setProduct(product);
        }
        
        detail.setQuantity(dto.getQuantity());
        detail.setUnit(dto.getUnit());
        detail.setNoteColor(dto.getNoteColor());
        detail.setNotes(dto.getNotes());
        
        return detailRepository.save(detail);
    }

    @Transactional
    public RfqDetail updateDetail(Long id, RfqDetailDto dto) {
        RfqDetail detail = detailRepository.findById(id).orElseThrow();
        
        if (dto.getProductId() != null) {
            Product product = new Product();
            product.setId(dto.getProductId());
            detail.setProduct(product);
        }
        
        detail.setQuantity(dto.getQuantity());
        detail.setUnit(dto.getUnit());
        detail.setNoteColor(dto.getNoteColor());
        detail.setNotes(dto.getNotes());
        
        return detailRepository.save(detail);
    }

    public void deleteDetail(Long id) { 
        detailRepository.deleteById(id); 
    }
    
    @Transactional
    public Rfq updateExpectedDeliveryDate(Long id, String expectedDeliveryDate) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        
        // Parse ngày từ string với hỗ trợ 2 định dạng
        java.time.LocalDate deliveryDate = parseDeliveryDate(expectedDeliveryDate);
        
        // Cập nhật chỉ ngày giao hàng mong muốn
        rfq.setExpectedDeliveryDate(deliveryDate);
        
        return rfqRepository.save(rfq);
    }
    
    private java.time.LocalDate parseDeliveryDate(String dateString) {
        try {
            // Thử định dạng yyyy-MM-dd trước
            return java.time.LocalDate.parse(dateString);
        } catch (java.time.format.DateTimeParseException e1) {
            try {
                // Thử định dạng dd-MM-yyyy
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
                return java.time.LocalDate.parse(dateString, formatter);
            } catch (java.time.format.DateTimeParseException e2) {
                throw new IllegalArgumentException("Ngày không hợp lệ. Hỗ trợ định dạng: yyyy-MM-dd hoặc dd-MM-yyyy. Ví dụ: 2025-05-10 hoặc 10-05-2025");
            }
        }
    }

    // RFQ Workflow Methods
    @Transactional
    public Rfq sendRfq(Long id) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        if (!"DRAFT".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ must be in DRAFT status to send");
        }
        // must have assignments before sending
        if (rfq.getAssignedSales() == null || rfq.getAssignedPlanning() == null) {
            throw new IllegalStateException("Cần gán Sales và Planning trước khi gửi RFQ");
        }
        rfq.setStatus("SENT");
        rfq.setSent(true);
        Rfq savedRfq = rfqRepository.save(rfq);
        notificationService.notifyNewRfq(savedRfq);
        return savedRfq;
    }

    @Transactional
    public Rfq preliminaryCheck(Long id) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        if (!"SENT".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ must be in SENT status to preliminary check");
        }
        // Kiểm tra sơ bộ: phải có ít nhất một dòng chi tiết và có ngày giao mong muốn
        List<RfqDetail> details = detailRepository.findByRfqId(rfq.getId());
        if (details == null || details.isEmpty()) {
            throw new IllegalStateException("RFQ must contain at least one product line");
        }
        if (rfq.getExpectedDeliveryDate() == null) {
            throw new IllegalStateException("Expected delivery date is required");
        }
        rfq.setStatus("PRELIMINARY_CHECKED");
        return rfqRepository.save(rfq);
    }

    @Transactional
    public Rfq forwardToPlanning(Long id) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        if (!"PRELIMINARY_CHECKED".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ must be preliminary-checked before forwarding to planning");
        }
        rfq.setStatus("FORWARDED_TO_PLANNING");
        Rfq savedRfq = rfqRepository.save(rfq);
        
        // Gửi thông báo cho Planning Staff
        notificationService.notifyRfqForwardedToPlanning(savedRfq);
        
        return savedRfq;
    }

    @Transactional
    public Rfq receiveByPlanning(Long id) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        if (!"FORWARDED_TO_PLANNING".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ must be forwarded to planning first");
        }
        rfq.setStatus("RECEIVED_BY_PLANNING");
        Rfq savedRfq = rfqRepository.save(rfq);
        
        // Gửi thông báo cho Sale Staff
        notificationService.notifyRfqReceivedByPlanning(savedRfq);
        
        return savedRfq;
    }

    @Transactional
    public Rfq cancelRfq(Long id) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        if ("CANCELED".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ is already canceled");
        }
        rfq.setStatus("CANCELED");
        Rfq savedRfq = rfqRepository.save(rfq);
        
        // Gửi thông báo hủy RFQ
        notificationService.notifyRfqCanceled(savedRfq);
        
        return savedRfq;
    }

    @Transactional
    public Rfq assignStaff(Long rfqId, Long salesId, Long planningId, Long approvedById) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (!"DRAFT".equals(rfq.getStatus())) {
            throw new IllegalStateException("Chỉ có thể gán nhân sự khi RFQ đang ở trạng thái DRAFT");
        }
        if (salesId == null || planningId == null) {
            throw new IllegalArgumentException("Thiếu Sales hoặc Planning để gán");
        }
        User sales = new User(); sales.setId(salesId);
        User planning = new User(); planning.setId(planningId);
        rfq.setAssignedSales(sales);
        rfq.setAssignedPlanning(planning);
        if (approvedById != null) {
            User director = new User(); director.setId(approvedById);
            rfq.setApprovedBy(director);
            rfq.setApprovalDate(java.time.Instant.now());
        }
        return rfqRepository.save(rfq);
    }

    // Lấy danh sách RFQ được gán cho Sales (dùng header X-User-Id để gọi)
    public List<Rfq> findByAssignedSales(Long salesId) {
        if (salesId == null) return java.util.Collections.emptyList();
        return rfqRepository.findAll().stream()
                .filter(r -> r.getAssignedSales() != null && salesId.equals(r.getAssignedSales().getId()))
                .collect(Collectors.toList());
    }

    // Lấy danh sách RFQ được gán cho Planning
    public List<Rfq> findByAssignedPlanning(Long planningId) {
        if (planningId == null) return java.util.Collections.emptyList();
        return rfqRepository.findAll().stream()
                .filter(r -> r.getAssignedPlanning() != null && planningId.equals(r.getAssignedPlanning().getId()))
                .collect(Collectors.toList());
    }

    // Lấy danh sách RFQ đang ở trạng thái DRAFT và chưa được gán (dành cho Director xem trước khi phân công)
    public List<Rfq> findDraftUnassigned() {
        return rfqRepository.findAll().stream()
                .filter(r -> "DRAFT".equals(r.getStatus()))
                .filter(r -> r.getAssignedSales() == null || r.getAssignedPlanning() == null)
                .collect(Collectors.toList());
    }

    private String generateRfqNumber() {
        String dateStr = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        int attempt = 0;
        while (attempt < 3) {
            Integer maxSeq = rfqRepository.findMaxRfqSeqForToday();
            int next = (maxSeq == null ? 1 : maxSeq + 1);
            String candidate = "RFQ-" + dateStr + "-" + String.format("%03d", next);
            if (!rfqRepository.existsByRfqNumber(candidate)) {
                return candidate;
            }
            attempt++;
            try { Thread.sleep(10L); } catch (InterruptedException ignored) {}
        }
        // Fallback random suffix to avoid collisions
        int rand = new java.security.SecureRandom().nextInt(900) + 100;
        return "RFQ-" + dateStr + "-" + String.format("%03d", rand);
    }
}
