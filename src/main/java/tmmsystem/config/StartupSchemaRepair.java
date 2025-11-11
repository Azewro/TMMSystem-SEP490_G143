package tmmsystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class StartupSchemaRepair implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupSchemaRepair.class);

    private final JdbcTemplate jdbc;

    @Value("${repair.planStage.enabled:true}")
    private boolean enabled;

    public StartupSchemaRepair(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("[Repair] ProductionPlanStage repair disabled");
            return;
        }
        try {
            ensureProductionPlanStageFk();
        } catch (Exception ex) {
            log.warn("[Repair] Failed to ensure FK: {}", ex.getMessage());
            // do not fail startup; FK can be added later
        }
    }

    private void ensureProductionPlanStageFk() {
        if (fkExists()) {
            log.info("[Repair] FK already exists. Skipping.");
            return;
        }
        int orphanCount = countOrphanStages();
        if (orphanCount > 0) {
            log.warn("[Repair] Found {} orphan production_plan_stage rows. Deleting them.", orphanCount);
            deleteOrphanStages();
        }
        alignColumnTypes();
        ensureInnoDb();
        addFk();
        log.info("[Repair] FK added successfully.");
    }

    private boolean fkExists() {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'production_plan_stage' AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME = 'fk_production_plan_stage_plan'";
        Integer count = jdbc.queryForObject(sql, Integer.class);
        return count != null && count > 0;
    }

    private int countOrphanStages() {
        String sql = "SELECT COUNT(*) FROM production_plan_stage s LEFT JOIN production_plan p ON s.plan_id = p.id WHERE s.plan_id IS NOT NULL AND p.id IS NULL";
        Integer count = jdbc.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    private void deleteOrphanStages() {
        String sql = "DELETE s FROM production_plan_stage s LEFT JOIN production_plan p ON s.plan_id = p.id WHERE s.plan_id IS NOT NULL AND p.id IS NULL";
        jdbc.update(sql);
    }

    private void alignColumnTypes() {
        // Align plan_id type to production_plan.id
        String idTypeSql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'production_plan' AND COLUMN_NAME = 'id'";
        List<String> idTypes = jdbc.query(idTypeSql, (rs, i) -> rs.getString(1));
        if (idTypes.isEmpty()) return;
        String idType = idTypes.get(0);
        String stageTypeSql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'production_plan_stage' AND COLUMN_NAME = 'plan_id'";
        List<String> stageTypes = jdbc.query(stageTypeSql, (rs, i) -> rs.getString(1));
        String stageType = stageTypes.isEmpty() ? null : stageTypes.get(0);
        if (stageType == null || !idType.equalsIgnoreCase(stageType)) {
            String alter = "ALTER TABLE production_plan_stage MODIFY COLUMN plan_id " + idType + " NULL";
            log.info("[Repair] Aligning plan_id type to {}", idType);
            jdbc.execute(alter);
        }
    }

    private void ensureInnoDb() {
        String engPlan = engineOf("production_plan");
        String engStage = engineOf("production_plan_stage");
        if (engPlan != null && !"InnoDB".equalsIgnoreCase(engPlan)) {
            log.info("[Repair] Alter engine production_plan -> InnoDB");
            jdbc.execute("ALTER TABLE production_plan ENGINE=InnoDB");
        }
        if (engStage != null && !"InnoDB".equalsIgnoreCase(engStage)) {
            log.info("[Repair] Alter engine production_plan_stage -> InnoDB");
            jdbc.execute("ALTER TABLE production_plan_stage ENGINE=InnoDB");
        }
    }

    private String engineOf(String table) {
        String sql = "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        List<String> engines = jdbc.query(sql, ps -> ps.setString(1, table), (rs, i) -> rs.getString(1));
        return engines.isEmpty() ? null : engines.get(0);
    }

    private void addFk() {
        // Drop any existing FK on plan_id if named differently to avoid duplicates
        String findOtherFk = "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='production_plan_stage' AND COLUMN_NAME='plan_id' AND REFERENCED_TABLE_NAME IS NOT NULL";
        List<String> fks = jdbc.query(findOtherFk, (rs, i) -> rs.getString(1));
        for (String fk : fks) {
            if (!"fk_production_plan_stage_plan".equalsIgnoreCase(fk)) {
                log.info("[Repair] Dropping existing FK {} on plan_id", fk);
                jdbc.execute("ALTER TABLE production_plan_stage DROP FOREIGN KEY " + fk);
            }
        }
        jdbc.execute("ALTER TABLE production_plan_stage ADD CONSTRAINT fk_production_plan_stage_plan FOREIGN KEY (plan_id) REFERENCES production_plan(id)");
    }
}

