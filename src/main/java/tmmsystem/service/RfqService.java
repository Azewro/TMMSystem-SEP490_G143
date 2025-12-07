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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class RfqService {
    private final RfqRepository rfqRepository;
    private final RfqDetailRepository detailRepository;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final QuotationRepository quotationRepository;

    public RfqService(RfqRepository rfqRepository, RfqDetailRepository detailRepository,
            NotificationService notificationService, CustomerRepository customerRepository,
            UserRepository userRepository, ProductRepository productRepository,
            QuotationRepository quotationRepository) {
        this.rfqRepository = rfqRepository;
        this.detailRepository = detailRepository;
        this.notificationService = notificationService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.quotationRepository = quotationRepository;
    }

    public List<Rfq> findAll() {
        return rfqRepository.findAll();
    }

    @Transactional
    public void syncRejectedQuotations(org.slf4j.Logger externalLog) {
        // Find all REJECTED quotations
        List<tmmsystem.entity.Quotation> rejectedQuotes = quotationRepository.findByStatus("REJECTED");
        int count = 0;
        for (tmmsystem.entity.Quotation q : rejectedQuotes) {
            if (q.getRfq() != null) {
                tmmsystem.entity.Rfq rfq = q.getRfq();
                if (!"REJECTED".equals(rfq.getStatus())) {
                    rfq.setStatus("REJECTED");
                    rfqRepository.save(rfq);
                    count++;
                }
            }
        }
        if (count > 0 && externalLog != null) {
            externalLog.info("Startup: Synced {} RFQs to REJECTED based on rejected quotations", count);
        }
    }

    public Page<Rfq> findAll(Pageable pageable) {
        return rfqRepository.findAll(pageable);
    }

    public Page<Rfq> findAll(Pageable pageable, String search, String status) {
        return findAll(pageable, search, status, null);
    }

    public Page<Rfq> findAll(Pageable pageable, String search, String status, Long customerId) {
        return findAll(pageable, search, status, customerId, null);
    }

    public Page<Rfq> findAll(Pageable pageable, String search, String status, Long customerId, String createdDate) {
        // Check if we have any actual filters (not null and not empty)
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty();
        boolean hasCustomerId = customerId != null;
        boolean hasCreatedDate = createdDate != null && !createdDate.trim().isEmpty();

        if (!hasSearch && !hasStatus && !hasCustomerId && !hasCreatedDate) {
            return rfqRepository.findAll(pageable);
        }

        String searchLower = hasSearch && search != null ? search.trim().toLowerCase() : "";
        String finalStatus = status;
        Long finalCustomerId = customerId;

        // Parse createdDate if provided
        java.time.LocalDate targetDate = null;
        if (hasCreatedDate) {
            try {
                targetDate = java.time.LocalDate.parse(createdDate.trim());
            } catch (Exception e) {
                // Invalid date format, ignore
            }
        }
        final java.time.LocalDate finalTargetDate = targetDate;

        return rfqRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Customer filter
            if (hasCustomerId && finalCustomerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), finalCustomerId));
            }

            // Search predicate - chỉ tìm theo mã RFQ và tên khách hàng
            if (hasSearch && search != null) {
                var searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("rfqNumber")), "%" + searchLower + "%"),
                        cb.like(cb.lower(root.get("customer").get("companyName")), "%" + searchLower + "%"));
                predicates.add(searchPredicate);
            }

            // Status filter
            if (hasStatus && finalStatus != null) {
                if ("WAITING_ASSIGNMENT".equals(finalStatus)) {
                    predicates.add(cb.equal(root.get("status"), "SENT"));
                    predicates.add(cb.isNull(root.get("assignedSales")));
                } else if ("ASSIGNED".equals(finalStatus)) {
                    predicates.add(cb.equal(root.get("status"), "SENT"));
                    predicates.add(cb.isNotNull(root.get("assignedSales")));
                } else if (finalStatus.contains(",")) {
                    String[] statuses = finalStatus.split(",");
                    var statusPredicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    for (String s : statuses) {
                        if (!s.trim().isEmpty()) {
                            statusPredicates.add(cb.equal(root.get("status"), s.trim()));
                        }
                    }
                    if (!statusPredicates.isEmpty()) {
                        predicates.add(cb.or(statusPredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
                    }
                } else {
                    predicates.add(cb.equal(root.get("status"), finalStatus));
                }
            }

            // Created date filter
            if (hasCreatedDate && finalTargetDate != null) {
                // Filter by date (ignoring time part)
                // Compare date part only: createdAt >= start of day AND createdAt < start of
                // next day
                java.time.LocalDateTime startOfDay = finalTargetDate.atStartOfDay();
                java.time.LocalDateTime startOfNextDay = finalTargetDate.plusDays(1).atStartOfDay();
                predicates.add(cb.and(
                        cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay),
                        cb.lessThan(root.get("createdAt"), startOfNextDay)));
            }

            // Ensure we have at least one predicate before combining
            if (predicates.isEmpty()) {
                return cb.conjunction(); // Return true predicate if no filters
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
    }

    public Rfq findById(Long id) {
        return rfqRepository.findById(id).orElseThrow();
    }

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
                    Product product = productRepository.findById(detailDto.getProductId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Product not found with id: " + detailDto.getProductId()));
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
        if (dto.getCustomerId() == null)
            throw new IllegalArgumentException("customerId is required");
        Rfq rfq = new Rfq();
        rfq.setRfqNumber(dto.getRfqNumber());
        Customer c = new Customer();
        c.setId(dto.getCustomerId());
        rfq.setCustomer(c);
        rfq.setSourceType(dto.getSourceType());
        rfq.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        rfq.setStatus(dto.getStatus());
        rfq.setSent(dto.getIsSent());
        rfq.setNotes(dto.getNotes());
        if (dto.getCreatedById() != null) {
            User u = new User();
            u.setId(dto.getCreatedById());
            rfq.setCreatedBy(u);
        }
        if (dto.getAssignedPlanningId() != null) {
            User u = new User();
            u.setId(dto.getAssignedPlanningId());
            rfq.setAssignedPlanning(u);
        }
        if (dto.getApprovedById() != null) {
            User u = new User();
            u.setId(dto.getApprovedById());
            rfq.setApprovedBy(u);
        }
        rfq.setApprovalDate(dto.getApprovalDate());

        if (dto.getEmployeeCode() != null && !dto.getEmployeeCode().isBlank()) {
            String code = dto.getEmployeeCode().trim();
            User user = userRepository.findByEmployeeCode(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid employeeCode or user not found: " + code));
            String roleName = user.getRole() != null ? user.getRole().getName() : null;
            if (roleName == null || !roleName.toUpperCase().contains("SALE")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not a Sales staff: " + code);
            }
            rfq.setAssignedSales(user);
        } else if (dto.getAssignedSalesId() != null) {
            User u = new User();
            u.setId(dto.getAssignedSalesId());
            rfq.setAssignedSales(u);
        } else {
            // Auto-assign if no specific sales requested
            User sales = findLeastBusySalesStaff();
            if (sales != null) {
                rfq.setAssignedSales(sales);
            }
        }

        // Auto-set status to SENT if Sales is assigned and status is DRAFT or null
        // Request: "trạng thái lúc này là sent luôn"
        if (rfq.getStatus() == null || "DRAFT".equals(rfq.getStatus())) {
            rfq.setStatus("SENT");
            rfq.setSent(true);
        }

        if (dto.getExpectedDeliveryDate() != null)
            validateExpectedDeliveryDate(dto.getExpectedDeliveryDate(), java.time.LocalDate.now());

        // Prioritize contact info from DTO, fall back to customer profile for
        // historical snapshot
        Customer loaded = customerRepository.findById(dto.getCustomerId()).orElse(null);
        if (loaded != null) {
            rfq.setContactPersonSnapshot(
                    dto.getContactPerson() != null && !dto.getContactPerson().isBlank() ? dto.getContactPerson()
                            : loaded.getContactPerson());
            rfq.setContactEmailSnapshot(dto.getContactEmail() != null && !dto.getContactEmail().isBlank()
                    ? normalizeEmail(dto.getContactEmail())
                    : normalizeEmail(loaded.getEmail()));
            rfq.setContactPhoneSnapshot(dto.getContactPhone() != null && !dto.getContactPhone().isBlank()
                    ? normalizePhone(dto.getContactPhone())
                    : normalizePhone(loaded.getPhoneNumber()));
            rfq.setContactAddressSnapshot(
                    dto.getContactAddress() != null && !dto.getContactAddress().isBlank() ? dto.getContactAddress()
                            : loaded.getAddress());

            // Prioritize contact method from DTO, then infer from the chosen contact info
            String rfqContactEmail = rfq.getContactEmailSnapshot();
            String rfqContactPhone = rfq.getContactPhoneSnapshot();
            String method = validateAndDetermineMethod(dto.getContactMethod(), rfqContactEmail, rfqContactPhone);
            rfq.setContactMethod(method);
        }

        return createWithDetails(rfq, dto.getDetails());
    }

    @Transactional
    public Rfq createFromPublic(RfqPublicCreateDto dto) {
        // validate at service level as well (caller should have validated DTO)
        if ((dto.getContactEmail() == null || dto.getContactEmail().isBlank())
                && (dto.getContactPhone() == null || dto.getContactPhone().isBlank())) {
            throw new IllegalArgumentException("Public submission must provide contactEmail or contactPhone");
        }

        // Validate contactAddress is not empty (frontend sends full address string)
        if (dto.getContactAddress() == null || dto.getContactAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng điền đầy đủ địa chỉ nhận hàng.");
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
            if (normEmail != null)
                newCustomer.setEmail(normEmail);
            if (normPhone != null)
                newCustomer.setPhoneNumber(normPhone);
            newCustomer.setAddress(dto.getContactAddress());
            newCustomer.setVerified(false);
            newCustomer.setRegistrationType("PUBLIC_FORM");
            try {
                customer = customerRepository.save(newCustomer);
            } catch (DataIntegrityViolationException ex) {
                // Race condition: someone else inserted same email/phone concurrently; re-query
                if (normEmail != null)
                    customer = customerRepository.findByEmail(normEmail).orElse(null);
                if (customer == null && normPhone != null)
                    customer = customerRepository.findByPhoneNumber(normPhone).orElse(null);
                if (customer == null)
                    throw ex; // rethrow if still not found
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
            User user = userRepository.findByEmployeeCode(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid employeeCode or user not found: " + code));
            String roleName = user.getRole() != null ? user.getRole().getName() : null;
            if (roleName == null || !roleName.toUpperCase().contains("SALE")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not a Sales staff: " + code);
            }
        } else {
            // Auto-assign for public requests
            User sales = findLeastBusySalesStaff();
            if (sales != null) {
                rfq.setAssignedSales(sales);
            }
        }

        // Auto-set status to SENT
        if (rfq.getStatus() == null || "DRAFT".equals(rfq.getStatus())) {
            rfq.setStatus("SENT");
            rfq.setSent(true);
        }

        if (dto.getExpectedDeliveryDate() != null)
            validateExpectedDeliveryDate(dto.getExpectedDeliveryDate(), java.time.LocalDate.now());

        return createWithDetails(rfq, dto.getDetails());
    }

    @Transactional
    public Rfq createBySales(SalesRfqCreateRequest req, Long salesUserId) {
        if (salesUserId == null)
            throw new IllegalArgumentException("Missing salesUserId header");
        if ((req.getContactEmail() == null || req.getContactEmail().isBlank())
                && (req.getContactPhone() == null || req.getContactPhone().isBlank())) {
            throw new IllegalArgumentException("Must provide contactEmail or contactPhone");
        }

        // Validate contactAddress is not empty (frontend sends full address string)
        if (req.getContactAddress() == null || req.getContactAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng điền đầy đủ địa chỉ nhận hàng.");
        }
        String email = normalizeEmail(req.getContactEmail());
        String phone = normalizePhone(req.getContactPhone());
        String method = validateAndDetermineMethod(req.getContactMethod(), email, phone);
        Customer customer = null;
        if (email != null)
            customer = customerRepository.findByEmail(email).orElse(null);
        if (customer == null && phone != null)
            customer = customerRepository.findByPhoneNumber(phone).orElse(null);
        if (customer == null) {
            customer = new Customer();
            String prefix = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            int rand = new java.security.SecureRandom().nextInt(900) + 100;
            customer.setCustomerCode("CUS-" + prefix + "-" + rand);
            customer.setContactPerson(req.getContactPerson());
            if (email != null)
                customer.setEmail(email);
            if (phone != null)
                customer.setPhoneNumber(phone);
            customer.setAddress(req.getContactAddress());
            customer.setVerified(false);
            customer.setRegistrationType("SALES_ON_BEHALF");
            customer = customerRepository.save(customer);
        }
        Rfq rfq = new Rfq();
        rfq.setCustomer(customer);
        rfq.setSourceType("BY_SALES");
        rfq.setExpectedDeliveryDate(req.getExpectedDeliveryDate());

        // Logic: If Sales creates it, they are assigned.
        // Request: "nếu là create bởi sales thì sửa lại là forwarded to planning"
        rfq.setStatus("FORWARDED_TO_PLANNING");
        rfq.setSent(true);
        rfq.setSalesConfirmedAt(java.time.Instant.now());

        rfq.setNotes(req.getNotes());
        User sales = new User();
        sales.setId(salesUserId);
        rfq.setAssignedSales(sales);
        rfq.setSalesConfirmedBy(sales);
        // snapshots
        rfq.setContactPersonSnapshot(req.getContactPerson());
        rfq.setContactEmailSnapshot(email);
        rfq.setContactPhoneSnapshot(phone);
        rfq.setContactAddressSnapshot(req.getContactAddress());
        rfq.setContactMethod(method);
        if (req.getExpectedDeliveryDate() != null)
            validateExpectedDeliveryDate(req.getExpectedDeliveryDate(), java.time.LocalDate.now());
        return createWithDetails(rfq, req.getDetails());
    }

    private String validateAndDetermineMethod(String providedMethod, String email, String phone) {
        String inferred = inferContactMethod(email, phone);
        if (providedMethod == null || providedMethod.isBlank())
            return inferred;
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
        if (email != null && !email.isBlank())
            return "EMAIL";
        if (phone != null && !phone.isBlank())
            return "PHONE";
        return null; // caller validation ensures at least one present
    }

    private String normalizeEmail(String email) {
        if (email == null)
            return null;
        String e = email.trim().toLowerCase();
        return e.isEmpty() ? null : e;
    }

    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        String p = phone.trim();
        if (p.isEmpty())
            return null;
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

    @Transactional
    public Rfq updateRfqWithDetails(Long rfqId, tmmsystem.dto.sales.RfqDto dto) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        // Check if RFQ can be edited (similar to salesEditRfqAndCustomer)
        if (!"DRAFT".equals(rfq.getStatus()) && !"SENT".equals(rfq.getStatus())) {
            throw new IllegalStateException("Chỉ sửa khi RFQ ở DRAFT hoặc SENT trước preliminary-check");
        }
        if (Boolean.TRUE.equals(rfq.getLocked())) {
            throw new IllegalStateException("RFQ locked");
        }

        // Update basic fields
        if (dto.getExpectedDeliveryDate() != null) {
            java.time.LocalDate baseDate = rfq.getCreatedAt() != null
                    ? rfq.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    : java.time.LocalDate.now();
            validateExpectedDeliveryDate(dto.getExpectedDeliveryDate(), baseDate);
            rfq.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        }
        if (dto.getNotes() != null)
            rfq.setNotes(dto.getNotes());

        // Update customer contact info and snapshots
        Customer cust = rfq.getCustomer();
        if (cust != null) {
            if (dto.getContactPerson() != null) {
                cust.setContactPerson(dto.getContactPerson());
                rfq.setContactPersonSnapshot(dto.getContactPerson());
            }
            if (dto.getContactEmail() != null) {
                String normEmail = normalizeEmail(dto.getContactEmail());
                cust.setEmail(normEmail);
                rfq.setContactEmailSnapshot(normEmail);
            }
            if (dto.getContactPhone() != null) {
                String normPhone = normalizePhone(dto.getContactPhone());
                cust.setPhoneNumber(normPhone);
                rfq.setContactPhoneSnapshot(normPhone);
            }
            if (dto.getContactAddress() != null) {
                cust.setAddress(dto.getContactAddress());
                rfq.setContactAddressSnapshot(dto.getContactAddress());
            }
            customerRepository.save(cust);
            // Re-infer contact method if snapshots changed
            String inferred = inferContactMethod(rfq.getContactEmailSnapshot(), rfq.getContactPhoneSnapshot());
            rfq.setContactMethod(inferred);
        }

        // Update details if provided
        if (dto.getDetails() != null) {
            detailRepository.findByRfqId(rfq.getId()).forEach(d -> detailRepository.deleteById(d.getId()));
            for (tmmsystem.dto.sales.RfqDetailDto detailDto : dto.getDetails()) {
                RfqDetail nd = new RfqDetail();
                nd.setRfq(rfq);
                if (detailDto.getProductId() != null) {
                    Product p = productRepository.findById(detailDto.getProductId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Product not found with id: " + detailDto.getProductId()));
                    nd.setProduct(p);
                }
                nd.setQuantity(detailDto.getQuantity());
                nd.setUnit(detailDto.getUnit());
                nd.setNoteColor(detailDto.getNoteColor());
                nd.setNotes(detailDto.getNotes());
                detailRepository.save(nd);
            }
        }

        return rfqRepository.save(rfq);
    }

    public void delete(Long id) {
        rfqRepository.deleteById(id);
    }

    public List<RfqDetail> findDetailsByRfqId(Long rfqId) {
        return detailRepository.findByRfqId(rfqId);
    }

    public RfqDetail findDetailById(Long id) {
        return detailRepository.findById(id).orElseThrow();
    }

    @Transactional
    public RfqDetail addDetail(Long rfqId, RfqDetailDto dto) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (isImmutableStatus(rfq.getStatus())) {
            throw new IllegalStateException("RFQ is not editable at current status");
        }
        RfqDetail detail = new RfqDetail();
        detail.setRfq(rfq);
        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Product not found with id: " + dto.getProductId()));
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
        Rfq rfq = detail.getRfq();
        if (isImmutableStatus(rfq.getStatus())) {
            throw new IllegalStateException("RFQ is not editable at current status");
        }
        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Product not found with id: " + dto.getProductId()));
            detail.setProduct(product);
        }
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
        if (status == null)
            return false;
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
        // must have sales assignment before sending
        if (rfq.getAssignedSales() == null) {
            throw new IllegalStateException("Cần gán Sales trước khi gửi RFQ");
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
        if (!"DRAFT".equals(rfq.getStatus()))
            throw new IllegalStateException(
                    "Chỉ có thể phân công nhân viên kinh doanh khi yêu cầu báo giá đang ở trạng thái DRAFT. Trạng thái hiện tại: "
                            + rfq.getStatus());
        if (salesId == null && (employeeCode == null || employeeCode.isBlank())) {
            throw new IllegalArgumentException("Thiếu assignedSalesId hoặc employeeCode");
        }
        User salesUser;
        if (salesId != null) {
            salesUser = userRepository.findById(salesId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Sales user not found with id: " + salesId));
        } else {
            salesUser = userRepository.findByEmployeeCode(employeeCode.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid employeeCode or user not found: " + employeeCode));
        }

        String roleName = salesUser.getRole() != null ? salesUser.getRole().getName() : null;
        if (roleName == null || !roleName.toUpperCase().contains("SALE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not a Sales staff");
        }

        if (!Boolean.TRUE.equals(salesUser.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nhân viên này đã bị vô hiệu hóa, không thể phân công.");
        }

        rfq.setAssignedSales(salesUser);
        Rfq savedRfq = rfqRepository.save(rfq);

        // Notify the assigned sales user
        notificationService.notifyUser(
                salesUser,
                "ORDER",
                "INFO",
                "Bạn có RFQ mới được phân công",
                "Yêu cầu báo giá #" + savedRfq.getRfqNumber() + " từ khách hàng "
                        + (savedRfq.getContactPersonSnapshot() != null ? savedRfq.getContactPersonSnapshot() : "N/A")
                        + " đã được gán cho bạn.",
                "RFQ",
                savedRfq.getId());

        return savedRfq;
    }

    @Transactional
    public Rfq assignStaff(Long rfqId, Long salesId, Long approvedById) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (!"DRAFT".equals(rfq.getStatus())) {
            throw new IllegalStateException("Chỉ có thể gán nhân sự khi RFQ đang ở trạng thái DRAFT");
        }
        if (salesId == null) {
            throw new IllegalArgumentException("assignedSalesId là bắt buộc");
        }
        User sales = new User();
        sales.setId(salesId);
        rfq.setAssignedSales(sales);
        if (approvedById != null) {
            User director = new User();
            director.setId(approvedById);
            rfq.setApprovedBy(director);
            rfq.setApprovalDate(java.time.Instant.now());
        }
        return rfqRepository.save(rfq);
    }

    public List<Rfq> findByAssignedSales(Long salesId) {
        if (salesId == null)
            return java.util.Collections.emptyList();
        return rfqRepository.findByAssignedSales_Id(salesId);
    }

    public Page<Rfq> findByAssignedSales(Long salesId, Pageable pageable) {
        if (salesId == null)
            return Page.empty(pageable);
        return rfqRepository.findByAssignedSales_Id(salesId, pageable);
    }

    public Page<Rfq> findByAssignedSales(Long salesId, Pageable pageable, String search, String status) {
        if (salesId == null)
            return Page.empty(pageable);
        if ((search != null && !search.trim().isEmpty()) || (status != null && !status.trim().isEmpty())) {
            String searchLower = search != null ? search.trim().toLowerCase() : "";
            String finalStatus = status;
            return rfqRepository.findAll((root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                predicates.add(cb.equal(root.get("assignedSales").get("id"), salesId));

                if (search != null && !search.trim().isEmpty()) {
                    var searchPredicate = cb.or(
                            cb.like(cb.lower(root.get("rfqNumber")), "%" + searchLower + "%"),
                            cb.like(cb.lower(root.get("contactPersonSnapshot")), "%" + searchLower + "%"));
                    predicates.add(searchPredicate);
                }

                if (finalStatus != null && !finalStatus.trim().isEmpty()) {
                    if (finalStatus.contains(",")) {
                        String[] statuses = finalStatus.split(",");
                        var statusPredicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                        for (String s : statuses) {
                            if (!s.trim().isEmpty()) {
                                statusPredicates.add(cb.equal(root.get("status"), s.trim()));
                            }
                        }
                        if (!statusPredicates.isEmpty()) {
                            predicates.add(
                                    cb.or(statusPredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
                        }
                    } else {
                        predicates.add(cb.equal(root.get("status"), finalStatus));
                    }
                }

                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }, pageable);
        } else {
            return rfqRepository.findByAssignedSales_Id(salesId, pageable);
        }
    }

    public List<Rfq> findByAssignedPlanning(Long planningId) {
        if (planningId == null)
            return java.util.Collections.emptyList();
        return rfqRepository.findByAssignedPlanning_Id(planningId);
    }

    public Page<Rfq> findByAssignedPlanning(Long planningId, Pageable pageable) {
        if (planningId == null)
            return Page.empty(pageable);
        return rfqRepository.findByAssignedPlanning_Id(planningId, pageable);
    }

    public Page<Rfq> findByAssignedPlanning(Long planningId, Pageable pageable, String search, String status) {
        if (planningId == null)
            return Page.empty(pageable);
        if (search != null && !search.trim().isEmpty() || status != null && !status.trim().isEmpty()) {
            String searchLower = search != null ? search.trim().toLowerCase() : "";
            String finalStatus = status;
            return rfqRepository.findAll((root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                predicates.add(cb.equal(root.get("assignedPlanning").get("id"), planningId));

                if (search != null && !search.trim().isEmpty()) {
                    var searchPredicate = cb.or(
                            cb.like(cb.lower(root.get("rfqNumber")), "%" + searchLower + "%"),
                            cb.like(cb.lower(root.get("contactPersonSnapshot")), "%" + searchLower + "%"));
                    predicates.add(searchPredicate);
                }

                if (finalStatus != null && !finalStatus.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("status"), finalStatus));
                }

                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }, pageable);
        } else {
            return rfqRepository.findByAssignedPlanning_Id(planningId, pageable);
        }
    }

    public List<Rfq> findDraftUnassigned() {
        return rfqRepository.findByStatusAndAssignedSalesIsNull("DRAFT");
    }

    public Page<Rfq> findDraftUnassigned(Pageable pageable) {
        return rfqRepository.findByStatusAndAssignedSalesIsNull("DRAFT", pageable);
    }

    @Transactional
    public int scanAndFixDraftRfqs() {
        List<Rfq> draftsWithSales = rfqRepository.findByStatusAndAssignedSalesIsNotNull("DRAFT");
        int count = 0;
        for (Rfq rfq : draftsWithSales) {
            rfq.setStatus("SENT");
            rfq.setSent(true);
            rfqRepository.save(rfq);
            count++;
        }
        return count;
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
            try {
                Thread.sleep(10L);
            } catch (InterruptedException ignored) {
            }
        }
        // Fallback random suffix to avoid collisions
        int rand = new java.security.SecureRandom().nextInt(900) + 100;
        return "RFQ-" + dateStr + "-" + String.format("%03d", rand);
    }

    // Hook to persist capacity evaluation summary into RFQ
    @Transactional
    public void persistCapacityEvaluation(Long rfqId, String capacityStatus, String reason,
            java.time.LocalDate proposedNewDate) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        rfq.setCapacityStatus(capacityStatus);
        rfq.setCapacityReason(reason);
        rfq.setProposedNewDeliveryDate(proposedNewDate);
        // Khi không đủ năng lực, chuyển status về SENT để Sales có thể chỉnh sửa hoặc
        // hủy
        if ("INSUFFICIENT".equalsIgnoreCase(capacityStatus)) {
            rfq.setStatus("SENT");
            notificationService.notifyCapacityInsufficient(rfq);
        }
        rfqRepository.save(rfq);
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
        if (req.getExpectedDeliveryDate() != null) {
            java.time.LocalDate baseDate = rfq.getCreatedAt() != null
                    ? java.time.LocalDate.ofInstant(rfq.getCreatedAt(), java.time.ZoneId.of("UTC"))
                    : java.time.LocalDate.now(java.time.ZoneId.of("UTC"));
            if (req.getExpectedDeliveryDate().isBefore(baseDate.plusDays(7))) {
                // warning or allow
            }
            rfq.setExpectedDeliveryDate(req.getExpectedDeliveryDate());
        }

        if (rfq.getCustomer() != null) {
            Customer cust = rfq.getCustomer();
            if (req.getContactPerson() != null)
                cust.setContactPerson(req.getContactPerson());
            if (req.getContactEmail() != null)
                cust.setEmail(normalizeEmail(req.getContactEmail()));
            if (req.getContactPhone() != null)
                cust.setPhoneNumber(normalizePhone(req.getContactPhone()));
            if (req.getContactAddress() != null)
                cust.setAddress(req.getContactAddress());
            customerRepository.save(cust);

            // Update snapshots simultaneously
            if (req.getContactPerson() != null)
                rfq.setContactPersonSnapshot(req.getContactPerson());
            if (req.getContactEmail() != null)
                rfq.setContactEmailSnapshot(normalizeEmail(req.getContactEmail()));
            if (req.getContactPhone() != null)
                rfq.setContactPhoneSnapshot(normalizePhone(req.getContactPhone()));
            if (req.getContactAddress() != null)
                rfq.setContactAddressSnapshot(req.getContactAddress());
            if (req.getContactMethod() != null && !req.getContactMethod().isBlank()) {
                String method = validateAndDetermineMethod(req.getContactMethod(), rfq.getContactEmailSnapshot(),
                        rfq.getContactPhoneSnapshot());
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
                if (d.getProductId() != null) {
                    Product p = new Product();
                    p.setId(d.getProductId());
                    nd.setProduct(p);
                }
                nd.setQuantity(d.getQuantity());
                nd.setUnit(d.getUnit());
                nd.setNoteColor(d.getNoteColor());
                nd.setNotes(d.getNotes());
                detailRepository.save(nd);
            }
        }

        // Reset capacity status so Planning can re-evaluate
        rfq.setCapacityStatus(null);
        rfq.setCapacityReason(null);
        rfq.setProposedNewDeliveryDate(null);

        return rfqRepository.save(rfq);

    }

    @Transactional
    public Rfq updateExpectedDeliveryDate(Long id, String expectedDateStr) {
        Rfq rfq = rfqRepository.findById(id).orElseThrow();
        if (Boolean.TRUE.equals(rfq.getLocked())) {
            throw new IllegalStateException("RFQ is locked and cannot change expected delivery date");
        }
        java.time.LocalDate date = parseFlexibleDate(expectedDateStr);
        java.time.LocalDate baseDate = rfq.getCreatedAt() != null
                ? rfq.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                : java.time.LocalDate.now();
        validateExpectedDeliveryDate(date, baseDate);
        rfq.setExpectedDeliveryDate(date);
        return rfqRepository.save(rfq);
    }

    private java.time.LocalDate parseFlexibleDate(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("expectedDeliveryDate is required");
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
                    throw new IllegalArgumentException(
                            "Invalid date format. Use yyyy-MM-dd or dd-MM-yyyy or day of month (1-31)");
                }
            }
        }
    }

    private void validateExpectedDeliveryDate(java.time.LocalDate date, java.time.LocalDate baseDate) {
        if (date == null)
            throw new IllegalArgumentException("expectedDeliveryDate is required");
        if (baseDate == null)
            baseDate = java.time.LocalDate.now();

        // Use 29 days to be lenient for timezone differences, while the UI and error
        // message still enforce 30 days.
        java.time.LocalDate min = baseDate.plusDays(29);
        if (date.isBefore(min)) {
            throw new IllegalArgumentException("Expected delivery date must be at least 30 days from creation date ("
                    + baseDate + ") (>= " + baseDate.plusDays(30) + ")");
        }
    }

    private User findLeastBusySalesStaff() {
        // Try precise roles first
        List<User> salesStaff = userRepository.findByRoleName("SALE STAFF");
        if (salesStaff.isEmpty()) {
            salesStaff = userRepository.findByRoleName("SALE");
        }

        if (salesStaff.isEmpty()) {
            return null;
        }

        User bestCandidate = null;
        long minCount = Long.MAX_VALUE;

        for (User u : salesStaff) {
            // Updated rule: Exclude inactive users
            if (!Boolean.TRUE.equals(u.getActive())) {
                continue;
            }

            long count = rfqRepository.countByAssignedSales_Id(u.getId());
            if (count < minCount) {
                minCount = count;
                bestCandidate = u;
            }
        }
        return bestCandidate;
    }

    @Transactional
    public void assignSalesToUnassignedRfqs(org.slf4j.Logger log) {
        // Find RFQs that are not cancelled and have no sales assigned
        List<Rfq> all = rfqRepository.findAll();
        int count = 0;
        for (Rfq rfq : all) {
            String status = rfq.getStatus() != null ? rfq.getStatus() : "DRAFT";
            if (rfq.getAssignedSales() == null && !"CANCELED".equals(status) && !"REJECTED".equals(status)) {
                User sales = findLeastBusySalesStaff();
                if (sales != null) {
                    rfq.setAssignedSales(sales);
                    // Also ensure status is SENT if it was DRAFT
                    if ("DRAFT".equals(status) || rfq.getStatus() == null) {
                        rfq.setStatus("SENT");
                        rfq.setSent(true);
                    }
                    rfqRepository.save(rfq);
                    count++;
                }
            }
        }
        if (count > 0 && log != null) {
            log.info("Startup: Auto-assigned {} RFQs to Sales Staff", count);
        }
    }

    @Transactional
    public void reassignInactiveSalesRfqs(org.slf4j.Logger log) {
        // Find RFQs assigned to inactive sales
        List<Rfq> all = rfqRepository.findAll();
        int count = 0;
        for (Rfq rfq : all) {
            if (rfq.getAssignedSales() != null && !Boolean.TRUE.equals(rfq.getAssignedSales().getActive())) {
                String oldName = rfq.getAssignedSales().getName();
                User newSales = findLeastBusySalesStaff(); // Now filters active only
                if (newSales != null) {
                    rfq.setAssignedSales(newSales);
                    rfqRepository.save(rfq);
                    count++;
                    if (log != null) {
                        log.info("Reassigned RFQ #{} from inactive sales '{}' to active sales '{}'",
                                rfq.getRfqNumber(), oldName, newSales.getName());
                    }
                } else if (log != null) {
                    log.warn("Could not reassign RFQ #{} from inactive sales '{}': No active sales staff found",
                            rfq.getRfqNumber(), oldName);
                }
            }
        }
        if (count > 0 && log != null) {
            log.info("Startup: Reassigned {} RFQs from inactive sales staff", count);
        }
    }
}
