-- Backfill existing quotation assignments from related RFQ
UPDATE quotation q
SET assigned_sales_id = r.assigned_sales_id,
    assigned_planning_id = r.assigned_planning_id
FROM rfq r
WHERE q.rfq_id = r.id
  AND (q.assigned_sales_id IS NULL OR q.assigned_planning_id IS NULL);

