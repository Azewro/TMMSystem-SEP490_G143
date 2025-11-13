package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.repository.*;
import tmmsystem.dto.sales.PriceCalculationDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuotationService {
    private final QuotationRepository quotationRepository;
    private final QuotationDetailRepository quotationDetailRepository;
    private final RfqRepository rfqRepository;
    private final RfqDetailRepository rfqDetailRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;
    private final MaterialStockRepository materialStockRepository;
    private final ContractRepository contractRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    // NEW: inject CustomerService to provision password when needed
    private final CustomerService customerService;

    public QuotationService(QuotationRepository quotationRepository, 
                           QuotationDetailRepository quotationDetailRepository,
                           RfqRepository rfqRepository,
                           RfqDetailRepository rfqDetailRepository,
                           ProductRepository productRepository,
                           MaterialRepository materialRepository,
                           MaterialStockRepository materialStockRepository,
                           ContractRepository contractRepository,
                           NotificationService notificationService,
                           EmailService emailService,
                           FileStorageService fileStorageService,
                           CustomerService customerService) {
        this.quotationRepository = quotationRepository;
        this.quotationDetailRepository = quotationDetailRepository;
        this.rfqRepository = rfqRepository;
        this.rfqDetailRepository = rfqDetailRepository;
        this.productRepository = productRepository;
        this.materialRepository = materialRepository;
        this.materialStockRepository = materialStockRepository;
        this.contractRepository = contractRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.fileStorageService = fileStorageService;
        this.customerService = customerService;
    }

    public List<Quotation> findAll() { 
        return quotationRepository.findAll(); 
    }

    public Quotation findById(Long id) { 
        return quotationRepository.findById(id).orElseThrow(); 
    }

    @Transactional
    public Quotation create(Quotation quotation) { 
        // Auto populate from RFQ if provided
        if (quotation.getRfq() != null && quotation.getRfq().getId() != null) {
            Rfq rfq = rfqRepository.findById(quotation.getRfq().getId()).orElseThrow();
            if (quotation.getCustomer() == null) quotation.setCustomer(rfq.getCustomer());
            if (quotation.getAssignedSales() == null && rfq.getAssignedSales() != null) quotation.setAssignedSales(rfq.getAssignedSales());
            if (quotation.getAssignedPlanning() == null && rfq.getAssignedPlanning() != null) quotation.setAssignedPlanning(rfq.getAssignedPlanning());
            if (quotation.getContactPersonSnapshot() == null) quotation.setContactPersonSnapshot(rfq.getContactPersonSnapshot());
            if (quotation.getContactEmailSnapshot() == null) quotation.setContactEmailSnapshot(rfq.getContactEmailSnapshot());
            if (quotation.getContactPhoneSnapshot() == null) quotation.setContactPhoneSnapshot(rfq.getContactPhoneSnapshot());
            if (quotation.getContactAddressSnapshot() == null) quotation.setContactAddressSnapshot(rfq.getContactAddressSnapshot());
            if (quotation.getContactMethod() == null) quotation.setContactMethod(rfq.getContactMethod());
        }
        return quotationRepository.save(quotation);
    }

    @Transactional
    public Quotation update(Long id, Quotation updated) {
        Quotation existing = quotationRepository.findById(id).orElseThrow();
        existing.setQuotationNumber(updated.getQuotationNumber());
        existing.setRfq(updated.getRfq());
        existing.setCustomer(updated.getCustomer());
        existing.setValidUntil(updated.getValidUntil());
        existing.setTotalAmount(updated.getTotalAmount());
        existing.setStatus(updated.getStatus());
        existing.setAccepted(updated.getAccepted());
        existing.setCanceled(updated.getCanceled());
        existing.setCapacityCheckedBy(updated.getCapacityCheckedBy());
        existing.setCapacityCheckedAt(updated.getCapacityCheckedAt());
        existing.setCapacityCheckNotes(updated.getCapacityCheckNotes());
        existing.setAssignedSales(updated.getAssignedSales()); // NEW
        existing.setAssignedPlanning(updated.getAssignedPlanning()); // NEW
        existing.setCreatedBy(updated.getCreatedBy());
        existing.setApprovedBy(updated.getApprovedBy());
        existing.setFilePath(updated.getFilePath());
        existing.setSentAt(updated.getSentAt());
        return existing;
    }

    public void delete(Long id) { 
        quotationRepository.deleteById(id); 
    }

    // Planning Department: Tính giá báo giá từ RFQ (xem trước)
    public PriceCalculationDto calculateQuotationPrice(Long rfqId, BigDecimal profitMargin) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        
        // Kiểm tra trạng thái RFQ
        if (!"RECEIVED_BY_PLANNING".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ must be received by planning to calculate price");
        }

        // Lấy chi tiết RFQ và tính giá
        List<RfqDetail> rfqDetails = rfqDetailRepository.findByRfqId(rfqId);
        BigDecimal totalMaterialCost = BigDecimal.ZERO;
        BigDecimal totalProcessCost = BigDecimal.ZERO;
        BigDecimal totalBaseCost = BigDecimal.ZERO;
        BigDecimal finalTotalPrice = BigDecimal.ZERO;
        
        List<PriceCalculationDto.ProductPriceDetailDto> productDetails = new java.util.ArrayList<>();

        for (RfqDetail rfqDetail : rfqDetails) {
            Product product = productRepository.findById(rfqDetail.getProduct().getId()).orElseThrow();
            
            // Tính giá theo công thức
            PriceCalculationDto.ProductPriceDetailDto detail = calculateProductPriceDetail(product, rfqDetail.getQuantity(), profitMargin);
            productDetails.add(detail);
            
            totalMaterialCost = totalMaterialCost.add(detail.getMaterialCostPerUnit().multiply(detail.getQuantity()));
            totalProcessCost = totalProcessCost.add(detail.getProcessCostPerUnit().multiply(detail.getQuantity()));
            totalBaseCost = totalBaseCost.add(detail.getBaseCostPerUnit().multiply(detail.getQuantity()));
            finalTotalPrice = finalTotalPrice.add(detail.getTotalPrice());
        }

        // Tạo response
        PriceCalculationDto result = new PriceCalculationDto();
        result.setTotalMaterialCost(totalMaterialCost);
        result.setTotalProcessCost(totalProcessCost);
        result.setTotalBaseCost(totalBaseCost);
        result.setProfitMargin(profitMargin);
        result.setFinalTotalPrice(finalTotalPrice);
        result.setProductDetails(productDetails);

        return result;
    }
    
    // Planning Department: Tính lại giá khi thay đổi profit margin
    public PriceCalculationDto recalculateQuotationPrice(Long rfqId, BigDecimal profitMargin) {
        // Sử dụng cùng logic với calculateQuotationPrice nhưng không cần kiểm tra trạng thái RFQ
        // vì đã được gọi từ form đang mở
        return calculateQuotationPrice(rfqId, profitMargin);
    }
    
    private PriceCalculationDto.ProductPriceDetailDto calculateProductPriceDetail(Product product, BigDecimal quantity, BigDecimal profitMargin) {
        PriceCalculationDto.ProductPriceDetailDto detail = new PriceCalculationDto.ProductPriceDetailDto();
        detail.setProductId(product.getId());
        detail.setProductName(product.getName());
        detail.setQuantity(quantity);
        BigDecimal unitWeightKg = product.getStandardWeight().divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        detail.setUnitWeight(unitWeightKg);
        String productName = product.getName().toLowerCase();
        BigDecimal materialPricePerKg;
        if (productName.contains("cotton") && productName.contains("bambo")) {
            BigDecimal cottonAvgPrice = getAverageMaterialPrice("Ne 32/1CD");
            BigDecimal bambooAvgPrice = getAverageMaterialPrice("Ne 30/1");
            materialPricePerKg = cottonAvgPrice.add(bambooAvgPrice).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        } else if (productName.contains("bambo")) {
            materialPricePerKg = getAverageMaterialPrice("Ne 30/1");
        } else {
            materialPricePerKg = getAverageMaterialPrice("Ne 32/1CD");
        }
        BigDecimal materialCostPerUnit = unitWeightKg.multiply(materialPricePerKg);
        BigDecimal processCostPerUnit = unitWeightKg.multiply(new BigDecimal("45000"));
        BigDecimal baseCostPerUnit = materialCostPerUnit.add(processCostPerUnit);
        BigDecimal unitPrice = baseCostPerUnit.multiply(profitMargin);
        detail.setMaterialCostPerUnit(materialCostPerUnit);
        detail.setProcessCostPerUnit(processCostPerUnit);
        detail.setBaseCostPerUnit(baseCostPerUnit);
        detail.setUnitPrice(unitPrice);
        detail.setTotalPrice(unitPrice.multiply(quantity));
        return detail;
    }

    // Planning Department: Tạo báo giá từ RFQ
    @Transactional
    public Quotation createQuotationFromRfq(Long rfqId, java.lang.Long planningUserId, java.math.BigDecimal profitMargin, String capacityCheckNotes) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        if (!"RECEIVED_BY_PLANNING".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ must be received by planning to create quotation");
        }
        // Require capacity evaluation done
        if (rfq.getCapacityStatus() == null) {
            throw new IllegalStateException("Capacity evaluation not performed yet (capacityStatus is null)");
        }
        if (!"SUFFICIENT".equalsIgnoreCase(rfq.getCapacityStatus())) {
            // Allow INS UFFICIENT only if proposedNewDeliveryDate accepted by updating expectedDeliveryDate
            if ("INSUFFICIENT".equalsIgnoreCase(rfq.getCapacityStatus())) {
                if (rfq.getProposedNewDeliveryDate() == null) {
                    throw new IllegalStateException("Capacity insufficient. Proposed new delivery date missing");
                }
                if (!rfq.getProposedNewDeliveryDate().equals(rfq.getExpectedDeliveryDate())) {
                    throw new IllegalStateException("Capacity insufficient. Please update expectedDeliveryDate to proposedNewDeliveryDate before creating quotation");
                }
            } else {
                throw new IllegalStateException("RFQ capacityStatus must be SUFFICIENT to create quotation");
            }
        }
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber(generateQuotationNumber());
        quotation.setRfq(rfq);
        quotation.setCustomer(rfq.getCustomer());
        quotation.setValidUntil(LocalDate.now().plusDays(30));
        quotation.setStatus("DRAFT");
        User planningUser = new User(); planningUser.setId(planningUserId);
        quotation.setCapacityCheckedBy(planningUser);
        quotation.setCapacityCheckedAt(java.time.Instant.now());
        quotation.setCapacityCheckNotes(capacityCheckNotes != null ? capacityCheckNotes : "Khả năng sản xuất đã được kiểm tra - Kho đủ nguyên liệu, máy móc sẵn sàng");
        quotation.setCreatedBy(planningUser);
        // NEW: carry over assignees from RFQ
        if (rfq.getAssignedSales() != null) quotation.setAssignedSales(rfq.getAssignedSales());
        if (rfq.getAssignedPlanning() != null) quotation.setAssignedPlanning(rfq.getAssignedPlanning());
        // NEW: copy contact snapshot fields from RFQ
        quotation.setContactPersonSnapshot(rfq.getContactPersonSnapshot());
        quotation.setContactEmailSnapshot(rfq.getContactEmailSnapshot());
        quotation.setContactPhoneSnapshot(rfq.getContactPhoneSnapshot());
        quotation.setContactAddressSnapshot(rfq.getContactAddressSnapshot());
        quotation.setContactMethod(rfq.getContactMethod());

        List<RfqDetail> rfqDetails = rfqDetailRepository.findByRfqId(rfqId);
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<QuotationDetail> qDetails = new java.util.ArrayList<>();
        for (RfqDetail rfqDetail : rfqDetails) {
            Product product = productRepository.findById(rfqDetail.getProduct().getId()).orElseThrow();
            QuotationDetail quotationDetail = calculateQuotationDetail(product, rfqDetail.getQuantity(), profitMargin);
            quotationDetail.setQuotation(quotation);
            qDetails.add(quotationDetail);
            totalAmount = totalAmount.add(quotationDetail.getTotalPrice());
        }
        quotation.setTotalAmount(totalAmount);
        quotation.setDetails(qDetails);
        Quotation savedQuotation = quotationRepository.save(quotation);
        quotationDetailRepository.saveAll(qDetails);
        rfq.setStatus("QUOTED");
        rfqRepository.save(rfq);
        notificationService.notifyQuotationCreated(savedQuotation);

        // NEW: nếu customer chưa có mật khẩu, cấp mật khẩu tạm và gửi email kèm URL báo giá
        Customer customer = rfq.getCustomer();
        String tempPassword = null;
        if (customer != null && (customer.getPassword() == null || customer.getPassword().isBlank())) {
            try { tempPassword = customerService.provisionTemporaryPassword(customer.getId()); } catch (Exception ignore) {}
        }
        try { emailService.sendQuotationEmailWithLogin(savedQuotation, tempPassword); } catch (Exception ignore) {}
        return savedQuotation;
    }

    private QuotationDetail calculateQuotationDetail(Product product, BigDecimal quantity, BigDecimal profitMargin) {
        QuotationDetail detail = new QuotationDetail();
        detail.setProduct(product);
        detail.setQuantity(quantity);
        detail.setUnit("CÁI");
        String productName = product.getName().toLowerCase();
        BigDecimal materialPricePerKg;
        if (productName.contains("cotton") && productName.contains("bambo")) {
            BigDecimal cottonAvgPrice = getAverageMaterialPrice("Ne 32/1CD");
            BigDecimal bambooAvgPrice = getAverageMaterialPrice("Ne 30/1");
            materialPricePerKg = cottonAvgPrice.add(bambooAvgPrice).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        } else if (productName.contains("bambo")) {
            materialPricePerKg = getAverageMaterialPrice("Ne 30/1");
        } else {
            materialPricePerKg = getAverageMaterialPrice("Ne 32/1CD");
        }
        BigDecimal unitWeightKg = product.getStandardWeight().divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        BigDecimal materialCostPerUnit = unitWeightKg.multiply(materialPricePerKg);
        BigDecimal processCostPerUnit = unitWeightKg.multiply(new BigDecimal("45000"));
        BigDecimal basePricePerUnit = materialCostPerUnit.add(processCostPerUnit);
        BigDecimal unitPrice = basePricePerUnit.multiply(profitMargin);
        detail.setUnitPrice(unitPrice);
        detail.setTotalPrice(unitPrice.multiply(quantity));
        detail.setNoteColor(null);
        return detail;
    }

    private String generateQuotationNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = quotationRepository.count() + 1;
        return String.format("QUO-%s-%03d", dateStr, count);
    }

    /**
     * Tính giá trung bình của nguyên liệu dựa trên các batch nhập khác nhau
     * Công thức: (quantity1 * price1 + quantity2 * price2 + ...) / (quantity1 + quantity2 + ...)
     */
    private BigDecimal getAverageMaterialPrice(String materialCode) {
        Material material = materialRepository.findAll().stream()
                .filter(m -> materialCode.equals(m.getCode()))
                .findFirst()
                .orElse(null);
        
        if (material == null) {
            // Fallback về giá chuẩn nếu không tìm thấy
            return "Ne 32/1CD".equals(materialCode) ? new BigDecimal("68000") : new BigDecimal("78155");
        }

        // Lấy tất cả stock của material này
        List<MaterialStock> stocks = materialStockRepository.findByMaterialId(material.getId());
        
        if (stocks.isEmpty()) {
            // Fallback về giá chuẩn nếu không có stock
            return "Ne 32/1CD".equals(materialCode) ? new BigDecimal("68000") : new BigDecimal("78155");
        }

        // Tính giá trung bình có trọng số
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        
        for (MaterialStock stock : stocks) {
            if (stock.getUnitPrice() != null && stock.getQuantity() != null) {
                BigDecimal batchValue = stock.getQuantity().multiply(stock.getUnitPrice());
                totalValue = totalValue.add(batchValue);
                totalQuantity = totalQuantity.add(stock.getQuantity());
            }
        }
        
        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return totalValue.divide(totalQuantity, 2, RoundingMode.HALF_UP);
        } else {
            // Fallback về giá chuẩn
            return "Ne 32/1CD".equals(materialCode) ? new BigDecimal("68000") : new BigDecimal("78155");
        }
    }

    // Sale Staff: Lấy báo giá chờ gửi
    public List<Quotation> findPendingQuotations() {
        return quotationRepository.findAll().stream()
                .filter(q -> "DRAFT".equals(q.getStatus()))
                .collect(Collectors.toList());
    }

    // Sale Staff: Lấy báo giá chờ gửi nhưng chỉ những quotation được gán cho Sales này
    public List<Quotation> findPendingQuotationsByAssignedSales(Long salesUserId) {
        return quotationRepository.findAll().stream()
                .filter(q -> "DRAFT".equals(q.getStatus()))
                .filter(q -> q.getAssignedSales() != null && salesUserId.equals(q.getAssignedSales().getId()))
                .collect(Collectors.toList());
    }

    // Sale Staff: Gửi báo giá cho Customer
    @Transactional
    public Quotation sendQuotationToCustomer(Long quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId).orElseThrow();
        if (!"DRAFT".equals(quotation.getStatus())) {
            throw new IllegalStateException("Quotation must be in DRAFT status to send");
        }
        
        quotation.setStatus("SENT");
        quotation.setSentAt(java.time.Instant.now());
        Quotation savedQuotation = quotationRepository.save(quotation);

        // Gửi thông báo cho Customer
        notificationService.notifyQuotationSentToCustomer(savedQuotation);
        
        // Gửi email cho Customer
        emailService.sendQuotationEmail(savedQuotation);

        return savedQuotation;
    }

    // Customer: Lấy báo giá của mình
    public List<Quotation> findQuotationsByCustomer(Long customerId) {
        return quotationRepository.findAll().stream()
                .filter(q -> q.getCustomer().getId().equals(customerId))
                .collect(Collectors.toList());
    }

    // Customer: Duyệt báo giá
    @Transactional
    public Quotation approveQuotation(Long quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId).orElseThrow();
        if (!"SENT".equals(quotation.getStatus())) {
            throw new IllegalStateException("Quotation must be SENT to approve");
        }
        
        quotation.setStatus("ACCEPTED");
        quotation.setAccepted(true);
        quotation.setAcceptedAt(java.time.Instant.now());
        Quotation savedQuotation = quotationRepository.save(quotation);

        // Gửi thông báo cho Sale Staff
        notificationService.notifyQuotationApproved(savedQuotation);

        // Tự động tạo đơn hàng từ báo giá đã được duyệt và gửi thông báo "Order created"
        // (createOrderFromQuotation() sẽ chịu trách nhiệm gửi notification và email xác nhận đơn hàng)
        createOrderFromQuotation(quotationId);

        return savedQuotation;
    }

    // Customer: Từ chối báo giá
    @Transactional
    public Quotation rejectQuotation(Long quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId).orElseThrow();
        if (!"SENT".equals(quotation.getStatus())) {
            throw new IllegalStateException("Quotation must be SENT to reject");
        }
        
        quotation.setStatus("REJECTED");
        quotation.setRejectedAt(java.time.Instant.now());
        Quotation savedQuotation = quotationRepository.save(quotation);

        // Gửi thông báo cho Sale Staff
        notificationService.notifyQuotationRejected(savedQuotation);

        return savedQuotation;
    }

    @Transactional
    public Quotation rejectQuotation(Long quotationId, String reason) {
        Quotation quotation = quotationRepository.findById(quotationId).orElseThrow();
        if (!"SENT".equals(quotation.getStatus())) {
            throw new IllegalStateException("Quotation must be SENT to reject");
        }
        quotation.setStatus("REJECTED");
        quotation.setRejectedAt(java.time.Instant.now());
        quotation.setRejectReason(reason);
        Quotation savedQuotation = quotationRepository.save(quotation);
        notificationService.notifyQuotationRejected(savedQuotation);
        return savedQuotation;
    }

    // Tạo đơn hàng từ báo giá
    @Transactional
    public Object createOrderFromQuotation(Long quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId).orElseThrow();
        
        if (!"ACCEPTED".equals(quotation.getStatus())) {
            throw new IllegalStateException("Quotation must be ACCEPTED to create order");
        }

        // Tạo Contract từ Quotation
        Contract contract = new Contract();
        contract.setContractNumber(generateContractNumber());
        contract.setQuotation(quotation);
        contract.setCustomer(quotation.getCustomer());
        contract.setContractDate(java.time.LocalDate.now());
        contract.setDeliveryDate(quotation.getValidUntil());
        contract.setTotalAmount(quotation.getTotalAmount());
        contract.setStatus("PENDING_UPLOAD"); // pending upload signed contract by Sales
        contract.setCreatedBy(quotation.getCreatedBy());
        // propagate assignments from quotation
        contract.setAssignedSales(quotation.getAssignedSales());
        contract.setAssignedPlanning(quotation.getAssignedPlanning());
        // NEW: propagate approvals if any existed on quotation (mirror RFQ/Quotation)
        if (quotation.getApprovedBy() != null) {
            // treat as salesApprovedBy by default (can adjust per business rules)
            contract.setSalesApprovedBy(quotation.getApprovedBy());
            contract.setSalesApprovedAt(java.time.Instant.now());
        }
        if (quotation.getCapacityCheckedBy() != null) {
            contract.setPlanningApprovedBy(quotation.getCapacityCheckedBy());
            contract.setPlanningApprovedAt(quotation.getCapacityCheckedAt());
        }

        Contract savedContract = contractRepository.save(contract);

        quotation.setStatus("ORDER_CREATED");
        quotationRepository.save(quotation);

        notificationService.notifyOrderCreated(savedContract);
        emailService.sendOrderConfirmationEmail(savedContract);

        return savedContract;
    }

    // Helper to attach quotation PDF/file
    @Transactional
    public Quotation attachQuotationFile(Long quotationId, org.springframework.web.multipart.MultipartFile file) {
        Quotation quotation = quotationRepository.findById(quotationId).orElseThrow();
        try {
            String path = fileStorageService.uploadQuotationFile(file, quotationId);
            quotation.setFilePath(path);
            Quotation saved = quotationRepository.save(quotation);

            // NEW: If a contract exists for this quotation and currently waiting for upload, move to pending approval
            Contract contract = contractRepository.findFirstByQuotation_Id(quotationId);
            if (contract != null && "PENDING_UPLOAD".equals(contract.getStatus())) {
                contract.setStatus("PENDING_APPROVAL");
                contractRepository.save(contract);
                // Notify Director that contract is ready for approval
                notificationService.notifyContractUploaded(contract);
            }
            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload quotation file: " + e.getMessage(), e);
        }
    }

    private String generateContractNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = contractRepository.count() + 1;
        return String.format("CON-%s-%03d", dateStr, count);
    }

    // Planning Department: Kiểm tra khả năng cung ứng của RFQ
    private boolean isRfqCapacitySufficient(Long rfqId, BigDecimal profitMargin) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();

        // Lấy chi tiết RFQ
        List<RfqDetail> rfqDetails = rfqDetailRepository.findByRfqId(rfqId);

        for (RfqDetail rfqDetail : rfqDetails) {
            Product product = productRepository.findById(rfqDetail.getProduct().getId()).orElseThrow();

            // Tính toán khả năng cung ứng dựa trên tồn kho nguyên liệu và năng lực sản xuất
            BigDecimal requiredMaterialQty = rfqDetail.getQuantity().multiply(product.getStandardWeight()).divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
            BigDecimal availableMaterialQty = materialStockRepository.findByMaterialId(product.getId()).stream()
                    .map(MaterialStock::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (availableMaterialQty.compareTo(requiredMaterialQty) < 0) {
                return false; // Tồn kho không đủ
            }
        }

        return true; // Đủ khả năng cung ứng
    }

    public String getQuotationFileUrl(Long quotationId) {
        return fileStorageService.getQuotationFileUrl(quotationId);
    }
    public byte[] downloadQuotationFile(Long quotationId) {
        try { return fileStorageService.downloadQuotationFile(quotationId); } catch (Exception e) { throw new RuntimeException(e); }
    }
    public String getQuotationFileName(Long quotationId) {
        try { return fileStorageService.getQuotationFileName(quotationId); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
