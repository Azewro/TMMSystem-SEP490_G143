package tmmsystem.service.timeline;

import java.math.BigDecimal;

public class SequentialCapacityResult {
    private BigDecimal warpingDays = BigDecimal.ZERO;
    private BigDecimal weavingDays = BigDecimal.ZERO;
    private BigDecimal dyeingDays = BigDecimal.ZERO;
    private BigDecimal cuttingDays = BigDecimal.ZERO;
    private BigDecimal sewingDays = BigDecimal.ZERO;
    private BigDecimal totalDays = BigDecimal.ZERO;
    private String bottleneck = "WARPING";

    public BigDecimal getWarpingDays() { return warpingDays; }
    public void setWarpingDays(BigDecimal warpingDays) { this.warpingDays = warpingDays; }

    public BigDecimal getWeavingDays() { return weavingDays; }
    public void setWeavingDays(BigDecimal weavingDays) { this.weavingDays = weavingDays; }

    public BigDecimal getDyeingDays() { return dyeingDays; }
    public void setDyeingDays(BigDecimal dyeingDays) { this.dyeingDays = dyeingDays; }

    public BigDecimal getCuttingDays() { return cuttingDays; }
    public void setCuttingDays(BigDecimal cuttingDays) { this.cuttingDays = cuttingDays; }

    public BigDecimal getSewingDays() { return sewingDays; }
    public void setSewingDays(BigDecimal sewingDays) { this.sewingDays = sewingDays; }

    public BigDecimal getTotalDays() { return totalDays; }
    public void setTotalDays(BigDecimal totalDays) { this.totalDays = totalDays; }

    public String getBottleneck() { return bottleneck; }
    public void setBottleneck(String bottleneck) { this.bottleneck = bottleneck; }
}

