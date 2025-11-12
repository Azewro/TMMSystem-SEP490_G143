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
import tmmsystem.dto.sales.SalesRfqCreateRequest;
import tmmsystem.dto.sales.SalesRfqEditRequest;

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
        Rfq savedRfq = rfqRepository.save(rfq);
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

        if (dto.getExpectedDeliveryDate() != null) validateExpectedDeliveryDate(dto.getExpectedDeliveryDate());

        // Snapshot contact from customer if available (for historical integrity)
        Customer loaded = customerRepository.findById(dto.getCustomerId()).orElse(null);
        if (loaded != null) {
            rfq.setContactPersonSnapshot(loaded.getContactPerson());
            rfq.setContactEmailSnapshot(loaded.getEmail());
            rfq.setContactPhoneSnapshot(loaded.getPhoneNumber());
            rfq.setContactAddressSnapshot(loaded.getAddress());
            // Determine contact method if possible (prefer EMAIL if valid email present)
            String method = inferContactMethod(loaded.getEmail(), loaded.getPhoneNumber());
            rfq.setContactMethod(method);
        }

        return createWithDetails(rfq, dto.getDetails());
    }

    @Transactional
    public Rfq createFromPublic(RfqPublicCreateDto dto) {
        // validate at service level as well (caller should have validated DTO)
        if ((dto.getContactEmail() == null || dto.getContactEmail().isBlank()) && (dto.getContactPhone() == null || dto.getContactPhone().isBlank())) {
            throw new IllegalArgumentException("Public submission must provide contactEmail or contactPhone");
        }

        String normEmail = normalizeEmail(dto.getContactEmail());
        String normPhone = normalizePhone(dto.getContactPhone());

        String effectiveMethod = validateAndDetermineMethod(dto.getContactMethod(), normEmail, normPhone);

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
        // snapshot fields from submission
        rfq.setContactPersonSnapshot(dto.getContactPerson());
        rfq.setContactEmailSnapshot(normEmail);
        rfq.setContactPhoneSnapshot(normPhone);
        rfq.setContactAddressSnapshot(dto.getContactAddress());
        rfq.setContactMethod(effectiveMethod);

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

        if (dto.getExpectedDeliveryDate() != null) validateExpectedDeliveryDate(dto.getExpectedDeliveryDate());

        return createWithDetails(rfq, dto.getDetails());
    }

    @Transactional
    public Rfq createBySales(SalesRfqCreateRequest req, Long salesUserId) {
        if (salesUserId == null) throw new IllegalArgumentException("Missing salesUserId header");
        if ((req.getContactEmail() == null || req.getContactEmail().isBlank()) && (req.getContactPhone() == null || req.getContactPhone().isBlank())) {
            throw new IllegalArgumentException("Must provide contactEmail or contactPhone");
        }
        String email = normalizeEmail(req.getContactEmail());
        String phone = normalizePhone(req.getContactPhone());
        String method = validateAndDetermineMethod(req.getContactMethod(), email, phone);
        Customer customer = null;
        if (email != null) customer = customerRepository.findByEmail(email).orElse(null);
        if (customer == null && phone != null) customer = customerRepository.findByPhoneNumber(phone).orElse(null);
        if (customer == null) {
            customer = new Customer();
            String prefix = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            int rand = new java.security.SecureRandom().nextInt(900) + 100;
            customer.setCustomerCode("CUS-" + prefix + "-" + rand);
            customer.setContactPerson(req.getContactPerson());
            if (email != null) customer.setEmail(email);
            if (phone != null) customer.setPhoneNumber(phone);
            customer.setAddress(req.getContactAddress());
            customer.setVerified(false);
            customer.setRegistrationType("SALES_CREATED_ON_BEHALF");
            customer = customerRepository.save(customer);
        }
        Rfq rfq = new Rfq();
        rfq.setCustomer(customer);
        rfq.setSourceType("BY_SALES");
        rfq.setExpectedDeliveryDate(req.getExpectedDeliveryDate());
        rfq.setStatus("DRAFT");
        rfq.setNotes(req.getNotes());
        User sales = new User(); sales.setId(salesUserId); rfq.setAssignedSales(sales);
        // snapshots
        rfq.setContactPersonSnapshot(req.getContactPerson());
        rfq.setContactEmailSnapshot(email);
        rfq.setContactPhoneSnapshot(phone);
        rfq.setContactAddressSnapshot(req.getContactAddress());
        rfq.setContactMethod(method);
        if (req.getExpectedDeliveryDate() != null) validateExpectedDeliveryDate(req.getExpectedDeliveryDate());
        return createWithDetails(rfq, req.getDetails());
    }

    private String validateAndDetermineMethod(String providedMethod, String email, String phone) {
        String inferred = inferContactMethod(email, phone);
        if (providedMethod == null || providedMethod.isBlank()) return inferred;
        String upper = providedMethod.trim().toUpperCase();
        if (!upper.equals("EMAIL") && !upper.equals("PHONE")) {
            throw new IllegalArgumentException("contactMethod must be EMAIL or PHONE");
        }
        if (upper.equals("EMAIL") && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("contactEmail required when contactMethod=EMAIL");
        }
        if (upper.equals("PHONE") && (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("contactPhone required when contactMethod=PHONE");
        }
        return upper;
    }

    private String inferContactMethod(String email, String phone) {
        if (email != null && !email.isBlank()) return "EMAIL";
        if (phone != null && !phone.isBlank()) return "PHONE";
        return null; // caller validation ensures at least one present
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

    public List<RfqDetail> findDetailsByRfqId(Long rfqId) { return detailRepository.findByRfqId(rfqId); }
    public RfqDetail findDetailById(Long id) { return detailRepository.findById(id).orElseThrow(); }

    @Transactional
    public RfqDetail addDetail(Long rfqId, RfqDetailDto dto) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (isImmutableStatus(rfq.getStatus())) {
            throw new IllegalStateException("RFQ is not editable at current status");
        }
        RfqDetail detail = new RfqDetail();
        detail.setRfq(rfq);
        if (dto.getProductId() != null) { Product product = new Product(); product.setId(dto.getProductId()); detail.setProduct(product); }
        detail.setQuantity(dto.getQuantity());
        detail.setUnit(dto.getUnit());
        detail.setNoteColor(dto.getNoteColor());
        detail.setNotes(dto.getNotes());
        return detailRepository.save(detail);
    }

    @Transactional
    public RfqDetail updateDetail(Long id, RfqDetailDto dto) {
        RfqDetail detail = detailRepository.findById(id).orElseThrow();
        Rfq rfq = detail.getRfq();
        if (isImmutableStatus(rfq.getStatus())) {
            throw new IllegalStateException("RFQ is not editable at current status");
        }
        if (dto.getProductId() != null) { Product product = new Product(); product.setId(dto.getProductId()); detail.setProduct(product); }
        detail.setQuantity(dto.getQuantity());
        detail.setUnit(dto.getUnit());
        detail.setNoteColor(dto.getNoteColor());
        detail.setNotes(dto.getNotes());
        return detailRepository.save(detail);
    }

    public void deleteDetail(Long id) {
        RfqDetail detail = detailRepository.findById(id).orElseThrow();
        Rfq rfq = detail.getRfq();
        if (isImmutableStatus(rfq.getStatus())) {
            throw new IllegalStateException("RFQ is not editable at current status");
        }
        detailRepository.deleteById(id);
    }

    private boolean isImmutableStatus(String status) {
        if (status == null) return false;
        return switch (status) {
            case "PRELIMINARY_CHECKED", "FORWARDED_TO_PLANNING", "RECEIVED_BY_PLANNING", "QUOTED", "CANCELED" -> true;
            default -> false;
        };
    }

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
        var details = detailRepository.findByRfqId(rfq.getId());
        if (details == null || details.isEmpty()) {
            throw new IllegalStateException("RFQ must contain at least one product line");
        }
        if (rfq.getExpectedDeliveryDate() == null) {
            throw new IllegalStateException("Expected delivery date is required");
        }
        rfq.setStatus("PRELIMINARY_CHECKED");
        rfq.setSalesConfirmedAt(java.time.Instant.now());
        rfq.setSalesConfirmedBy(rfq.getAssignedSales());
        Rfq saved = rfqRepository.save(rfq);
        notificationService.notifySalesConfirmed(saved);
        return saved;
    }

    @Transactional
    public Rfq forwardToPlanning(Long id) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        if (!"PRELIMINARY_CHECKED".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ must be preliminary-checked before forwarding to planning");
        }
        rfq.setStatus("FORWARDED_TO_PLANNING");
        Rfq savedRfq = rfqRepository.save(rfq);
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
        notificationService.notifyRfqCanceled(savedRfq);
        return savedRfq;
    }

    @Transactional
    public Rfq assignSales(Long rfqId, Long salesId, String employeeCode) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (!"DRAFT".equals(rfq.getStatus())) throw new IllegalStateException("Chỉ gán Sales khi RFQ ở DRAFT");
        if (salesId == null && (employeeCode == null || employeeCode.isBlank())) {
            throw new IllegalArgumentException("Thiếu assignedSalesId hoặc employeeCode");
        }
        User salesUser;
        if (salesId != null) {
            salesUser = new User(); salesUser.setId(salesId);
        } else {
            User user = userRepository.findByEmployeeCode(employeeCode.trim()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid employeeCode or user not found: " + employeeCode));
            String roleName = user.getRole() != null ? user.getRole().getName() : null;
            if (roleName == null || !roleName.toUpperCase().contains("SALE")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not a Sales staff: " + employeeCode);
            }
            salesUser = user;
        }
        rfq.setAssignedSales(salesUser);
        return rfqRepository.save(rfq);
    }

    @Transactional
    public Rfq assignPlanning(Long rfqId, Long planningId) {
        if (planningId == null) throw new IllegalArgumentException("assignedPlanningId is required");
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (!"DRAFT".equals(rfq.getStatus())) throw new IllegalStateException("Chỉ gán Planning khi RFQ ở DRAFT");
        User planning = new User(); planning.setId(planningId);
        rfq.setAssignedPlanning(planning);
        return rfqRepository.save(rfq);
    }

    @Transactional
    public Rfq assignStaff(Long rfqId, Long salesId, Long planningId, Long approvedById) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (!"DRAFT".equals(rfq.getStatus())) {
            throw new IllegalStateException("Chỉ có thể gán nhân sự khi RFQ đang ở trạng thái DRAFT");
        }
        if (salesId != null) { User sales = new User(); sales.setId(salesId); rfq.setAssignedSales(sales); }
        if (planningId != null) { User planning = new User(); planning.setId(planningId); rfq.setAssignedPlanning(planning); }
        if (approvedById != null) { User director = new User(); director.setId(approvedById); rfq.setApprovedBy(director); rfq.setApprovalDate(java.time.Instant.now()); }
        return rfqRepository.save(rfq);
    }

    public List<Rfq> findByAssignedSales(Long salesId) {
        if (salesId == null) return java.util.Collections.emptyList();
        return rfqRepository.findAll().stream()
                .filter(r -> r.getAssignedSales() != null && salesId.equals(r.getAssignedSales().getId()))
                .collect(Collectors.toList());
    }

    public List<Rfq> findByAssignedPlanning(Long planningId) {
        if (planningId == null) return java.util.Collections.emptyList();
        return rfqRepository.findAll().stream()
                .filter(r -> r.getAssignedPlanning() != null && planningId.equals(r.getAssignedPlanning().getId()))
                .collect(Collectors.toList());
    }

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

    // Hook to persist capacity evaluation summary into RFQ
    @Transactional
    public void persistCapacityEvaluation(Long rfqId, String capacityStatus, String reason, java.time.LocalDate proposedNewDate) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        rfq.setCapacityStatus(capacityStatus);
        rfq.setCapacityReason(reason);
        rfq.setProposedNewDeliveryDate(proposedNewDate);
        rfqRepository.save(rfq);
        if ("INSUFFICIENT".equalsIgnoreCase(capacityStatus)) {
            notificationService.notifyCapacityInsufficient(rfq);
        }
    }

    @Transactional
    public Rfq salesEditRfqAndCustomer(Long rfqId, Long salesUserId, SalesRfqEditRequest req) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (!"DRAFT".equals(rfq.getStatus()) && !"SENT".equals(rfq.getStatus())) {
            throw new IllegalStateException("Chỉ sửa khi RFQ ở DRAFT hoặc SENT trước preliminary-check");
        }
        if (Boolean.TRUE.equals(rfq.getLocked())) {
            throw new IllegalStateException("RFQ locked");
        }
        if (rfq.getAssignedSales() == null || !salesUserId.equals(rfq.getAssignedSales().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned sales");
        }
        if (req.getExpectedDeliveryDate() != null) validateExpectedDeliveryDate(req.getExpectedDeliveryDate());
        if (req.getNotes() != null) rfq.setNotes(req.getNotes());
        Customer cust = rfq.getCustomer();
        if (cust != null) {
            if (req.getContactPerson() != null) cust.setContactPerson(req.getContactPerson());
            if (req.getContactEmail() != null) cust.setEmail(normalizeEmail(req.getContactEmail()));
            if (req.getContactPhone() != null) cust.setPhoneNumber(normalizePhone(req.getContactPhone()));
            if (req.getContactAddress() != null) cust.setAddress(req.getContactAddress());
            customerRepository.save(cust);
            // Update snapshots simultaneously
            if (req.getContactPerson() != null) rfq.setContactPersonSnapshot(req.getContactPerson());
            if (req.getContactEmail() != null) rfq.setContactEmailSnapshot(normalizeEmail(req.getContactEmail()));
            if (req.getContactPhone() != null) rfq.setContactPhoneSnapshot(normalizePhone(req.getContactPhone()));
            if (req.getContactAddress() != null) rfq.setContactAddressSnapshot(req.getContactAddress());
            if (req.getContactMethod() != null && !req.getContactMethod().isBlank()) {
                String method = validateAndDetermineMethod(req.getContactMethod(), rfq.getContactEmailSnapshot(), rfq.getContactPhoneSnapshot());
                rfq.setContactMethod(method);
            } else {
                // Re-infer if snapshots changed
                String inferred = inferContactMethod(rfq.getContactEmailSnapshot(), rfq.getContactPhoneSnapshot());
                rfq.setContactMethod(inferred);
            }
        }
        if (req.getDetails() != null) {
            detailRepository.findByRfqId(rfq.getId()).forEach(d -> detailRepository.deleteById(d.getId()));
            for (RfqDetailDto d : req.getDetails()) {
                RfqDetail nd = new RfqDetail();
                nd.setRfq(rfq);
                if (d.getProductId() != null) { Product p = new Product(); p.setId(d.getProductId()); nd.setProduct(p); }
                nd.setQuantity(d.getQuantity()); nd.setUnit(d.getUnit()); nd.setNoteColor(d.getNoteColor()); nd.setNotes(d.getNotes());
                detailRepository.save(nd);
            }
        }
        return rfqRepository.save(rfq);
    }

    @Transactional
    public Rfq updateExpectedDeliveryDate(Long id, String expectedDateStr) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        if (Boolean.TRUE.equals(rfq.getLocked())) {
            throw new IllegalStateException("RFQ is locked and cannot change expected delivery date");
        }
        java.time.LocalDate date = parseFlexibleDate(expectedDateStr);
        validateExpectedDeliveryDate(date);
        rfq.setExpectedDeliveryDate(date);
        return rfqRepository.save(rfq);
    }

    private java.time.LocalDate parseFlexibleDate(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("expectedDeliveryDate is required");
        String v = value.trim();
        try {
            return java.time.LocalDate.parse(v); // yyyy-MM-dd
        } catch (java.time.format.DateTimeParseException e) {
            try {
                java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
                return java.time.LocalDate.parse(v, f);
            } catch (java.time.format.DateTimeParseException ex) {
                try {
                    int day = Integer.parseInt(v);
                    java.time.LocalDate now = java.time.LocalDate.now();
                    return now.withDayOfMonth(day);
                } catch (NumberFormatException ex2) {
                    throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd or dd-MM-yyyy or day of month (1-31)");
                }
            }
        }
    }

    private void validateExpectedDeliveryDate(java.time.LocalDate date) {
        if (date == null) throw new IllegalArgumentException("expectedDeliveryDate is required");
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate min = today.plusDays(30);
        if (date.isBefore(min)) {
            throw new IllegalArgumentException("Expected delivery date must be at least 30 days from today (>= " + min + ")");
        }
    }
}
