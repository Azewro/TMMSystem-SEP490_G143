package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.repository.*;
import tmmsystem.dto.sales.QuotationDto;
import tmmsystem.dto.sales.QuotationDetailDto;
import tmmsystem.dto.sales.RfqDetailDto;
import tmmsystem.dto.sales.PriceCalculationDto;

import java.math.BigDecimal;
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

    // Chi phí công cố định
    private static final BigDecimal PROCESS_COST_PER_KG = new BigDecimal("45000"); // 20k + 15k + 10k
    private static final BigDecimal PROFIT_MARGIN = new BigDecimal("1.10"); // 10% lợi nhuận

    public QuotationService(QuotationRepository quotationRepository, 
                           QuotationDetailRepository quotationDetailRepository,
                           RfqRepository rfqRepository,
                           RfqDetailRepository rfqDetailRepository,
                           ProductRepository productRepository,
                           MaterialRepository materialRepository,
                           MaterialStockRepository materialStockRepository,
                           ContractRepository contractRepository,
                           NotificationService notificationService,
                           EmailService emailService) {
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
    }

    public List<Quotation> findAll() { 
        return quotationRepository.findAll(); 
    }

    public Quotation findById(Long id) { 
        return quotationRepository.findById(id).orElseThrow(); 
    }

    @Transactional
    public Quotation create(Quotation quotation) { 
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
        existing.setCreatedBy(updated.getCreatedBy());
        existing.setApprovedBy(updated.getApprovedBy());
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
        
        // Tính trọng lượng đơn vị (kg)
        BigDecimal unitWeightKg = product.getStandardWeight().divide(new BigDecimal("1000")); // gram to kg
        detail.setUnitWeight(unitWeightKg);
        
        // Xác định thành phần sợi từ tên sản phẩm và tính giá trung bình
        String productName = product.getName().toLowerCase();
        BigDecimal materialPricePerKg;
        
        if (productName.contains("cotton") && productName.contains("bambo")) {
            // 50-50 cotton + bamboo
            BigDecimal cottonAvgPrice = getAverageMaterialPrice("Ne 32/1CD");
            BigDecimal bambooAvgPrice = getAverageMaterialPrice("Ne 30/1");
            materialPricePerKg = cottonAvgPrice.add(bambooAvgPrice).divide(new BigDecimal("2"));
        } else if (productName.contains("bambo")) {
            // 100% bamboo
            materialPricePerKg = getAverageMaterialPrice("Ne 30/1");
        } else {
            // 100% cotton (mặc định)
            materialPricePerKg = getAverageMaterialPrice("Ne 32/1CD");
        }

        // Tính giá theo công thức
        BigDecimal materialCostPerUnit = unitWeightKg.multiply(materialPricePerKg);
        BigDecimal processCostPerUnit = unitWeightKg.multiply(PROCESS_COST_PER_KG);
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
    public Quotation createQuotationFromRfq(Long rfqId, Long planningUserId, BigDecimal profitMargin, String capacityCheckNotes) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        
        // Kiểm tra trạng thái RFQ
        if (!"RECEIVED_BY_PLANNING".equals(rfq.getStatus())) {
            throw new IllegalStateException("RFQ must be received by planning to create quotation");
        }

        // Tạo Quotation
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber(generateQuotationNumber());
        quotation.setRfq(rfq);
        quotation.setCustomer(rfq.getCustomer());
        quotation.setValidUntil(LocalDate.now().plusDays(30)); // 30 ngày hiệu lực
        quotation.setStatus("DRAFT");
        quotation.setCapacityCheckedBy(new User());
        quotation.getCapacityCheckedBy().setId(planningUserId);
        quotation.setCapacityCheckedAt(java.time.Instant.now());
        quotation.setCapacityCheckNotes(capacityCheckNotes != null ? capacityCheckNotes : "Khả năng sản xuất đã được kiểm tra - Kho đủ nguyên liệu, máy móc sẵn sàng");
        quotation.setCreatedBy(new User());
        quotation.getCreatedBy().setId(planningUserId);

        // Lấy chi tiết RFQ và tính giá
        List<RfqDetail> rfqDetails = rfqDetailRepository.findByRfqId(rfqId);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RfqDetail rfqDetail : rfqDetails) {
            Product product = productRepository.findById(rfqDetail.getProduct().getId()).orElseThrow();
            
            // Tính giá theo công thức với profit margin có thể thay đổi
            QuotationDetail quotationDetail = calculateQuotationDetail(product, rfqDetail.getQuantity(), profitMargin);
            quotationDetail.setQuotation(quotation);
            quotation.getDetails().add(quotationDetail);
            
            totalAmount = totalAmount.add(quotationDetail.getTotalPrice());
        }

        quotation.setTotalAmount(totalAmount);
        Quotation savedQuotation = quotationRepository.save(quotation);

        // Lưu chi tiết báo giá
        for (QuotationDetail detail : quotation.getDetails()) {
            quotationDetailRepository.save(detail);
        }

        // Cập nhật trạng thái RFQ
        rfq.setStatus("QUOTED");
        rfqRepository.save(rfq);

        // Gửi thông báo cho Sale Staff
        notificationService.notifyQuotationCreated(savedQuotation);

        return savedQuotation;
    }

    private QuotationDetail calculateQuotationDetail(Product product, BigDecimal quantity, BigDecimal profitMargin) {
        QuotationDetail detail = new QuotationDetail();
        detail.setProduct(product);
        detail.setQuantity(quantity);
        detail.setUnit("CÁI");

        // Xác định thành phần sợi từ tên sản phẩm và tính giá trung bình
        String productName = product.getName().toLowerCase();
        BigDecimal materialPricePerKg;
        
        if (productName.contains("cotton") && productName.contains("bambo")) {
            // 50-50 cotton + bamboo
            BigDecimal cottonAvgPrice = getAverageMaterialPrice("Ne 32/1CD");
            BigDecimal bambooAvgPrice = getAverageMaterialPrice("Ne 30/1");
            materialPricePerKg = cottonAvgPrice.add(bambooAvgPrice).divide(new BigDecimal("2"));
        } else if (productName.contains("bambo")) {
            // 100% bamboo
            materialPricePerKg = getAverageMaterialPrice("Ne 30/1");
        } else {
            // 100% cotton (mặc định)
            materialPricePerKg = getAverageMaterialPrice("Ne 32/1CD");
        }

        // Tính giá theo công thức
        BigDecimal unitWeightKg = product.getStandardWeight().divide(new BigDecimal("1000")); // gram to kg
        BigDecimal materialCostPerUnit = unitWeightKg.multiply(materialPricePerKg);
        BigDecimal processCostPerUnit = unitWeightKg.multiply(PROCESS_COST_PER_KG);
        BigDecimal basePricePerUnit = materialCostPerUnit.add(processCostPerUnit);
        BigDecimal unitPrice = basePricePerUnit.multiply(profitMargin); // Lợi nhuận có thể thay đổi

        detail.setUnitPrice(unitPrice);
        detail.setTotalPrice(unitPrice.multiply(quantity));
        detail.setNoteColor(null); // Không có noteColor từ RfqDetail

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
        // Tìm material theo code
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
            return totalValue.divide(totalQuantity, 2, BigDecimal.ROUND_HALF_UP);
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

    // Sale Staff: Lấy báo giá chờ gửi nhưng chỉ những quotation có RFQ được gán cho Sales này
    public List<Quotation> findPendingQuotationsByAssignedSales(Long salesUserId) {
        return quotationRepository.findAll().stream()
                .filter(q -> "DRAFT".equals(q.getStatus()))
                .filter(q -> q.getRfq() != null && q.getRfq().getAssignedSales() != null)
                .filter(q -> salesUserId.equals(q.getRfq().getAssignedSales().getId()))
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
        Quotation savedQuotation = quotationRepository.save(quotation);

        // Gửi thông báo cho Sale Staff
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
        contract.setDeliveryDate(quotation.getValidUntil()); // Sử dụng ngày hết hạn của báo giá
        contract.setTotalAmount(quotation.getTotalAmount());
        contract.setStatus("DRAFT");
        contract.setCreatedBy(quotation.getCreatedBy());

        // Lưu Contract
        Contract savedContract = contractRepository.save(contract);

        // Cập nhật trạng thái Quotation
        quotation.setStatus("ORDER_CREATED");
        quotationRepository.save(quotation);

        // Gửi thông báo cho Sale Staff
        notificationService.notifyOrderCreated(savedContract);
        
        // Gửi email xác nhận đơn hàng cho Customer
        emailService.sendOrderConfirmationEmail(savedContract);

        return savedContract;
    }

    private String generateContractNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = contractRepository.count() + 1;
        return String.format("CON-%s-%03d", dateStr, count);
    }
}