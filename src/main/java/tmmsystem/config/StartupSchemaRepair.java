package tmmsystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

@Component
public class StartupSchemaRepair implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupSchemaRepair.class);

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    @Value("${repair.planStage.enabled:true}")
    private boolean enabled;

    public StartupSchemaRepair(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("[Repair] ProductionPlanStage repair disabled");
            return;
        }
        if (!isMySql()) {
            log.info("[Repair] Non-MySQL database detected. Skipping schema repair.");
            return;
        }
        try {
            // 1) Ensure new plan_id exists and is populated
            ensurePlanIdColumnAndBackfill();
            // 2) Relax or drop legacy plan_detail_id to avoid NOT NULL constraint violations
            relaxOrDropLegacyPlanDetailColumn();
            // 3) Ensure FK and related housekeeping
            ensureProductionPlanStageFk();
        } catch (Exception ex) {
            log.warn("[Repair] Schema repair failed: {}", ex.getMessage());
            // do not fail startup; migration-less repair is best-effort
        }
    }

    private boolean isMySql() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            String product = md.getDatabaseProductName();
            return product != null && product.toLowerCase().contains("mysql");
        } catch (Exception e) {
            log.warn("[Repair] Unable to determine DB product: {}", e.getMessage());
            return false;
        }
    }

    // ===== New: Ensure stage.plan_id exists and is backfilled from production_plan_detail if available =====
    private void ensurePlanIdColumnAndBackfill() {
        if (!columnExists("production_plan_stage", "plan_id")) {
            String planIdType = columnType("production_plan", "id");
            String type = planIdType != null ? planIdType : "BIGINT";
            log.info("[Repair] Adding production_plan_stage.plan_id {} NULL", type);
            jdbc.execute("ALTER TABLE production_plan_stage ADD COLUMN plan_id " + type + " NULL");
            // create index for performance if not exists
            try { jdbc.execute("CREATE INDEX idx_plan_stage_plan ON production_plan_stage(plan_id)"); } catch (Exception ignore) {}
        }
        // Backfill from legacy production_plan_detail if it exists
        if (tableExists("production_plan_detail") && columnExists("production_plan_stage", "plan_detail_id")) {
            log.info("[Repair] Backfilling stage.plan_id from legacy plan_detail_id -> production_plan_detail.plan_id");
            String update = "UPDATE production_plan_stage s JOIN production_plan_detail d ON s.plan_detail_id = d.id SET s.plan_id = d.plan_id WHERE s.plan_id IS NULL";
            try { jdbc.update(update); } catch (Exception e) { log.warn("[Repair] Backfill failed: {}", e.getMessage()); }
        }
    }

    // ===== New: Drop or relax NOT NULL on plan_detail_id to avoid insert failures =====
    private void relaxOrDropLegacyPlanDetailColumn() {
        if (!columnExists("production_plan_stage", "plan_detail_id")) return;
        // Drop FK referencing production_plan_detail on plan_detail_id if any
        List<String> fks = jdbc.query(
                "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='production_plan_stage' AND COLUMN_NAME='plan_detail_id' AND REFERENCED_TABLE_NAME IS NOT NULL",
                (rs, i) -> rs.getString(1)
        );
        for (String fk : fks) {
            try {
                log.info("[Repair] Dropping FK {} on production_plan_stage.plan_detail_id", fk);
                jdbc.execute("ALTER TABLE production_plan_stage DROP FOREIGN KEY " + fk);
            } catch (Exception e) {
                log.warn("[Repair] Could not drop FK {}: {}", fk, e.getMessage());
            }
        }
        // Try drop column entirely (preferred)
        try {
            log.info("[Repair] Dropping legacy column production_plan_stage.plan_detail_id");
            jdbc.execute("ALTER TABLE production_plan_stage DROP COLUMN plan_detail_id");
            return; // done
        } catch (Exception dropEx) {
            log.warn("[Repair] DROP COLUMN plan_detail_id failed: {} -- will make it NULLABLE instead", dropEx.getMessage());
        }
        // Fallback: make it nullable to avoid NOT NULL errors
        try {
            String type = columnType("production_plan_stage", "plan_detail_id");
            if (type == null) type = "BIGINT";
            log.info("[Repair] Alter legacy plan_detail_id to be NULLABLE");
            jdbc.execute("ALTER TABLE production_plan_stage MODIFY COLUMN plan_detail_id " + type + " NULL");
        } catch (Exception e) {
            log.warn("[Repair] Failed to relax plan_detail_id nullability: {}", e.getMessage());
        }
        // Optionally drop legacy table production_plan_detail if not referenced anymore
        if (tableExists("production_plan_detail")) {
            try {
                log.info("[Repair] Dropping legacy table production_plan_detail (if unused)");
                jdbc.execute("DROP TABLE IF EXISTS production_plan_detail");
            } catch (Exception ignore) {}
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

    // ===== Helpers =====
    private boolean columnExists(String table, String column) {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        Integer cnt = jdbc.query(sql, ps -> { ps.setString(1, table); ps.setString(2, column); }, (rs, i) -> rs.getInt(1))
                .stream().findFirst().orElse(0);
        return cnt != null && cnt > 0;
    }

    private boolean tableExists(String table) {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        Integer cnt = jdbc.query(sql, ps -> ps.setString(1, table), (rs, i) -> rs.getInt(1))
                .stream().findFirst().orElse(0);
        return cnt != null && cnt > 0;
    }

    private String columnType(String table, String column) {
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        List<String> res = jdbc.query(sql, ps -> { ps.setString(1, table); ps.setString(2, column); }, (rs, i) -> rs.getString(1));
        return res.isEmpty() ? null : res.get(0);
    }
}
