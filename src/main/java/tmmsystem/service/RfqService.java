package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.repository.RfqDetailRepository;
import tmmsystem.repository.RfqRepository;
import tmmsystem.dto.sales.RfqDetailDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RfqService {
    private final RfqRepository rfqRepository;
    private final RfqDetailRepository detailRepository;
    private final NotificationService notificationService;

    public RfqService(RfqRepository rfqRepository, RfqDetailRepository detailRepository, NotificationService notificationService) {
        this.rfqRepository = rfqRepository;
        this.detailRepository = detailRepository;
        this.notificationService = notificationService;
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
