package tmmsystem.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import tmmsystem.entity.Contract;
import tmmsystem.entity.QuotationDetail;
import tmmsystem.repository.ContractRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tự động gom lô (1 lot = 1 product) cho các hợp đồng APPROVED chưa gắn vào Lot.
 * Sau khi merge xong, sẽ để Lot ở trạng thái READY_FOR_PLANNING (chưa lock PLANNING).
 */
@Service
@Slf4j
public class AutoMergeService {
    private final ContractRepository contractRepository;
    private final ProductionPlanService productionPlanService;
    private final NotificationService notificationService;

    public AutoMergeService(ContractRepository contractRepository,
                            ProductionPlanService productionPlanService,
                            NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.productionPlanService = productionPlanService;
        this.notificationService = notificationService;
    }

    /**
     * Chạy mỗi 5 phút (mặc định). Có thể chỉnh bằng property: autoMerge.cron.
     */
    @Scheduled(cron = "${autoMerge.cron:0 0/5 * * * *}")
    @Transactional
    public void scanNewlyApprovedContracts(){
        List<Contract> approvedNoLot = contractRepository.findApprovedWithoutLot();
        if (approvedNoLot==null || approvedNoLot.isEmpty()) return;
        int createdLots = 0;
        for (Contract c : approvedNoLot){
            if (c.getQuotation()==null || c.getQuotation().getDetails()==null || c.getQuotation().getDetails().isEmpty()) continue;
            Map<Long, List<QuotationDetail>> byProduct = c.getQuotation().getDetails().stream()
                    .collect(Collectors.groupingBy(d -> d.getProduct().getId()));
            for (Map.Entry<Long, List<QuotationDetail>> e : byProduct.entrySet()){
                try {
                    productionPlanService.createOrMergeLotFromContractAndProduct(c, e.getKey(), e.getValue());
                    createdLots++;
                } catch (Exception ex){
                    log.warn("AutoMerge failed for contract {} product {}: {}", c.getId(), e.getKey(), ex.getMessage());
                }
            }
        }
        if (createdLots>0){
            try {
                notificationService.notifyPlanningNewLotsCreated(createdLots);
            } catch (Exception ignore) {}
        }
    }
}

